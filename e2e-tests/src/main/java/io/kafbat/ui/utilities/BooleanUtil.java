package io.kafbat.ui.utilities;

import static io.kafbat.ui.utilities.StringUtil.clearString;
import static org.apache.commons.lang3.BooleanUtils.FALSE;
import static org.apache.commons.lang3.BooleanUtils.TRUE;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import org.apache.commons.lang3.StringUtils;

public class BooleanUtil {

  public static boolean getOptionalBoolean(boolean defaultValue, boolean... customValue) {
    return !isEmpty(customValue) ? customValue[0] : defaultValue;
  }

  public static boolean parseBoolean(String original) {
    String cleanStr = clearString(original);
    if (StringUtils.isEmpty(cleanStr)) {
      throw new IllegalStateException("Unexpected value: " + original);
    } else if (cleanStr.equalsIgnoreCase(TRUE)) {
      return true;
    } else if (cleanStr.equalsIgnoreCase(FALSE)) {
      return false;
    } else {
      throw new IllegalStateException("Unexpected value: " + original);
    }
  }
}
