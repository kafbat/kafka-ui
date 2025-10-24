package io.kafbat.ui.service.index.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

public class NameDistanceScoringFunction extends DoubleValuesSource {
  private final String fieldName;
  private final List<String> prefixes;

  public NameDistanceScoringFunction(String fieldName, List<String> prefixes) {
    this.fieldName = fieldName;
    this.prefixes = prefixes;
  }

  @Override
  public DoubleValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {

    Terms terms = ctx.reader().terms(fieldName);
    Map<Integer, Integer> positions = new HashMap<>();

    for (String prefix : prefixes) {
      TermsEnum iterator = terms.iterator();
      TermsEnum.SeekStatus seekStatus = iterator.seekCeil(new BytesRef(prefix));
      if (!seekStatus.equals(TermsEnum.SeekStatus.END)) {

        PostingsEnum postings = iterator.postings(
            null,
            PostingsEnum.OFFSETS | PostingsEnum.FREQS | PostingsEnum.POSITIONS
        );

        while (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
          int doc = postings.docID();
          int smallest = Integer.MAX_VALUE;

          for (int i = 0; i < postings.freq(); i++) {
            postings.nextPosition();
            smallest = Math.min(smallest, postings.startOffset());
          }
          int finalSmall = smallest;
          int s = positions.computeIfAbsent(doc, d -> finalSmall);
          if (finalSmall < s) {
            positions.put(doc, finalSmall);
          }
        }
      }
    }

    return new DoubleValues() {
      int doc = -1;

      @Override
      public double doubleValue() {
        Integer pos = positions.get(doc);
        if (pos == null) {
          return 1.0;
        }
        return 1.0 / (1.0 + pos);
      }

      @Override
      public boolean advanceExact(int target) {
        doc = target;
        return true;
      }
    };
  }

  @Override
  public boolean needsScores() {
    return false;
  }

  @Override
  public DoubleValuesSource rewrite(IndexSearcher searcher) {
    return this;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(fieldName, prefixes);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    NameDistanceScoringFunction that = (NameDistanceScoringFunction) obj;
    return java.util.Objects.equals(fieldName, that.fieldName)
        && java.util.Objects.equals(prefixes, that.prefixes);
  }

  @Override
  public String toString() {
    return "NameDistanceScoringFunction";
  }

  @Override
  public boolean isCacheable(LeafReaderContext ctx) {
    return false;
  }
}
