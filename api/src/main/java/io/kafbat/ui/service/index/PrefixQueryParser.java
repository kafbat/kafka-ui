package io.kafbat.ui.service.index;

import static org.apache.lucene.search.BoostAttribute.DEFAULT_BOOST;

import io.kafbat.ui.service.index.TopicsIndex.FieldType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  private final List<String> prefixes = new ArrayList<>();

  public PrefixQueryParser(String field, Analyzer analyzer) {
    super(field, analyzer);
  }

  @Override
  protected Query newRangeQuery(String field, String part1, String part2, boolean startInclusive,
                                boolean endInclusive) {
    FieldType fieldType = Optional.ofNullable(field)
        .map(TopicsIndex.FIELD_TYPES::get)
        .orElse(FieldType.STRING);

    return switch (fieldType) {
      case STRING, BOOLEAN -> super.newRangeQuery(field, part1, part2, startInclusive, endInclusive);
      case INT -> IntPoint.newRangeQuery(field, parseInt(part1, true), parseInt(part2, false));
      case LONG -> LongPoint.newRangeQuery(field, parseLong(part1, true), parseLong(part2, false));
    };
  }

  private Integer parseInt(String value, boolean min) {
    if ("*".equals(value) || value == null) {
      return min ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    } else {
      return Integer.parseInt(value);
    }
  }

  private Long parseLong(String value, boolean min) {
    if ("*".equals(value) || value == null) {
      return min ? Long.MIN_VALUE : Long.MAX_VALUE;
    } else {
      return Long.parseLong(value);
    }
  }

  @Override
  protected Query newTermQuery(Term term, float boost) {

    FieldType fieldType = Optional.ofNullable(term.field())
        .map(TopicsIndex.FIELD_TYPES::get)
        .orElse(FieldType.STRING);

    Query query =  switch (fieldType) {
      case STRING -> {
        if (Objects.equals(term.field(), field)) {
          prefixes.add(term.text());
        }

        yield new PrefixQuery(term);
      }
      case INT -> IntPoint.newExactQuery(term.field(), Integer.parseInt(term.text()));
      case LONG -> LongPoint.newExactQuery(term.field(), Long.parseLong(term.text()));
      case BOOLEAN -> new TermQuery(term);
    };

    if (boost == DEFAULT_BOOST) {
      return query;
    }
    return new BoostQuery(query, boost);
  }

  public List<String> getPrefixes() {
    return prefixes;
  }
}
