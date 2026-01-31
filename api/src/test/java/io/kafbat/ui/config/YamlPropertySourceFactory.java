package io.kafbat.ui.config;

import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

public class YamlPropertySourceFactory implements PropertySourceFactory {
  private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

  @Override
  public @NotNull PropertySource<?> createPropertySource(String name, EncodedResource resource)
      throws IOException {
    List<PropertySource<?>> loaded = loader.load(name, resource.getResource());
    if (loaded.size() == 1) {
      return loaded.get(0);
    } else {
      throw new IOException(resource.getResource().getFilename());
    }
  }
}
