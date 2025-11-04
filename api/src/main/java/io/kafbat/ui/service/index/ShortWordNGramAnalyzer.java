package io.kafbat.ui.service.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class ShortWordNGramAnalyzer extends Analyzer {
  private final int minGram;
  private final int maxGram;
  private final boolean preserveOriginal;

  public ShortWordNGramAnalyzer(int minGram, int maxGram) {
    this(minGram, maxGram, true);
  }

  public ShortWordNGramAnalyzer(int minGram, int maxGram, boolean preserveOriginal) {
    this.minGram = minGram;
    this.maxGram = maxGram;
    this.preserveOriginal = preserveOriginal;
  }


  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    Tokenizer tokenizer = new StandardTokenizer();

    TokenStream tokenStream = new WordDelimiterGraphFilter(
        tokenizer,
        WordDelimiterGraphFilter.GENERATE_WORD_PARTS
            | WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE
            | WordDelimiterGraphFilter.STEM_ENGLISH_POSSESSIVE,
        null
    );

    tokenStream = new LowerCaseFilter(tokenStream);

    // Add n-gram generation from characters (min=2, max=4)
    tokenStream = new NGramTokenFilter(tokenStream, minGram, maxGram, this.preserveOriginal);

    return new TokenStreamComponents(tokenizer, tokenStream);
  }
}
