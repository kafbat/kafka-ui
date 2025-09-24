package io.kafbat.ui.util;

import java.time.Duration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

// representer, that skips fields with null values
public class YamlNullSkipRepresenter extends Representer {
  public YamlNullSkipRepresenter(DumperOptions options) {
    super(options);
    this.representers.put(Duration.class, data -> this.representScalar(Tag.STR, data.toString()));
  }

  @Override
  protected NodeTuple representJavaBeanProperty(Object javaBean,
                                                Property property,
                                                Object propertyValue,
                                                Tag customTag) {
    if (propertyValue == null) {
      return null; // if value of property is null, ignore it.
    } else {
      return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
    }
  }
}
