package io.kafbat.ui.service.app;

import io.kafbat.ui.util.ApplicationRestarter;
import io.kafbat.ui.util.DynamicConfigOperations;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;

@Service
//@ConditionalOnProperty(value = "dynamic.config.autoreload", havingValue = "true")
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

  private final ApplicationContext context;
  private final ConfigurableEnvironment environment;

  @PostConstruct
  public void init() {

/*    environment.getPropertySources()
        .stream()
        .filter(ps -> ps instanceof OriginTrackedMapPropertySource)
        .map(ps -> (OriginTrackedMapPropertySource)ps)
        .map(ps -> ps.getSource())
        .map(source -> source.values())
        .map(values -> {
          return (HashMap<String, String>) values;
        })
//    .map(sourceValues -> sourceValues.)
        .collect(Collectors.toUnmodifiableList());*/


    // =============

/*    environment.getPropertySources().stream()
        .filter(ps -> ps instanceof EnumerablePropertySource)
        .filter(ps -> ps instanceof OriginLookup)
        .flatMap(ps -> {
          EnumerablePropertySource<?> eps = (EnumerablePropertySource<?>) ps;
          OriginLookup<String> lookup = (OriginLookup<String>) ps;
          return Arrays.stream(eps.getPropertyNames())
              .map(name -> {
                Origin origin = lookup.getOrigin(name);
                return origin != null ? origin.toString() : null;
              });
        })
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toUnmodifiableList());*/

    // ===============

/*    Map<String, Object> map = new HashMap();
    for(Iterator it = ((AbstractEnvironment) environment).getPropertySources().iterator(); it.hasNext(); ) {
      PropertySource propertySource = (PropertySource) it.next();
      if (propertySource instanceof MapPropertySource) {
        map.putAll(((MapPropertySource) propertySource).getSource());
      }
    }*/

    // ====

    SpringConfigurableEnvironment properties = new SpringConfigurableEnvironment(springEnv);
    SpringConfigurableEnvironment.PropertyInfo info = properties.get("profile.env");
    assertEquals("default", properties.get(info.getValue());
    assertEquals(
        "Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'",
        info.getSourceList.get(0));




    System.out.println();
//    environment.getPropertySources()
//        .stream()

    var configPath = dynamicConfigOperations.dynamicConfigFilePath();
    if (!Files.exists(configPath) || !Files.isReadable(configPath)) {
      log.warn("Dynamic config file {} doesnt exist or is not readable. Auto reload is disabled", configPath);
      return;
    }

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
