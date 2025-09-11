package io.kafbat.ui.util;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
public final class MultiFileWatcher implements AutoCloseable {

  private final WatchService watchService = FileSystems.getDefault().newWatchService();
  private final Set<URI> watchedFiles = ConcurrentHashMap.newKeySet();
  private final Map<WatchKey, Path> watchDirsByKey = new HashMap<>();
  private final Consumer<Path> reloader;

  public MultiFileWatcher(Collection<Path> filesToWatch, Consumer<Path> reloader) throws IOException {
    Assert.notNull(reloader, "reloader must not be null");
    this.reloader = reloader;

    if (filesToWatch.isEmpty()) {
      log.warn("No files to watch, aborting");
    }


    watchedFiles.addAll(filesToWatch.stream()
        .map(p -> p.toAbsolutePath().normalize())
        .map(Path::toUri)
        .toList()
    );

    if (watchedFiles.isEmpty()) {
      log.warn("No files to watch resolved, aborting");
      return;
    }

    log.debug("Going to watch {} files", watchedFiles.size());
    log.trace("Watching files: {}", watchedFiles.stream().map(URI::toString).toList());

    var directories = filesToWatch
        .stream()
        .map(Path::getParent)
        .distinct()
        .toList();

    directories
        .forEach(dir -> {
          try {
            var key = dir.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
            watchDirsByKey.put(key, dir);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });

    log.trace("Watching directories: {}", directories.stream().map(Path::toString).toList());
  }

  public void watchLoop() {
    while (true) {
      try {
        var key = watchService.take();
        Path dir = watchDirsByKey.get(key);
        if (dir == null) {
          continue;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          Path relativePath = (Path) event.context();
          Path path = dir.resolve(relativePath);
          if (watchedFiles.contains(path.toAbsolutePath().normalize().toUri())) {
            reloader.accept(path);
          }
        }
        key.reset();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (ClosedWatchServiceException e) {
        log.trace("Watch service closed, exiting watcher thread");
        break;
      } catch (Exception e) {
        log.error("Error while calling the reloader", e);
        break;
      }
    }
  }

  @Override
  public void close() throws IOException {
    watchService.close();
  }
}

