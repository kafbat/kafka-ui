package io.kafbat.ui.service;

import static io.kafbat.ui.api.model.AuthType.DISABLED;
import static io.kafbat.ui.api.model.AuthType.OAUTH2;
import static io.kafbat.ui.model.ApplicationInfoDTO.EnabledFeaturesEnum;
import static io.kafbat.ui.util.GithubReleaseInfo.GITHUB_RELEASE_INFO_TIMEOUT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import io.kafbat.ui.model.AppAuthenticationSettingsDTO;
import io.kafbat.ui.model.ApplicationInfoBuildDTO;
import io.kafbat.ui.model.ApplicationInfoDTO;
import io.kafbat.ui.model.ApplicationInfoLatestReleaseDTO;
import io.kafbat.ui.model.AuthTypeDTO;
import io.kafbat.ui.model.OAuthProviderDTO;
import io.kafbat.ui.util.DynamicConfigOperations;
import io.kafbat.ui.util.GithubReleaseInfo;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInfoService {
  private final GithubReleaseInfo githubReleaseInfo;
  private final ApplicationContext applicationContext;
  private final DynamicConfigOperations dynamicConfigOperations;
  private final BuildProperties buildProperties;
  private final GitProperties gitProperties;

  public ApplicationInfoService(DynamicConfigOperations dynamicConfigOperations,
                                ApplicationContext applicationContext,
                                @Autowired(required = false) BuildProperties buildProperties,
                                @Autowired(required = false) GitProperties gitProperties,
                                @Value("${" + GITHUB_RELEASE_INFO_TIMEOUT + ":10}") int githubApiMaxWaitTime) {
    this.applicationContext = applicationContext;
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

  public AppAuthenticationSettingsDTO getAuthenticationProperties() {
    return new AppAuthenticationSettingsDTO()
        .authType(AuthTypeDTO.fromValue(getAuthType()))
        .oAuthProviders(getOAuthProviders());
  }

  private String getAuthType() {
    return Optional.ofNullable(applicationContext.getEnvironment().getProperty("auth.type"))
        .orElse(DISABLED.getValue());
  }

  @SuppressWarnings("unchecked")
  private List<OAuthProviderDTO> getOAuthProviders() {
    if (!getAuthType().equalsIgnoreCase(OAUTH2.getValue())) {
      return Collections.emptyList();
    }
    var type = ResolvableType.forClassWithGenerics(Iterable.class, ClientRegistration.class);
    String[] names = this.applicationContext.getBeanNamesForType(type);
    var bean = (Iterable<ClientRegistration>) (names.length == 1 ? this.applicationContext.getBean(names[0]) : null);

    if (bean == null) {
      return Collections.emptyList();
    }

    return Streams.stream(bean.iterator())
        .filter(r -> AuthorizationGrantType.AUTHORIZATION_CODE.equals(r.getAuthorizationGrantType()))
        .map(r -> new OAuthProviderDTO()
            .clientName(r.getClientName())
            .authorizationUri("/oauth2/authorization/" + r.getRegistrationId()))
        .toList();
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
