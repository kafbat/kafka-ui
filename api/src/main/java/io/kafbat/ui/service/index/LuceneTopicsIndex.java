package io.kafbat.ui.service.index;

import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import io.kafbat.ui.service.index.lucene.IndexedTextField;
import io.kafbat.ui.service.index.lucene.NameDistanceScoringFunction;
import io.kafbat.ui.service.index.lucene.ShortWordAnalyzer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
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
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

@Slf4j
public class LuceneTopicsIndex implements TopicsIndex {
  public static final String FIELD_NAME_RAW = "name_raw";

  private final Directory directory;
  private final DirectoryReader indexReader;
  private final IndexSearcher indexSearcher;
  private final Analyzer analyzer;
  private final int maxSize;
  private final ReadWriteLock closeLock = new ReentrantReadWriteLock();
  private final Map<String, InternalTopic> topicMap;

  public LuceneTopicsIndex(List<InternalTopic> topics) throws IOException {
    this.analyzer = new ShortWordAnalyzer();
    this.topicMap = topics.stream().collect(Collectors.toMap(InternalTopic::getName, Function.identity()));
    this.directory = build(topics);
    this.indexReader = DirectoryReader.open(directory);
    this.indexSearcher = new IndexSearcher(indexReader);
    this.maxSize = topics.size();
  }

  private Directory build(List<InternalTopic> topics) {
    Directory directory = new ByteBuffersDirectory();

    try (IndexWriter directoryWriter = new IndexWriter(directory, new IndexWriterConfig(this.analyzer))) {
      for (InternalTopic topic : topics) {
        Document doc = new Document();

        doc.add(new StringField(FIELD_NAME_RAW, topic.getName(), Field.Store.YES));
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
        directoryWriter.addDocument(doc);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return directory;
  }

  @Override
  public void close() throws Exception {
    this.closeLock.writeLock().lock();
    try {
      if (indexReader != null) {
        this.indexReader.close();
      }
      if (this.directory != null) {
        this.directory.close();
      }
    } finally {
      this.closeLock.writeLock().unlock();
    }
  }

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
    try {

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
      Query wrapped = new FunctionScoreQuery(
          combined,
          new NameDistanceScoringFunction(FIELD_NAME, queryParser.getPrefixes())
      );

      List<SortField> sortFields = new ArrayList<>();
      sortFields.add(SortField.FIELD_SCORE);
      if (!sortField.equals(FIELD_NAME)) {
        sortFields.add(new SortField(sortField, SortField.Type.INT, true));
      }

      Sort sort = new Sort(sortFields.toArray(new SortField[0]));

      TopDocs result = this.indexSearcher.search(wrapped, count != null ? count : this.maxSize, sort);

      List<String> topics = new ArrayList<>();
      for (ScoreDoc scoreDoc : result.scoreDocs) {
        if (minScore > 0.00001f && scoreDoc.score < minScore) {
          continue;
        }
        Document document = this.indexSearcher.storedFields().document(scoreDoc.doc);
        topics.add(document.get(FIELD_NAME_RAW));
      }
      return topics.stream().map(topicMap::get).filter(Objects::nonNull).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    } finally {
      this.closeLock.readLock().unlock();
    }
  }
}
