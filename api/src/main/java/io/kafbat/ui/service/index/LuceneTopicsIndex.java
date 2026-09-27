package io.kafbat.ui.service.index;

import com.google.common.hash.Hashing;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import io.kafbat.ui.service.index.lucene.IndexedTextField;
import io.kafbat.ui.service.index.lucene.NameDistanceScoringFunction;
import io.kafbat.ui.service.index.lucene.ShortWordAnalyzer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;

/**
 * Full text search index over topic names, backed by a long-lived in-memory Lucene index.
 *
 * <p>The instance is meant to outlive a single cluster state snapshot: {@link #update(Collection)}
 * applies only the difference against the topics that are already indexed, so a cluster whose topics
 * rarely change does not pay for re-analyzing and re-indexing every name on every scrape. Queries are
 * served through a {@link SearcherManager}, so a reader reflecting the latest writes is picked up
 * without rebuilding the index and without blocking concurrent searches.
 */
@Slf4j
public class LuceneTopicsIndex implements TopicsIndex {
  public static final String FIELD_NAME_RAW = "name_raw";

  private final Analyzer analyzer;
  private final Directory directory;
  private final IndexWriter writer;
  private final SearcherManager searcherManager;
  private final ReadWriteLock closeLock = new ReentrantReadWriteLock();

  private volatile Map<String, InternalTopic> topicMap = Map.of();
  private volatile Map<String, Long> fingerprints = Map.of();
  private UpdateStats lastUpdateStats = UpdateStats.EMPTY;

  public LuceneTopicsIndex(Collection<InternalTopic> topics) throws IOException {
    this.analyzer = new ShortWordAnalyzer();
    this.directory = new ByteBuffersDirectory();
    this.writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
    // apply all deletes, but do not write them out: the index lives in memory and there is no
    // reason to force a flush to disk. A null factory means the default searcher factory.
    this.searcherManager = new SearcherManager(writer, true, false, null);
    update(topics);
  }

