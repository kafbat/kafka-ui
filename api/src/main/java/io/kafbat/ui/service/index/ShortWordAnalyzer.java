package io.kafbat.ui.service.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

class ShortWordAnalyzer extends Analyzer {

  public ShortWordAnalyzer() {}

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    Tokenizer tokenizer = new StandardTokenizer();

    TokenStream tokenStream = new WordDelimiterGraphFilter(
        tokenizer,
        WordDelimiterGraphFilter.GENERATE_WORD_PARTS
            | WordDelimiterGraphFilter.SPLIT_ON_CASE_CHANGE
            | WordDelimiterGraphFilter.PRESERVE_ORIGINAL
            | WordDelimiterGraphFilter.GENERATE_NUMBER_PARTS
            | WordDelimiterGraphFilter.STEM_ENGLISH_POSSESSIVE,

        null
    );

    tokenStream = new LowerCaseFilter(tokenStream);

    return new TokenStreamComponents(tokenizer, tokenStream);
  }
}
