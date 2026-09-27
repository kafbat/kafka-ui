package io.kafbat.ui.service.index;

import io.kafbat.ui.model.InternalTopic;
import java.util.List;
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
  private volatile LuceneTopicsIndex luceneIndex;
  private volatile boolean luceneUnavailable;

  public LazyTopicsIndex(List<InternalTopic> topics) {
    this.topics = topics;
  }

  /**
   * Replaces the topic set this index answers queries for. Deliberately cheap: the Lucene index is
   * synced with the new topics lazily, on the next full text search. The one thing that is not
   * deferred is following the topic objects themselves, see
   * {@link LuceneTopicsIndex#refreshTopics(Collection)}.
   */
  public void update(List<InternalTopic> topics) {
    this.topics = topics;
    var index = luceneIndex;
    if (index != null) {
      index.refreshTopics(topics);
    }
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
    // idempotent, and a no-op when the topics did not change since the last search
    index.update(topics);
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
