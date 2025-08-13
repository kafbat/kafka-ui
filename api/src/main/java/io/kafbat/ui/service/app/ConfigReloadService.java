package io.kafbat.ui.service.app;

import io.kafbat.ui.util.MultiFileWatcher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
//@ConditionalOnProperty(value = "dynamic.config.autoreload", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ConfigReloadService {

  private static final String THREAD_NAME = "config-watcher-thread";

  private final ConfigurableEnvironment environment;

  private Thread watcherThread;
  private MultiFileWatcher multiFileWatcher;

  @PostConstruct
  public void init() {
    var propertySourcePaths = StreamSupport.stream(environment.getPropertySources().spliterator(), false)
        .filter(OriginTrackedMapPropertySource.class::isInstance)
        .map(OriginTrackedMapPropertySource.class::cast)
        .flatMap(ps -> ps.getSource().values().stream())
        .map(v -> (v instanceof OriginTrackedValue otv) ? otv.getOrigin() : null)
        .filter(Objects::nonNull)
        .flatMap(o -> Stream.iterate(o, Objects::nonNull, Origin::getParent))
        .filter(TextResourceOrigin.class::isInstance)
        .map(TextResourceOrigin.class::cast)
        .map(TextResourceOrigin::getResource)
        .filter(Objects::nonNull)
        .filter(Resource::exists)
        .filter(Resource::isReadable)
        .filter(Resource::isFile)
        .map(r -> {
          try {
            return r.getURI();
          } catch (IOException e) {
            log.error("can't retrieve resource URL", e);
            return null;
          }
        })
        .filter(Objects::nonNull)
        .map(Paths::get)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    if (propertySourcePaths.isEmpty()) {
      log.debug("No config files found, auto reload is disabled");
      return;
    }

    log.debug("Auto reload is enabled, will watch for config changes");

    try {
      this.multiFileWatcher = new MultiFileWatcher(propertySourcePaths, this::reload);
      this.watcherThread = new Thread(multiFileWatcher::watchLoop, THREAD_NAME);
      this.watcherThread.start();
    } catch (IOException e) {
      log.error("Error while registering watch service", e);
    }
  }

  @PreDestroy
  public void shutdown() {
    try {
      if (multiFileWatcher != null) {
        multiFileWatcher.close();
      }
    } catch (IOException ignored) {
    }
    if (watcherThread != null) {
      this.watcherThread.interrupt();
    }
  }

  private void reload() {

  }


}
