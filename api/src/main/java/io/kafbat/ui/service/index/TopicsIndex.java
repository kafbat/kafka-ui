package io.kafbat.ui.service.index;

import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
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

public class TopicsIndex implements AutoCloseable {
  public static final String FIELD_NAME_RAW = "name_raw";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_INTERNAL = "internal";
  public static final String FIELD_PARTITIONS = "partitions";
  public static final String FIELD_REPLICATION = "replication";
  public static final String FIELD_SIZE = "size";
  public static final String FIELD_CONFIG_PREFIX = "config";

  private final Directory directory;
  private final DirectoryReader indexReader;
  private final IndexSearcher indexSearcher;
  private final Analyzer analyzer;
  private final int maxSize;
  private final ReadWriteLock closeLock = new ReentrantReadWriteLock();

  public TopicsIndex(List<InternalTopic> topics) throws IOException {
    this(topics, 3, 5);
  }

  public TopicsIndex(List<InternalTopic> topics, int minNgram, int maxNgram) throws IOException {
    this.analyzer = new ShortWordNGramAnalyzer(minNgram, maxNgram);
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
        doc.add(new TextField(FIELD_NAME, topic.getName(), Field.Store.NO));
        doc.add(new IntPoint(FIELD_PARTITIONS, topic.getPartitionCount()));
        doc.add(new IntPoint(FIELD_REPLICATION, topic.getReplicationFactor()));
        doc.add(new LongPoint(FIELD_SIZE, topic.getSegmentSize()));
        if (topic.getTopicConfigs() != null && !topic.getTopicConfigs().isEmpty()) {
          for (InternalTopicConfig topicConfig : topic.getTopicConfigs()) {
            doc.add(new StringField(FIELD_CONFIG_PREFIX + "_" + topicConfig.getName(), topicConfig.getValue(),
                Field.Store.NO));
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

  public List<String> find(String search, Boolean showInternal, Integer count) throws IOException {
    return find(search, showInternal, FIELD_NAME, count, 0.0f);
  }

  public List<String> find(String search, Boolean showInternal, String sort, Integer count) throws IOException {
    return find(search, showInternal, sort, count, 0.0f);
  }

  public List<String> find(String search, Boolean showInternal,
                           String sortField, Integer count, float minScore) throws IOException {
    closeLock.readLock().lock();
    try {
      QueryParser queryParser = new QueryParser(FIELD_NAME, this.analyzer);
      queryParser.setDefaultOperator(QueryParser.Operator.AND);
      Query nameQuery;
      try {
        nameQuery = queryParser.parse(search);
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }

      Query internalFilter = new TermQuery(new Term(FIELD_INTERNAL, "true"));

      BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
      queryBuilder.add(nameQuery, BooleanClause.Occur.MUST);
      if (showInternal == null || !showInternal) {
        queryBuilder.add(internalFilter, BooleanClause.Occur.MUST_NOT);
      }

      List<SortField> sortFields = new ArrayList<>();
      sortFields.add(SortField.FIELD_SCORE);
      if (!sortField.equals(FIELD_NAME)) {
        sortFields.add(new SortField(sortField, SortField.Type.INT, true));
      }

      Sort sort = new Sort(sortFields.toArray(new SortField[0]));

      TopDocs result = this.indexSearcher.search(queryBuilder.build(), count != null ? count : this.maxSize, sort);

      List<String> topics = new ArrayList<>();
      for (ScoreDoc scoreDoc : result.scoreDocs) {
        if (minScore > 0.00001f && scoreDoc.score < minScore) {
          continue;
        }
        Document document = this.indexSearcher.storedFields().document(scoreDoc.doc);
        topics.add(document.get(FIELD_NAME_RAW));
      }
      return topics;
    } finally {
      this.closeLock.readLock().unlock();
    }
  }
}
