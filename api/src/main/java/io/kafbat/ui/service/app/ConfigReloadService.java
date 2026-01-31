package io.kafbat.ui.service.app;

import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.util.MultiFileWatcher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "config.autoreload", havingValue = "true")
public class ConfigReloadService {

  private static final String THREAD_NAME = "config-watcher-thread";

  private final ConfigurableEnvironment environment;
  private final RoleBasedAccessControlProperties rbacProperties;
  private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

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
      this.multiFileWatcher = new MultiFileWatcher(propertySourcePaths, this::reloadFile);
      this.watcherThread = new Thread(multiFileWatcher::watchLoop, THREAD_NAME);
      this.watcherThread.start();
    } catch (IOException e) {
      log.error("Error while registering watch service", e);
    }
  }

  private void reloadFile(Path path) {
    log.info("Reloading file {}", path);
    try {
      if (!path.toString().endsWith(".yml") && !path.toString().endsWith(".yaml")) {
        log.trace("Skipping non-YML file {}", path);
      }

      String name = String.format("Config resource 'file [%s] via location '%s'",
          path.toAbsolutePath(),
          path.toAbsolutePath()); // TODO extract an obj reference from env

      List<PropertySource<?>> load = yamlLoader.load(path.toString(), new FileSystemResource(path));
      environment.getPropertySources().remove(name);
      environment.getPropertySources().addFirst(load.get(0));
      Binder binder = Binder.get(environment);

      binder.bind("rbac", RoleBasedAccessControlProperties.class)
          .ifBound(bound -> rbacProperties.setRoles(bound.getRoles()));
    } catch (Throwable e) {
      log.error("Error while reloading file {}", path, e);
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
}
