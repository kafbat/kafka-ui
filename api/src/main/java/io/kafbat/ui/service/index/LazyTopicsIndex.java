package io.kafbat.ui.service.index;

import io.kafbat.ui.model.InternalTopic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link TopicsIndex} of a single cluster that outlives the individual cluster state snapshots.
 *
 * <p>Building a Lucene index is linear in the number of topics and the topics of a cluster do not
 * change that often, so a single instance is kept per cluster and reused across scrapes instead of
 * being rebuilt from scratch. The Lucene index itself is still created lazily, on the first query
 * that actually asks for full text search, and it is only brought in line with the current topic set
 * at that point: a deployment that never enables FTS never pays for indexing anything.
 */
@Slf4j
public class LazyTopicsIndex implements TopicsIndex {

  private volatile List<InternalTopic> topics;
  private final ConcurrentMap<String, InternalTopic> updates = new ConcurrentHashMap<>();
  private volatile LuceneTopicsIndex luceneIndex;
  private volatile boolean luceneUnavailable;

  public LazyTopicsIndex(Collection<InternalTopic> topics) {
    this.topics = List.copyOf(topics);
  }

  /**
   * Replaces the topic set this index answers queries for. Deliberately cheap: the Lucene index is
   * synced with the new topics lazily, on the next full text search. The one thing that is not
   * deferred is following the topic objects themselves, see
   * {@link LuceneTopicsIndex#refreshTopics(Collection)}.
   */
  public synchronized void update(Collection<InternalTopic> topics) {
    this.topics = List.copyOf(topics);
    updates.clear();
    var index = luceneIndex;
    if (index != null) {
      index.refreshTopics(this.topics);
    }
  }

  /**
   * Updates only the topics that were actually fetched, without rebuilding all the others.
   *
   * <p>The snapshot is republished as a list of references, so a topic listing with 10k+ topics no
   * longer pays for converting every topic and every partition on every page request. New topics are
   * appended, existing ones keep their position in the snapshot.
   */
  public synchronized void updateTopics(Collection<InternalTopic> changedTopics) {
    if (changedTopics.isEmpty()) {
      return;
    }
    changedTopics.forEach(topic -> updates.put(topic.getName(), topic));
    var index = luceneIndex;
    if (index != null) {
      index.updateTopics(changedTopics);
    }
    this.topics = mergedTopics();
  }

  /**
   * The currently visible topic objects keyed by name, so callers can reuse them when their source
   * data has not changed.
   */
  public Map<String, InternalTopic> topicByName() {
    var map = HashMap.<String, InternalTopic>newHashMap(topics.size());
    topics.forEach(topic -> map.put(topic.getName(), topic));
    return map;
  }

  private List<InternalTopic> mergedTopics() {
    if (updates.isEmpty()) {
      return topics;
    }
    var merged = new ArrayList<InternalTopic>(topics.size() + updates.size());
    var knownNames = new HashSet<String>(updates.size());
    for (var topic : topics) {
      var updated = updates.get(topic.getName());
      merged.add(updated != null ? updated : topic);
      knownNames.add(topic.getName());
    }
    updates.forEach((name, topic) -> {
      if (!knownNames.contains(name)) {
        merged.add(topic);
      }
    });
    return List.copyOf(merged);
  }

  @Override
  public List<InternalTopic> find(String search, Boolean showInternal, String sort,
                                  boolean fts, Integer count) {
    if (!fts) {
      return new FilterTopicIndex(topics).find(search, showInternal, sort, fts, count);
    }
    return ftsIndex().find(search, showInternal, sort, fts, count);
  }

  private TopicsIndex ftsIndex() {
    var index = luceneIndex;
    if (index == null) {
      synchronized (this) {
        if (luceneIndex == null && !luceneUnavailable) {
          try {
            luceneIndex = new LuceneTopicsIndex(topics);
          } catch (Exception e) {
            log.error("Error creating lucene topics index, falling back to filter based search", e);
            luceneUnavailable = true;
          }
        }
        index = luceneIndex;
      }
    }
    if (index == null) {
      return new FilterTopicIndex(topics);
    }
    index.updateIfStale(topics);
    return index;
  }

  @Override
  public void close() throws Exception {
    var index = luceneIndex;
    if (index != null) {
      index.close();
    }
  }
}
