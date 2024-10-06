package io.kafbat.ui.util;

import java.time.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class GithubReleaseInfo {
  public static final String GITHUB_RELEASE_INFO_TIMEOUT = "github.release.info.timeout";
  private static final String GITHUB_LATEST_RELEASE_RETRIEVAL_URL =
      "https://api.github.com/repos/kafbat/kafka-ui/releases/latest";

  public record GithubReleaseDto(String html_url, String tag_name, String published_at) {
    static GithubReleaseDto empty() {
      return new GithubReleaseDto(null, null, null);
    }
  }

  @Getter
  private final int githubApiMaxWaitTime;
  private volatile GithubReleaseDto release;

  public GithubReleaseInfo(int githubApiMaxWaitTime) {
    this.githubApiMaxWaitTime = githubApiMaxWaitTime;
  }

  public GithubReleaseDto get() {
    if (release != null) {
      return release;
    }

    refresh();
    return release == null ? GithubReleaseDto.empty() : release;
  }

  public void refresh() {
    refresh(GITHUB_LATEST_RELEASE_RETRIEVAL_URL);
  }

  public void refresh(String url) {
    new WebClientConfigurator().build()
        .get()
        .uri(url)
        .exchangeToMono(resp -> resp.bodyToMono(GithubReleaseDto.class))
        .timeout(Duration.ofSeconds(githubApiMaxWaitTime))
        .doOnError(th -> log.error("Failed to retrieve latest release info", th))
        .onErrorResume(th -> true, th -> Mono.just(GithubReleaseDto.empty()))
        .doOnNext(r -> this.release = r)
        .block();
  }



}
