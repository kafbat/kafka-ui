package io.kafbat.ui.util;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class GithubReleaseInfo {
  public static final String GITHUB_RELEASE_INFO_ENABLED = "github.release.info.enabled";
  public static final String GITHUB_RELEASE_INFO_TIMEOUT = "github.release.info.timeout";

  private static final String GITHUB_LATEST_RELEASE_RETRIEVAL_URL =
      "https://api.github.com/repos/kafbat/kafka-ui/releases/latest";

  public record GithubReleaseDto(String html_url, String tag_name, String published_at) {

    static GithubReleaseDto empty() {
      return new GithubReleaseDto(null, null, null);
    }
  }

  private volatile GithubReleaseDto release = GithubReleaseDto.empty();

  private final Mono<Void> refreshMono;

  @Getter
  private final int githubApiMaxWaitTime;

  public GithubReleaseInfo(int githubApiMaxWaitTime) {
    this(GITHUB_LATEST_RELEASE_RETRIEVAL_URL, githubApiMaxWaitTime);
  }

  @VisibleForTesting
  GithubReleaseInfo(String url, int githubApiMaxWaitTime) {
    this.githubApiMaxWaitTime = githubApiMaxWaitTime;
    this.refreshMono = new WebClientConfigurator().build()
        .get()
        .uri(url)
        .exchangeToMono(resp -> resp.bodyToMono(GithubReleaseDto.class))
        .timeout(Duration.ofSeconds(this.githubApiMaxWaitTime))
        .doOnError(th -> log.trace("Error getting latest github release info", th))
        .onErrorResume(th -> true, th -> Mono.just(GithubReleaseDto.empty()))
        .doOnNext(release -> this.release = release)
        .then();
  }

  public GithubReleaseDto get() {
    return release;
  }

  public Mono<Void> refresh() {
    return refreshMono;
  }

}
