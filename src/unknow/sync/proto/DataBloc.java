/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package unknow.sync.proto;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class DataBloc extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"DataBloc\",\"namespace\":\"unknow.sync.proto\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"data\",\"type\":\"bytes\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public int id;
  @Deprecated public java.nio.ByteBuffer data;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public DataBloc() {}

  /**
   * All-args constructor.
   */
  public DataBloc(java.lang.Integer id, java.nio.ByteBuffer data) {
    this.id = id;
    this.data = data;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return id;
    case 1: return data;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: id = (java.lang.Integer)value$; break;
    case 1: data = (java.nio.ByteBuffer)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'id' field.
   */
  public java.lang.Integer getId() {
    return id;
  }

  /**
   * Sets the value of the 'id' field.
   * @param value the value to set.
   */
  public void setId(java.lang.Integer value) {
    this.id = value;
  }

  /**
   * Gets the value of the 'data' field.
   */
  public java.nio.ByteBuffer getData() {
    return data;
  }

  /**
   * Sets the value of the 'data' field.
   * @param value the value to set.
   */
  public void setData(java.nio.ByteBuffer value) {
    this.data = value;
  }

  /** Creates a new DataBloc RecordBuilder */
  public static unknow.sync.proto.DataBloc.Builder newBuilder() {
    return new unknow.sync.proto.DataBloc.Builder();
  }
  
  /** Creates a new DataBloc RecordBuilder by copying an existing Builder */
  public static unknow.sync.proto.DataBloc.Builder newBuilder(unknow.sync.proto.DataBloc.Builder other) {
    return new unknow.sync.proto.DataBloc.Builder(other);
  }
  
  /** Creates a new DataBloc RecordBuilder by copying an existing DataBloc instance */
  public static unknow.sync.proto.DataBloc.Builder newBuilder(unknow.sync.proto.DataBloc other) {
    return new unknow.sync.proto.DataBloc.Builder(other);
  }
  
  /**
   * RecordBuilder for DataBloc instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<DataBloc>
    implements org.apache.avro.data.RecordBuilder<DataBloc> {

    private int id;
    private java.nio.ByteBuffer data;

    /** Creates a new Builder */
    private Builder() {
      super(unknow.sync.proto.DataBloc.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(unknow.sync.proto.DataBloc.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.id)) {
        this.id = data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.data)) {
        this.data = data().deepCopy(fields()[1].schema(), other.data);
        fieldSetFlags()[1] = true;
      }
    }
    
    /** Creates a Builder by copying an existing DataBloc instance */
    private Builder(unknow.sync.proto.DataBloc other) {
            super(unknow.sync.proto.DataBloc.SCHEMA$);
      if (isValidValue(fields()[0], other.id)) {
        this.id = data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.data)) {
        this.data = data().deepCopy(fields()[1].schema(), other.data);
        fieldSetFlags()[1] = true;
      }
    }

    /** Gets the value of the 'id' field */
    public java.lang.Integer getId() {
      return id;
    }
    
    /** Sets the value of the 'id' field */
    public unknow.sync.proto.DataBloc.Builder setId(int value) {
      validate(fields()[0], value);
      this.id = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'id' field has been set */
    public boolean hasId() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'id' field */
    public unknow.sync.proto.DataBloc.Builder clearId() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'data' field */
    public java.nio.ByteBuffer getData() {
      return data;
    }
    
    /** Sets the value of the 'data' field */
    public unknow.sync.proto.DataBloc.Builder setData(java.nio.ByteBuffer value) {
      validate(fields()[1], value);
      this.data = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'data' field has been set */
    public boolean hasData() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'data' field */
    public unknow.sync.proto.DataBloc.Builder clearData() {
      data = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    public DataBloc build() {
      try {
        DataBloc record = new DataBloc();
        record.id = fieldSetFlags()[0] ? this.id : (java.lang.Integer) defaultValue(fields()[0]);
        record.data = fieldSetFlags()[1] ? this.data : (java.nio.ByteBuffer) defaultValue(fields()[1]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
