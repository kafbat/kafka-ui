package io.kafbat.ui.smokesuite.schemas;

import com.codeborne.selenide.Condition;
import io.kafbat.ui.BaseTest;
import io.kafbat.ui.api.model.CompatibilityLevel;
import io.kafbat.ui.models.Schema;
import io.kafbat.ui.utilities.FileUtil;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class SchemasTest extends BaseTest {

  private static final Schema AVRO_SCHEMA = Schema.createSchemaAvro();
  private static final Schema JSON_SCHEMA = Schema.createSchemaJson();
  private static final Schema PROTOBUF_SCHEMA = Schema.createSchemaProtobuf();
  private static final List<Schema> SCHEMA_LIST = new ArrayList<>();

  @BeforeClass(alwaysRun = true)
  public void beforeClass() {
    SCHEMA_LIST.addAll(List.of(AVRO_SCHEMA, JSON_SCHEMA, PROTOBUF_SCHEMA));
    SCHEMA_LIST.forEach(schema -> apiService.createSchema(schema));
  }

  @Test(priority = 1)
  public void createSchemaAvroCheck() {
    Schema schemaAvro = Schema.createSchemaAvro();
    navigateToSchemaRegistry();
    schemaRegistryList
        .clickCreateSchema();
    schemaCreateForm
        .setSubjectName(schemaAvro.getName())
        .setSchemaField(FileUtil.fileToString(schemaAvro.getValuePath()))
        .selectSchemaTypeFromDropdown(schemaAvro.getType())
        .clickSubmitButton();
    schemaDetails
        .waitUntilScreenReady();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(schemaDetails.isSchemaHeaderVisible(schemaAvro.getName()),
        String.format("isSchemaHeaderVisible()[%s]", schemaAvro.getName()));
    softly.assertEquals(schemaDetails.getSchemaType(), schemaAvro.getType().getValue(), "getSchemaType()");
    softly.assertEquals(schemaDetails.getCompatibility(), CompatibilityLevel.CompatibilityEnum.BACKWARD.getValue(),
        "getCompatibility()");
    softly.assertAll();
    navigateToSchemaRegistry();
    Assert.assertTrue(schemaRegistryList.isSchemaVisible(AVRO_SCHEMA.getName()),
        String.format("isSchemaVisible()[%s]", AVRO_SCHEMA.getName()));
    SCHEMA_LIST.add(schemaAvro);
  }

  @Test(priority = 2)
  public void updateSchemaAvroCheck() {
    AVRO_SCHEMA.setValuePath(
        System.getProperty("user.dir") + "/src/main/resources/testdata/schemas/schema_avro_update.json");
    navigateToSchemaRegistryAndOpenDetails(AVRO_SCHEMA.getName());
    int latestVersion = schemaDetails
        .getLatestVersion();
    schemaDetails
        .openEditSchema();
    schemaCreateForm
        .waitUntilScreenReady();
    verifyElementsCondition(schemaCreateForm.getAllDetailsPageElements(), Condition.visible);
    SoftAssert softly = new SoftAssert();
    softly.assertFalse(schemaCreateForm.isSubmitBtnEnabled(), "isSubmitBtnEnabled()");
    softly.assertFalse(schemaCreateForm.isSchemaDropDownEnabled(), "isSchemaDropDownEnabled()");
    softly.assertAll();
    schemaCreateForm
        .selectCompatibilityLevelFromDropdown(CompatibilityLevel.CompatibilityEnum.NONE)
        .setNewSchemaValue(FileUtil.fileToString(AVRO_SCHEMA.getValuePath()))
        .clickSubmitButton();
    schemaDetails
        .waitUntilScreenReady();
    softly.assertEquals(schemaDetails.getLatestVersion(), latestVersion + 1, "getLatestVersion()");
    softly.assertEquals(schemaDetails.getCompatibility(), CompatibilityLevel.CompatibilityEnum.NONE.toString(),
        "getCompatibility()");
    softly.assertAll();
  }

  @Test(priority = 3)
  public void compareVersionsCheck() {
    navigateToSchemaRegistryAndOpenDetails(AVRO_SCHEMA.getName());
    int latestVersion = schemaDetails
        .waitUntilScreenReady()
        .getLatestVersion();
    schemaDetails
        .openCompareVersionMenu();
    int versionsNumberFromDdl = schemaCreateForm
        .waitUntilScreenReady()
        .openLeftVersionDdl()
        .getVersionsNumberFromList();
    Assert.assertEquals(versionsNumberFromDdl, latestVersion, "Versions number is not matched");
    schemaCreateForm
        .selectVersionFromDropDown(latestVersion)
        .openRightVersionDdl()
        .selectVersionFromDropDown(latestVersion - 1);
    Assert.assertEquals(schemaCreateForm.getMarkedLinesNumber(), 42, "getMarkedLinesNumber()");
  }

  @Test(priority = 4)
  public void deleteSchemaAvroCheck() {
    navigateToSchemaRegistryAndOpenDetails(AVRO_SCHEMA.getName());
    schemaDetails
        .removeSchema();
    schemaRegistryList
        .waitUntilScreenReady();
    Assert.assertFalse(schemaRegistryList.isSchemaVisible(AVRO_SCHEMA.getName()),
        String.format("isSchemaVisible()[%s]", AVRO_SCHEMA.getName()));
    SCHEMA_LIST.remove(AVRO_SCHEMA);
  }

  @Test(priority = 5)
  public void createSchemaJsonCheck() {
    Schema schemaJson = Schema.createSchemaJson();
    navigateToSchemaRegistry();
    schemaRegistryList
        .clickCreateSchema();
    schemaCreateForm
        .setSubjectName(schemaJson.getName())
        .setSchemaField(FileUtil.fileToString(schemaJson.getValuePath()))
        .selectSchemaTypeFromDropdown(schemaJson.getType())
        .clickSubmitButton();
    schemaDetails
        .waitUntilScreenReady();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(schemaDetails.isSchemaHeaderVisible(schemaJson.getName()),
        String.format("isSchemaHeaderVisible()[%s]", schemaJson.getName()));
    softly.assertEquals(schemaDetails.getSchemaType(), schemaJson.getType().getValue(), "getSchemaType()");
    softly.assertEquals(schemaDetails.getCompatibility(), CompatibilityLevel.CompatibilityEnum.BACKWARD.getValue(),
        "getCompatibility()");
    softly.assertAll();
    navigateToSchemaRegistry();
    Assert.assertTrue(schemaRegistryList.isSchemaVisible(JSON_SCHEMA.getName()),
        String.format("isSchemaVisible()[%s]", JSON_SCHEMA.getName()));
    SCHEMA_LIST.add(schemaJson);
  }

  @Test(priority = 6)
  public void deleteSchemaJsonCheck() {
    navigateToSchemaRegistryAndOpenDetails(JSON_SCHEMA.getName());
    schemaDetails
        .removeSchema();
    schemaRegistryList
        .waitUntilScreenReady();
    Assert.assertFalse(schemaRegistryList.isSchemaVisible(JSON_SCHEMA.getName()),
        String.format("isSchemaVisible()[%s]", JSON_SCHEMA.getName()));
    SCHEMA_LIST.remove(JSON_SCHEMA);
  }

  @Test(priority = 7)
  public void createSchemaProtobufCheck() {
    Schema schemaProtobuf = Schema.createSchemaProtobuf();
    navigateToSchemaRegistry();
    schemaRegistryList
        .clickCreateSchema();
    schemaCreateForm
        .setSubjectName(schemaProtobuf.getName())
        .setSchemaField(FileUtil.fileToString(schemaProtobuf.getValuePath()))
        .selectSchemaTypeFromDropdown(schemaProtobuf.getType())
        .clickSubmitButton();
    schemaDetails
        .waitUntilScreenReady();
    SoftAssert softly = new SoftAssert();
    softly.assertTrue(schemaDetails.isSchemaHeaderVisible(schemaProtobuf.getName()),
        String.format("isSchemaHeaderVisible()[%s]", schemaProtobuf.getName()));
    softly.assertEquals(schemaDetails.getSchemaType(), schemaProtobuf.getType().getValue(), "getSchemaType()");
    softly.assertEquals(schemaDetails.getCompatibility(), CompatibilityLevel.CompatibilityEnum.BACKWARD.getValue(),
        "getCompatibility()");
    softly.assertAll();
    navigateToSchemaRegistry();
    Assert.assertTrue(schemaRegistryList.isSchemaVisible(PROTOBUF_SCHEMA.getName()),
        String.format("isSchemaVisible()[%s]", PROTOBUF_SCHEMA.getName()));
    SCHEMA_LIST.add(schemaProtobuf);
  }

  @Test(priority = 8)
  public void deleteSchemaProtobufCheck() {
    navigateToSchemaRegistryAndOpenDetails(PROTOBUF_SCHEMA.getName());
    schemaDetails
        .removeSchema();
    schemaRegistryList
        .waitUntilScreenReady();
    Assert.assertFalse(schemaRegistryList.isSchemaVisible(PROTOBUF_SCHEMA.getName()),
        String.format("isSchemaVisible()[%s]", PROTOBUF_SCHEMA.getName()));
    SCHEMA_LIST.remove(PROTOBUF_SCHEMA);
  }

  @AfterClass(alwaysRun = true)
  public void afterClass() {
    SCHEMA_LIST.forEach(schema -> apiService.deleteSchema(schema.getName()));
  }
}
