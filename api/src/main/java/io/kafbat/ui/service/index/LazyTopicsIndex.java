package io.kafbat.ui.service.index;

import io.kafbat.ui.model.InternalTopic;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link TopicsIndex} that only materializes the Lucene index when a query actually asks for full text
 * search.
 *
 * <p>Building a Lucene index is linear in the number of topics in the cluster and happens on every
 * scrape, while the overwhelming majority of deployments never enable FTS. Deferring the build until
 * the first {@code fts=true} query keeps topic listing at the cost of a plain in-memory filter.
 */
@Slf4j
public class LazyTopicsIndex implements TopicsIndex {

  private final List<InternalTopic> topics;
  private final TopicsIndex plainIndex;
  private volatile TopicsIndex ftsIndex;

  public LazyTopicsIndex(List<InternalTopic> topics) {
    this.topics = topics;
    this.plainIndex = new FilterTopicIndex(topics);
  }

  @Override
  public List<InternalTopic> find(String search, Boolean showInternal, String sort,
                                  boolean fts, Integer count) {
    return fts ? ftsIndex().find(search, showInternal, sort, fts, count)
        : plainIndex.find(search, showInternal, sort, fts, count);
  }

  private TopicsIndex ftsIndex() {
    var index = ftsIndex;
    if (index == null) {
      synchronized (this) {
        index = ftsIndex;
        if (index == null) {
          index = buildFtsIndex();
          ftsIndex = index;
        }
      }
    }
    return index;
  }

  private TopicsIndex buildFtsIndex() {
    try {
      return new LuceneTopicsIndex(topics);
    } catch (Exception e) {
      log.error("Error creating lucene topics index, falling back to filter based search", e);
      return plainIndex;
    }
  }

  @Override
  public void close() throws Exception {
    var index = ftsIndex;
    if (index != null) {
      index.close();
    }
    plainIndex.close();
  }
}
