package io.kafbat.ui.settings;

import static io.kafbat.ui.utilities.BooleanUtil.parseBoolean;
import static io.kafbat.ui.utilities.StringUtil.getOptionalString;
import static org.apache.commons.lang3.BooleanUtils.TRUE;

public abstract class BaseSource {

  public static final boolean HEADLESS = parseBoolean(getOptionalString(TRUE, System.getProperty("headless")));
  public static final boolean SELENOID = true;
  public static final String CLUSTER_NAME = "local";
  public static final String CONNECT_NAME = "first";
  private static final String LOCAL_HOST = "localhost";
  public static final String REMOTE_URL = String.format("http://%s:4444/wd/hub", LOCAL_HOST);
  public static final String BASE_API_URL = String.format("http://%s:8080", LOCAL_HOST);
  public static final String BASE_HOST = true ? "host.docker.internal" : LOCAL_HOST;
  public static final String BASE_UI_URL = String.format("http://%s:8080", BASE_HOST);
}
