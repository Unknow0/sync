/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package unknow.sync.proto;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class ProjectInfo extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ProjectInfo\",\"namespace\":\"unknow.sync.proto\",\"fields\":[{\"name\":\"blocSize\",\"type\":\"int\"},{\"name\":\"fileDescs\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"FileDesc\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"blocCount\",\"type\":\"int\"},{\"name\":\"roll\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"hash\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"fixed\",\"name\":\"Hash\",\"size\":64}}},{\"name\":\"fileHash\",\"type\":\"Hash\"}]}}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public int blocSize;
  @Deprecated public java.util.List<unknow.sync.proto.FileDesc> fileDescs;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public ProjectInfo() {}

  /**
   * All-args constructor.
   */
  public ProjectInfo(java.lang.Integer blocSize, java.util.List<unknow.sync.proto.FileDesc> fileDescs) {
    this.blocSize = blocSize;
    this.fileDescs = fileDescs;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return blocSize;
    case 1: return fileDescs;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: blocSize = (java.lang.Integer)value$; break;
    case 1: fileDescs = (java.util.List<unknow.sync.proto.FileDesc>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'blocSize' field.
   */
  public java.lang.Integer getBlocSize() {
    return blocSize;
  }

  /**
   * Sets the value of the 'blocSize' field.
   * @param value the value to set.
   */
  public void setBlocSize(java.lang.Integer value) {
    this.blocSize = value;
  }

  /**
   * Gets the value of the 'fileDescs' field.
   */
  public java.util.List<unknow.sync.proto.FileDesc> getFileDescs() {
    return fileDescs;
  }

  /**
   * Sets the value of the 'fileDescs' field.
   * @param value the value to set.
   */
  public void setFileDescs(java.util.List<unknow.sync.proto.FileDesc> value) {
    this.fileDescs = value;
  }

  /** Creates a new ProjectInfo RecordBuilder */
  public static unknow.sync.proto.ProjectInfo.Builder newBuilder() {
    return new unknow.sync.proto.ProjectInfo.Builder();
  }
  
  /** Creates a new ProjectInfo RecordBuilder by copying an existing Builder */
  public static unknow.sync.proto.ProjectInfo.Builder newBuilder(unknow.sync.proto.ProjectInfo.Builder other) {
    return new unknow.sync.proto.ProjectInfo.Builder(other);
  }
  
  /** Creates a new ProjectInfo RecordBuilder by copying an existing ProjectInfo instance */
  public static unknow.sync.proto.ProjectInfo.Builder newBuilder(unknow.sync.proto.ProjectInfo other) {
    return new unknow.sync.proto.ProjectInfo.Builder(other);
  }
  
  /**
   * RecordBuilder for ProjectInfo instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ProjectInfo>
    implements org.apache.avro.data.RecordBuilder<ProjectInfo> {

    private int blocSize;
    private java.util.List<unknow.sync.proto.FileDesc> fileDescs;

    /** Creates a new Builder */
    private Builder() {
      super(unknow.sync.proto.ProjectInfo.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(unknow.sync.proto.ProjectInfo.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.blocSize)) {
        this.blocSize = data().deepCopy(fields()[0].schema(), other.blocSize);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.fileDescs)) {
        this.fileDescs = data().deepCopy(fields()[1].schema(), other.fileDescs);
        fieldSetFlags()[1] = true;
      }
    }
    
    /** Creates a Builder by copying an existing ProjectInfo instance */
    private Builder(unknow.sync.proto.ProjectInfo other) {
            super(unknow.sync.proto.ProjectInfo.SCHEMA$);
      if (isValidValue(fields()[0], other.blocSize)) {
        this.blocSize = data().deepCopy(fields()[0].schema(), other.blocSize);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.fileDescs)) {
        this.fileDescs = data().deepCopy(fields()[1].schema(), other.fileDescs);
        fieldSetFlags()[1] = true;
      }
    }

    /** Gets the value of the 'blocSize' field */
    public java.lang.Integer getBlocSize() {
      return blocSize;
    }
    
    /** Sets the value of the 'blocSize' field */
    public unknow.sync.proto.ProjectInfo.Builder setBlocSize(int value) {
      validate(fields()[0], value);
      this.blocSize = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'blocSize' field has been set */
    public boolean hasBlocSize() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'blocSize' field */
    public unknow.sync.proto.ProjectInfo.Builder clearBlocSize() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'fileDescs' field */
    public java.util.List<unknow.sync.proto.FileDesc> getFileDescs() {
      return fileDescs;
    }
    
    /** Sets the value of the 'fileDescs' field */
    public unknow.sync.proto.ProjectInfo.Builder setFileDescs(java.util.List<unknow.sync.proto.FileDesc> value) {
      validate(fields()[1], value);
      this.fileDescs = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'fileDescs' field has been set */
    public boolean hasFileDescs() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'fileDescs' field */
    public unknow.sync.proto.ProjectInfo.Builder clearFileDescs() {
      fileDescs = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    public ProjectInfo build() {
      try {
        ProjectInfo record = new ProjectInfo();
        record.blocSize = fieldSetFlags()[0] ? this.blocSize : (java.lang.Integer) defaultValue(fields()[0]);
        record.fileDescs = fieldSetFlags()[1] ? this.fileDescs : (java.util.List<unknow.sync.proto.FileDesc>) defaultValue(fields()[1]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}