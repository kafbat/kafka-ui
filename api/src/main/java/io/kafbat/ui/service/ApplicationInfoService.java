package io.kafbat.ui.service;

import static io.kafbat.ui.model.ApplicationInfoDTO.EnabledFeaturesEnum;
import static io.kafbat.ui.util.GithubReleaseInfo.GITHUB_RELEASE_INFO_TIMEOUT;

import com.google.common.annotations.VisibleForTesting;
import io.kafbat.ui.model.ApplicationInfoBuildDTO;
import io.kafbat.ui.model.ApplicationInfoDTO;
import io.kafbat.ui.model.ApplicationInfoLatestReleaseDTO;
import io.kafbat.ui.util.DynamicConfigOperations;
import io.kafbat.ui.util.GithubReleaseInfo;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInfoService {
  private final GithubReleaseInfo githubReleaseInfo;
  private final DynamicConfigOperations dynamicConfigOperations;
  private final BuildProperties buildProperties;
  private final GitProperties gitProperties;

  public ApplicationInfoService(DynamicConfigOperations dynamicConfigOperations,
                                @Autowired(required = false) BuildProperties buildProperties,
                                @Autowired(required = false) GitProperties gitProperties,
                                @Value("${" + GITHUB_RELEASE_INFO_TIMEOUT + ":10}") int githubApiMaxWaitTime) {
    this.dynamicConfigOperations = dynamicConfigOperations;
    this.buildProperties = Optional.ofNullable(buildProperties).orElse(new BuildProperties(new Properties()));
    this.gitProperties = Optional.ofNullable(gitProperties).orElse(new GitProperties(new Properties()));
    githubReleaseInfo = new GithubReleaseInfo(githubApiMaxWaitTime);
  }

  public ApplicationInfoDTO getApplicationInfo() {
    var releaseInfo = githubReleaseInfo.get();
    return new ApplicationInfoDTO()
        .build(getBuildInfo(releaseInfo))
        .enabledFeatures(getEnabledFeatures())
        .latestRelease(convert(releaseInfo));
  }

  private ApplicationInfoLatestReleaseDTO convert(GithubReleaseInfo.GithubReleaseDto releaseInfo) {
    return new ApplicationInfoLatestReleaseDTO()
        .htmlUrl(releaseInfo.html_url())
        .publishedAt(releaseInfo.published_at())
        .versionTag(releaseInfo.tag_name());
  }

  private ApplicationInfoBuildDTO getBuildInfo(GithubReleaseInfo.GithubReleaseDto release) {
    return new ApplicationInfoBuildDTO()
        .isLatestRelease(release.tag_name() != null && release.tag_name().equals(buildProperties.getVersion()))
        .commitId(gitProperties.getShortCommitId())
        .version(buildProperties.getVersion())
        .buildTime(buildProperties.getTime() != null
            ? DateTimeFormatter.ISO_INSTANT.format(buildProperties.getTime()) : null);
  }

  private List<EnabledFeaturesEnum> getEnabledFeatures() {
    var enabledFeatures = new ArrayList<EnabledFeaturesEnum>();
    if (dynamicConfigOperations.dynamicConfigEnabled()) {
      enabledFeatures.add(EnabledFeaturesEnum.DYNAMIC_CONFIG);
    }
    return enabledFeatures;
  }

  // updating on startup and every hour
  @Scheduled(fixedRateString = "${github-release-info-update-rate:3600000}")
  public void updateGithubReleaseInfo() {
    githubReleaseInfo.refresh().subscribe();
  }

  @VisibleForTesting
  GithubReleaseInfo githubReleaseInfo() {
    return githubReleaseInfo;
  }
}