  /**
   * Brings the index in line with the given topics, touching only the ones that actually changed.
   *
   * <p>The fingerprint deliberately leaves {@link TopicsIndex#FIELD_SIZE} out of it: the size of a topic
   * grows between scrapes, so including it would mark nearly every topic dirty on every scrape and
   * make this as expensive as rebuilding the whole index. Sorting and paging are done in memory by the
   * controller, so the only visible consequence is that an explicit {@code size:...} range query sees
   * the size a topic had when it was last indexed.
   */
  public synchronized void update(Collection<InternalTopic> topics) {
    closeLock.readLock().lock();
    try {
      var newTopicMap = byName(topics);
      var newFingerprints = new HashMap<String, Long>(topics.size());
      for (InternalTopic topic : topics) {
        newFingerprints.put(topic.getName(), fingerprint(topic));
      }

      var indexed = fingerprints;
      int added = 0;
      int reindexed = 0;
      for (var entry : newFingerprints.entrySet()) {
        String name = entry.getKey();
        // the fingerprints are boxed Longs, so they have to be compared by value: comparing the
        // references would mark every topic dirty on every update and defeat the whole point
        if (!entry.getValue().equals(indexed.get(name))) {
          writer.updateDocument(new Term(FIELD_NAME_RAW, name), buildDocument(newTopicMap.get(name)));
          if (indexed.containsKey(name)) {
            reindexed++;
          } else {
            added++;
          }
        }
      }
      int removed = 0;
      for (String name : indexed.keySet()) {
        if (!newFingerprints.containsKey(name)) {
          writer.deleteDocuments(new Term(FIELD_NAME_RAW, name));
          removed++;
        }
      }

      this.topicMap = newTopicMap;
      this.fingerprints = newFingerprints;
      this.lastUpdateStats = new UpdateStats(added, reindexed, removed);
      if (added > 0 || reindexed > 0 || removed > 0) {
        searcherManager.maybeRefresh();
        log.debug("Updated full text index: {} added, {} reindexed, {} removed", added, reindexed, removed);
      } else {
        log.trace("Full text index is up to date, nothing to reindex");
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      closeLock.readLock().unlock();
    }
  }

  /**
   * Points the index at the current topic objects without touching any Lucene document.
   *
   * <p>The topic model is rebuilt from scratch on every topic listing, so the index has to follow it
   * right away. {@link #update(Collection)} is only called on a full text search, and until one
   * happens the index would keep the objects of the previous snapshot alive: a whole second
   * generation of topics, their configs and their partitions, for as long as the index lives. On a
   * cluster with 15k topics that is tens of megabytes per cluster.
   */
  void refreshTopics(Collection<InternalTopic> topics) {
    this.topicMap = byName(topics);
  }

  private static Map<String, InternalTopic> byName(Collection<InternalTopic> topics) {
    var map = new HashMap<String, InternalTopic>(topics.size());
    for (InternalTopic topic : topics) {
      map.put(topic.getName(), topic);
    }
    return map;
  }

  /**
   * The topic objects a search currently resolves a matched name against. Exposed for tests that
   * verify the index follows the current snapshot instead of keeping an older one alive.
   */
  Collection<InternalTopic> currentTopics() {
    return topicMap.values();
  }

  /**
   * How many documents the last {@link #update(Collection)} touched. The document count cannot show
   * this on its own, because re-indexing a topic replaces the previous document rather than adding
   * one, so an update that rewrote everything would still report the same number of documents.
   */
  UpdateStats lastUpdateStats() {
    return lastUpdateStats;
  }

  record UpdateStats(int added, int reindexed, int removed) {
    static final UpdateStats EMPTY = new UpdateStats(0, 0, 0);

    int total() {
      return added + reindexed + removed;
    }
  }

  /**
   * Cheap digest of everything that ends up in a document except the size, used to tell whether a topic
   * really needs to be re-indexed. Config entries are sorted because {@code describeConfigs} does not
   * promise a stable order between calls.
   */
  private static long fingerprint(InternalTopic topic) {
    var canonical = new StringBuilder(topic.getName())
        .append('|').append(topic.isInternal())
        .append('|').append(topic.getPartitionCount())
        .append('|').append(topic.getReplicationFactor());
    if (topic.getTopicConfigs() != null) {
      topic.getTopicConfigs().stream()
          .map(config -> config.getName() + "=" + config.getValue())
          .sorted()
          .forEach(config -> canonical.append('|').append(config));
    }
    return Hashing.farmHashFingerprint64().hashUnencodedChars(canonical).asLong();
  }

  private Document buildDocument(InternalTopic topic) {
    Document doc = new Document();

    doc.add(new StringField(FIELD_NAME_RAW, topic.getName(), Field.Store.YES));
    // doc values so that equally scored topics can be ordered by name: an incrementally updated
    // index has no meaningful document order, so the tie has to be broken explicitly
    doc.add(new SortedDocValuesField(FIELD_NAME_RAW, new BytesRef(topic.getName())));
    doc.add(new IndexedTextField(FIELD_NAME, topic.getName(), Field.Store.YES));
    doc.add(new IntPoint(FIELD_PARTITIONS, topic.getPartitionCount()));
    doc.add(new IntPoint(FIELD_REPLICATION, topic.getReplicationFactor()));
    doc.add(new LongPoint(FIELD_SIZE, topic.getSegmentSize()));
    if (topic.getTopicConfigs() != null && !topic.getTopicConfigs().isEmpty()) {
      for (InternalTopicConfig topicConfig : topic.getTopicConfigs()) {
        final String topicConfigValue = topicConfig.getValue();
        if (topicConfigValue != null) {
          doc.add(new StringField(FIELD_CONFIG_PREFIX + "_" + topicConfig.getName(), topicConfig.getValue(),
              Field.Store.NO));
        } else {
          log.info(
              "Topic configuration item '{}' on internal topic '{}' has an unexpected value of null"
              + "; skipping processing", topicConfig.getName(), topic.getName()
          );
        }
      }
    }
    doc.add(new StringField(FIELD_INTERNAL, String.valueOf(topic.isInternal()), Field.Store.NO));
    return doc;
  }

  @Override
  public void close() throws Exception {
    this.closeLock.writeLock().lock();
    try {
      this.searcherManager.close();
      this.writer.close();
      this.directory.close();
    } finally {
      this.closeLock.writeLock().unlock();
    }
  }

  @Override
  public List<InternalTopic> find(String search, Boolean showInternal, String sort,
                                  boolean fts, Integer count) {
    if (!fts) {
      try (FilterTopicIndex filter = new FilterTopicIndex(this.topicMap.values())) {
        return filter.find(search, showInternal, sort, fts, count);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return find(search, showInternal, sort, count, 0.0f);
  }

  public List<InternalTopic> find(String search, Boolean showInternal,
                                  String sortField, Integer count, float minScore) {
    if (search == null || search.isBlank()) {
      return new ArrayList<>(this.topicMap.values());
    }
    closeLock.readLock().lock();
    IndexSearcher searcher = null;
    try {
      searcher = searcherManager.acquire();

      PrefixQueryParser queryParser = new PrefixQueryParser(FIELD_NAME, this.analyzer);
      queryParser.setDefaultOperator(QueryParser.Operator.AND);
      Query nameQuery = queryParser.parse(search);

      Query internalFilter = new TermQuery(new Term(FIELD_INTERNAL, "true"));

      BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
      queryBuilder.add(nameQuery, BooleanClause.Occur.MUST);
      if (showInternal == null || !showInternal) {
        queryBuilder.add(internalFilter, BooleanClause.Occur.MUST_NOT);
      }

      BooleanQuery combined = queryBuilder.build();
      final Query wrapped = new FunctionScoreQuery(
          combined,
          new NameDistanceScoringFunction(FIELD_NAME, queryParser.getPrefixes())
      );

      List<SortField> sortFields = new ArrayList<>();
      sortFields.add(SortField.FIELD_SCORE);
      if (!sortField.equals(FIELD_NAME)) {
        sortFields.add(new SortField(sortField, SortField.Type.INT, true));
      }
      sortFields.add(new SortField(FIELD_NAME_RAW, SortField.Type.STRING));

      Sort sort = new Sort(sortFields.toArray(new SortField[0]));

      int limit = count != null ? count : Math.max(1, searcher.getIndexReader().numDocs());
      TopDocs result = searcher.search(wrapped, limit, sort);

      var topicMap = this.topicMap;
      List<String> topics = new ArrayList<>();
      for (ScoreDoc scoreDoc : result.scoreDocs) {
        if (minScore > 0.00001f && scoreDoc.score < minScore) {
          continue;
        }
        Document document = searcher.storedFields().document(scoreDoc.doc);
        topics.add(document.get(FIELD_NAME_RAW));
      }
      return topics.stream().map(topicMap::get).filter(Objects::nonNull).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    } finally {
      if (searcher != null) {
        try {
          searcherManager.release(searcher);
        } catch (IOException e) {
          log.warn("Error releasing searcher of the topics index", e);
        }
      }
      closeLock.readLock().unlock();
    }
  }

  /**
   * Number of documents a query would currently match against, exposed for tests.
   */
  int searchableDocCount() throws IOException {
    closeLock.readLock().lock();
    try {
      IndexSearcher searcher = searcherManager.acquire();
      try {
        return searcher.getIndexReader().numDocs();
      } finally {
        searcherManager.release(searcher);
      }
    } finally {
      closeLock.readLock().unlock();
    }
  }
}
