package io.kafbat.ui.service.index;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Keeps one {@link LazyTopicsIndex} per cluster for the lifetime of the application.
 *
 * <p>The topic index holds a Lucene index that is expensive to build, and rebuilding it for every
 * cluster state snapshot means re-indexing every topic on every scrape. Clusters are fixed for the
 * lifetime of the process, so the indexes are created on first use and only released on shutdown.
 */
@Slf4j
@Component
public class TopicsIndexRegistry {

  private final Map<String, LazyTopicsIndex> indexes = new ConcurrentHashMap<>();

  public LazyTopicsIndex get(String clusterName) {
    return indexes.computeIfAbsent(clusterName, name -> new LazyTopicsIndex(List.of()));
  }

  @PreDestroy
  public void close() {
    indexes.forEach((clusterName, index) -> {
      try {
        index.close();
      } catch (Exception e) {
        log.error("Error closing topics index for cluster {}", clusterName, e);
      }
    });
    indexes.clear();
  }
}
