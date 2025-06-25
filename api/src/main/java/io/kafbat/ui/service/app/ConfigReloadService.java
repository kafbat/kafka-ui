package io.kafbat.ui.service.app;

import io.kafbat.ui.util.ApplicationRestarter;
import io.kafbat.ui.util.DynamicConfigOperations;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = {"dynamic.config.enabled", "dynamic.config.autoreload"}, havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ConfigReloadService {

  private static final String THREAD_NAME = "config-watcher-thread";
  private static final long STARTUP_SUPPRESSION_MS = 1000;
  private final long appStartedAt = System.currentTimeMillis();

  private final DynamicConfigOperations dynamicConfigOperations;
  private final ApplicationRestarter restarter;

  private WatchService watchService;
  private Thread watcherThread;

  @PostConstruct
  public void init() {
    log.debug("Auto reload is enabled, will watch for config changes");

    try {
      registerWatchService();
      startWatching();
    } catch (IOException e) {
      log.error("Error while registering watch service", e);
    }
  }

  @PreDestroy
  public void shutdown() {
    try {
      if (watchService != null) {
        watchService.close();
      }
    } catch (IOException ignored) {
    }
    if (watcherThread != null) {
      this.watcherThread.interrupt();
    }
  }

  private void registerWatchService() throws IOException {
    this.watchService = FileSystems.getDefault().newWatchService();
    dynamicConfigOperations.dynamicConfigFilePath()
        .getParent()
        .register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
  }

  private void startWatching() {
    watcherThread = new Thread(this::watchLoop, THREAD_NAME);
    watcherThread.start();
  }

  private void watchLoop() {
    final var watchedDir = dynamicConfigOperations.dynamicConfigFilePath().getParent();

    while (true) {
      try {
        WatchKey key = watchService.take();
        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          Path changed = watchedDir.resolve((Path) event.context());

          if (kind != StandardWatchEventKinds.ENTRY_MODIFY) {
            continue;
          }
          if (!changed.equals(dynamicConfigOperations.dynamicConfigFilePath())) {
            continue;
          }

          var now = System.currentTimeMillis();
          if (now - appStartedAt < STARTUP_SUPPRESSION_MS) {
            continue;
          }

          restart();
        }
        key.reset();
      } catch (ClosedWatchServiceException e) {
        log.trace("Watch service closed, exiting watcher thread");
        break;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private void restart() {
    log.info("Application config change detected, restarting");
    restarter.requestRestart();
  }


}
