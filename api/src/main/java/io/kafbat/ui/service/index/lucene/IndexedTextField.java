package io.kafbat.ui.service.index.lucene;


import java.io.Reader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredValue;
import org.apache.lucene.index.IndexOptions;

public class IndexedTextField extends Field {

  /** Indexed, tokenized, not stored. */
  public static final FieldType TYPE_NOT_STORED = new FieldType();

  /** Indexed, tokenized, stored. */
  public static final FieldType TYPE_STORED = new FieldType();

  static {
    TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    TYPE_NOT_STORED.setTokenized(true);
    TYPE_NOT_STORED.freeze();

    TYPE_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    TYPE_STORED.setTokenized(true);
    TYPE_STORED.setStored(true);
    TYPE_STORED.setStoreTermVectors(true);
    TYPE_STORED.setStoreTermVectorOffsets(true);
    TYPE_STORED.setStoreTermVectorPositions(true);
    TYPE_STORED.freeze();
  }

  private final StoredValue storedValue;

  /**
   * Creates a new un-stored TextField with Reader value.
   *
   * @param name field name
   * @param reader reader value
   * @throws IllegalArgumentException if the field name is null
   * @throws NullPointerException if the reader is null
   */
  public IndexedTextField(String name, Reader reader) {
    super(name, reader, TYPE_NOT_STORED);
    storedValue = null;
  }

  /**
   * Creates a new TextField with String value.
   *
   * @param name field name
   * @param value string value
   * @param store Store.YES if the content should also be stored
   * @throws IllegalArgumentException if the field name or value is null.
   */
  public IndexedTextField(String name, String value, Store store) {
    super(name, value, store == Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
    if (store == Store.YES) {
      storedValue = new StoredValue(value);
    } else {
      storedValue = null;
    }
  }

  /**
   * Creates a new un-stored TextField with TokenStream value.
   *
   * @param name field name
   * @param stream TokenStream value
   * @throws IllegalArgumentException if the field name is null.
   * @throws NullPointerException if the tokenStream is null
   */
  public IndexedTextField(String name, TokenStream stream) {
    super(name, stream, TYPE_NOT_STORED);
    storedValue = null;
  }

  @Override
  public void setStringValue(String value) {
    super.setStringValue(value);
    if (storedValue != null) {
      storedValue.setStringValue(value);
    }
  }

  @Override
  public StoredValue storedValue() {
    return storedValue;
  }
}
