package io.kafbat.ui.util;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

@Slf4j
public final class MultiFileWatcher implements AutoCloseable {

  private static final long DEBOUNCE_MS = Duration.ofMillis(1000).toMillis();

  private final WatchService watchService = FileSystems.getDefault().newWatchService();
  private final Set<Path> watchedFiles = ConcurrentHashMap.newKeySet();
  private final Map<WatchKey, Path> watchDirsByKey = new HashMap<>();
  private final Function<Path, Void> reloader;

  private long lastTriggerAt = 0;

  public MultiFileWatcher(Collection<Path> filesToWatch, Function<Path, Void> reloader) throws IOException {
    Assert.notNull(reloader, "reloader must not be null");
    this.reloader = reloader;

    if (filesToWatch.isEmpty()) {
      log.warn("No files to watch, aborting");
    }

    watchedFiles.addAll(filesToWatch.stream()
        .map(p -> {
          try {
            return Files.exists(p) ? p.toRealPath() : p.toAbsolutePath().normalize();
          } catch (IOException e) {
            return p.toAbsolutePath().normalize();
          }
        })
        .toList());

    if (watchedFiles.isEmpty()) {
      log.warn("No files to watch resolved, aborting");
      return;
    }

    log.debug("Going to watch {} files", watchedFiles.size());
    log.trace("Watching files: {}", watchedFiles.stream().map(Path::toString).toList());

    watchedFiles.stream()
//        .map(getParentPath())
        .distinct()
        .forEach(file -> {
          try {
            var key = getParentPath().apply(file).register(watchService,
                ENTRY_MODIFY,
                ENTRY_CREATE, ENTRY_DELETE // watch these for atomic replacements
            );
            watchDirsByKey.put(key, file);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });

    log.trace("Watching directories: {}", watchDirsByKey.values().stream().map(a -> getParentPath().apply(a)).map(Path::toString).toList());
  }

  public void watchLoop() {
    while (true) {
      try {
        var key = watchService.take();
        var dir = getParentPath().apply(watchDirsByKey.get(key));
        if (dir == null) {
          continue;
        }

        var hit = key.pollEvents()
            .stream()
            .filter(ev -> ev.kind() != OVERFLOW)
            .map(ev -> dir.resolve((Path) ev.context()).normalize().toAbsolutePath())
            .anyMatch(this::matchesTarget);

        if (hit && shouldTrigger()) {
          var filePath = watchDirsByKey.get(key); // TODO
          reloader.apply(filePath);
        }

        if (!key.reset()) {
          watchDirsByKey.remove(key);
        }
        if (watchDirsByKey.isEmpty()) {
          break;
        }

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

  private boolean matchesTarget(Path changed) {
    if (watchedFiles.contains(changed)) {
      return true;
    }
    try {
      return watchedFiles.contains(changed.toRealPath());
    } catch (IOException ignored) {
      return false;
    }
  }

  private boolean shouldTrigger() {
    var now = System.currentTimeMillis();

    if (now - lastTriggerAt < DEBOUNCE_MS) {
      return false;
    }

    lastTriggerAt = now;
    return true;
  }

  @Override
  public void close() throws IOException {
    watchService.close();
  }

  @NotNull
  private static Function<Path, Path> getParentPath() {
    return p -> Optional.ofNullable(p.getParent()).orElse(Path.of("."));
  }
}

