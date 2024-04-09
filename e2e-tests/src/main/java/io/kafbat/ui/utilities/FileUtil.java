package io.kafbat.ui.utilities;

import static org.apache.kafka.common.utils.Utils.readFileAsString;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public class FileUtil {

  public static String resourceToString(String resourcePath) {
    try {
      return IOUtils.resourceToString("/" + resourcePath, StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  public static String fileToString(String filePath) {
    try {
      return readFileAsString(filePath);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }
}
