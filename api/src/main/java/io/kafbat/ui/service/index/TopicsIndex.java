package io.kafbat.ui.service.index;

import io.kafbat.ui.model.InternalTopic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

public class TopicsIndex implements AutoCloseable {
  public static final String FIELD_NAME = "name";
  public static final String FIELD_INTERNAL = "internal";

  private final Directory directory;
  private final DirectoryReader indexReader;
  private final IndexSearcher indexSearcher;
  private final Analyzer analyzer;

  public TopicsIndex(List<InternalTopic> topics) throws IOException {
    this.directory = build(topics);
    this.indexReader = DirectoryReader.open(directory);
    this.indexSearcher = new IndexSearcher(indexReader);
    this.analyzer = new StandardAnalyzer();
  }

  private Directory build(List<InternalTopic> topics) {
    Directory directory = new ByteBuffersDirectory();
    try(IndexWriter directoryWriter = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()))) {
      for (InternalTopic topic : topics) {
        Document doc = new Document();
        doc.add(new TextField(FIELD_NAME, topic.getName(), Field.Store.YES));
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
    if (indexReader != null) {
      this.indexReader.close();
    }
    if (this.directory != null) {
      this.directory.close();
    }
  }

  public List<String> find(String search, Boolean showInternal, int count) throws IOException {
    return find(search, showInternal, count, 0.0f, 1);
  }
  public List<String> find(String search, Boolean showInternal, int count, float minScore, int maxEdits) throws IOException {
    Query nameQuery = new FuzzyQuery(new Term(FIELD_NAME, search), maxEdits);
    Query internalFilter = new TermQuery(new Term(FIELD_INTERNAL, "true"));
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(nameQuery, BooleanClause.Occur.MUST);
    if (showInternal == null || !showInternal) {
      builder.add(internalFilter, BooleanClause.Occur.MUST_NOT);
    }
    TopDocs result = this.indexSearcher.search(builder.build(), count);
    List<String> topics = new ArrayList<>();
    for (ScoreDoc scoreDoc : result.scoreDocs) {
      if (scoreDoc.score > minScore) {
        Document document = this.indexSearcher.storedFields().document(scoreDoc.doc);
        topics.add(document.get(FIELD_NAME));
      }
    }
    return topics;
  }
}
