package io.kafbat.ui.pages.panels;

import static com.codeborne.selenide.Selenide.$x;

import com.codeborne.selenide.SelenideElement;
import io.kafbat.ui.pages.BasePage;
import java.util.Arrays;
import java.util.List;

public class TopPanel extends BasePage {

  protected SelenideElement kafkaLogo = $x("//a[contains(text(),'Kafbat UI')]");
  protected SelenideElement kafkaVersion = $x("//a[@title='Current commit']");
  protected SelenideElement logOutBtn = $x("//button[contains(text(),'Log out')]");
  protected SelenideElement gitBtn = $x("//a[@href='https://github.com/kafbat/kafka-ui']");
  protected SelenideElement discordBtn = $x("//a[contains(@href,'https://discord.com/invite')]");

  public List<SelenideElement> getAllVisibleElements() {
    return Arrays.asList(kafkaLogo, kafkaVersion, gitBtn, discordBtn);
  }

  public List<SelenideElement> getAllEnabledElements() {
    return Arrays.asList(gitBtn, discordBtn, kafkaLogo);
  }
}
