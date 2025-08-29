package io.kafbat.ui.service.index;

import static org.apache.lucene.search.BoostAttribute.DEFAULT_BOOST;

import java.util.Optional;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class PrefixQueryParser extends QueryParser {
  
  public PrefixQueryParser(String field, Analyzer analyzer) {
    super(field, analyzer);
  }

  @Override
  protected Query newTermQuery(Term term, float boost) {

    TopicsIndex.FieldType fieldType = Optional.ofNullable(term.field())
        .map(TopicsIndex.FIELD_TYPES::get)
        .orElse(TopicsIndex.FieldType.STRING);

    Query query =  switch (fieldType) {
      case STRING -> new PrefixQuery(term);
      case INT -> IntPoint.newExactQuery(term.field(), Integer.parseInt(term.text()));
      case LONG -> LongPoint.newExactQuery(term.field(), Long.parseLong(term.text()));
      case BOOLEAN -> new TermQuery(term);
    };

    if (boost == DEFAULT_BOOST) {
      return query;
    }
    return new BoostQuery(query, boost);
  }

}
