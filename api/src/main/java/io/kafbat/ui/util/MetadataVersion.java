package io.kafbat.ui.util;

import java.util.Arrays;
import java.util.Optional;

public enum MetadataVersion {
  IBP_3_0_IV1(1, "3.0-IV1"),
  IBP_3_1_IV0(2, "3.1-IV0"),
  IBP_3_2_IV0(3, "3.2-IV0"),
  IBP_3_3_IV0(4, "3.3-IV0"),
  IBP_3_3_IV1(5, "3.3-IV1"),
  IBP_3_3_IV2(6, "3.3-IV2"),
  IBP_3_3_IV3(7, "3.3-IV3"),
  IBP_3_4_IV0(8, "3.4-IV0"),
  IBP_3_5_IV0(9, "3.5-IV0"),
  IBP_3_5_IV1(10, "3.5-IV1"),
  IBP_3_5_IV2(11, "3.5-IV2"),
  IBP_3_6_IV0(12, "3.6-IV0"),
  IBP_3_6_IV1(13, "3.6-IV1"),
  IBP_3_6_IV2(14, "3.6-IV2"),
  IBP_3_7_IV0(15, "3.7-IV0"),
  IBP_3_7_IV1(16, "3.7-IV1"),
  IBP_3_7_IV2(17, "3.7-IV2"),
  IBP_3_7_IV3(18, "3.7-IV3"),
  IBP_3_7_IV4(19, "3.7-IV4"),
  IBP_3_8_IV0(20, "3.8-IV0"),
  IBP_3_9_IV0(21, "3.9-IV0"),
  IBP_4_0_IV0(22, "4.0-IV0"),
  IBP_4_0_IV1(23, "4.0-IV1"),
  IBP_4_0_IV2(24, "4.0-IV2"),
  IBP_4_0_IV3(25, "4.0-IV3"),
  IBP_4_1_IV0(26, "4.1-IV0"),
  IBP_4_1_IV1(27, "4.1-IV1"),
  IBP_4_2_IV0(28, "4.2-IV0"),
  IBP_4_2_IV1(29, "4.2-IV1");

  private final int featureLevel;
  private final String release;

  MetadataVersion(int featureLevel, String release) {
    this.featureLevel = featureLevel;
    this.release = release;
  }

  public static Optional<String> findVersion(int featureLevel) {
    return Arrays.stream(values())
        .filter(v -> v.featureLevel == featureLevel)
        .findFirst().map(v -> v.release);
  }

}
