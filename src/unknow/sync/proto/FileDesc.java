/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package unknow.sync.proto;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class FileDesc extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"FileDesc\",\"namespace\":\"unknow.sync.proto\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"blocCount\",\"type\":\"int\"},{\"name\":\"roll\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"hash\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"fixed\",\"name\":\"Hash\",\"size\":64}}},{\"name\":\"fileHash\",\"type\":\"Hash\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public java.lang.CharSequence name;
  @Deprecated public int blocCount;
  @Deprecated public java.util.List<java.lang.Integer> roll;
  @Deprecated public java.util.List<unknow.sync.proto.Hash> hash;
  @Deprecated public unknow.sync.proto.Hash fileHash;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public FileDesc() {}

  /**
   * All-args constructor.
   */
  public FileDesc(java.lang.CharSequence name, java.lang.Integer blocCount, java.util.List<java.lang.Integer> roll, java.util.List<unknow.sync.proto.Hash> hash, unknow.sync.proto.Hash fileHash) {
    this.name = name;
    this.blocCount = blocCount;
    this.roll = roll;
    this.hash = hash;
    this.fileHash = fileHash;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return name;
    case 1: return blocCount;
    case 2: return roll;
    case 3: return hash;
    case 4: return fileHash;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: name = (java.lang.CharSequence)value$; break;
    case 1: blocCount = (java.lang.Integer)value$; break;
    case 2: roll = (java.util.List<java.lang.Integer>)value$; break;
    case 3: hash = (java.util.List<unknow.sync.proto.Hash>)value$; break;
    case 4: fileHash = (unknow.sync.proto.Hash)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'name' field.
   */
  public java.lang.CharSequence getName() {
    return name;
  }

  /**
   * Sets the value of the 'name' field.
   * @param value the value to set.
   */
  public void setName(java.lang.CharSequence value) {
    this.name = value;
  }

  /**
   * Gets the value of the 'blocCount' field.
   */
  public java.lang.Integer getBlocCount() {
    return blocCount;
  }

  /**
   * Sets the value of the 'blocCount' field.
   * @param value the value to set.
   */
  public void setBlocCount(java.lang.Integer value) {
    this.blocCount = value;
  }

  /**
   * Gets the value of the 'roll' field.
   */
  public java.util.List<java.lang.Integer> getRoll() {
    return roll;
  }

  /**
   * Sets the value of the 'roll' field.
   * @param value the value to set.
   */
  public void setRoll(java.util.List<java.lang.Integer> value) {
    this.roll = value;
  }

  /**
   * Gets the value of the 'hash' field.
   */
  public java.util.List<unknow.sync.proto.Hash> getHash() {
    return hash;
  }

  /**
   * Sets the value of the 'hash' field.
   * @param value the value to set.
   */
  public void setHash(java.util.List<unknow.sync.proto.Hash> value) {
    this.hash = value;
  }

  /**
   * Gets the value of the 'fileHash' field.
   */
  public unknow.sync.proto.Hash getFileHash() {
    return fileHash;
  }

  /**
   * Sets the value of the 'fileHash' field.
   * @param value the value to set.
   */
  public void setFileHash(unknow.sync.proto.Hash value) {
    this.fileHash = value;
  }

  /** Creates a new FileDesc RecordBuilder */
  public static unknow.sync.proto.FileDesc.Builder newBuilder() {
    return new unknow.sync.proto.FileDesc.Builder();
  }
  
  /** Creates a new FileDesc RecordBuilder by copying an existing Builder */
  public static unknow.sync.proto.FileDesc.Builder newBuilder(unknow.sync.proto.FileDesc.Builder other) {
    return new unknow.sync.proto.FileDesc.Builder(other);
  }
  
  /** Creates a new FileDesc RecordBuilder by copying an existing FileDesc instance */
  public static unknow.sync.proto.FileDesc.Builder newBuilder(unknow.sync.proto.FileDesc other) {
    return new unknow.sync.proto.FileDesc.Builder(other);
  }
  
  /**
   * RecordBuilder for FileDesc instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<FileDesc>
    implements org.apache.avro.data.RecordBuilder<FileDesc> {

    private java.lang.CharSequence name;
    private int blocCount;
    private java.util.List<java.lang.Integer> roll;
    private java.util.List<unknow.sync.proto.Hash> hash;
    private unknow.sync.proto.Hash fileHash;

    /** Creates a new Builder */
    private Builder() {
      super(unknow.sync.proto.FileDesc.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(unknow.sync.proto.FileDesc.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.name)) {
        this.name = data().deepCopy(fields()[0].schema(), other.name);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.blocCount)) {
        this.blocCount = data().deepCopy(fields()[1].schema(), other.blocCount);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.roll)) {
        this.roll = data().deepCopy(fields()[2].schema(), other.roll);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.hash)) {
        this.hash = data().deepCopy(fields()[3].schema(), other.hash);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.fileHash)) {
        this.fileHash = data().deepCopy(fields()[4].schema(), other.fileHash);
        fieldSetFlags()[4] = true;
      }
    }
    
    /** Creates a Builder by copying an existing FileDesc instance */
    private Builder(unknow.sync.proto.FileDesc other) {
            super(unknow.sync.proto.FileDesc.SCHEMA$);
      if (isValidValue(fields()[0], other.name)) {
        this.name = data().deepCopy(fields()[0].schema(), other.name);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.blocCount)) {
        this.blocCount = data().deepCopy(fields()[1].schema(), other.blocCount);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.roll)) {
        this.roll = data().deepCopy(fields()[2].schema(), other.roll);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.hash)) {
        this.hash = data().deepCopy(fields()[3].schema(), other.hash);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.fileHash)) {
        this.fileHash = data().deepCopy(fields()[4].schema(), other.fileHash);
        fieldSetFlags()[4] = true;
      }
    }

    /** Gets the value of the 'name' field */
    public java.lang.CharSequence getName() {
      return name;
    }
    
    /** Sets the value of the 'name' field */
    public unknow.sync.proto.FileDesc.Builder setName(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.name = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'name' field has been set */
    public boolean hasName() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'name' field */
    public unknow.sync.proto.FileDesc.Builder clearName() {
      name = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'blocCount' field */
    public java.lang.Integer getBlocCount() {
      return blocCount;
    }
    
    /** Sets the value of the 'blocCount' field */
    public unknow.sync.proto.FileDesc.Builder setBlocCount(int value) {
      validate(fields()[1], value);
      this.blocCount = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'blocCount' field has been set */
    public boolean hasBlocCount() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'blocCount' field */
    public unknow.sync.proto.FileDesc.Builder clearBlocCount() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'roll' field */
    public java.util.List<java.lang.Integer> getRoll() {
      return roll;
    }
    
    /** Sets the value of the 'roll' field */
    public unknow.sync.proto.FileDesc.Builder setRoll(java.util.List<java.lang.Integer> value) {
      validate(fields()[2], value);
      this.roll = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'roll' field has been set */
    public boolean hasRoll() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'roll' field */
    public unknow.sync.proto.FileDesc.Builder clearRoll() {
      roll = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'hash' field */
    public java.util.List<unknow.sync.proto.Hash> getHash() {
      return hash;
    }
    
    /** Sets the value of the 'hash' field */
    public unknow.sync.proto.FileDesc.Builder setHash(java.util.List<unknow.sync.proto.Hash> value) {
      validate(fields()[3], value);
      this.hash = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'hash' field has been set */
    public boolean hasHash() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'hash' field */
    public unknow.sync.proto.FileDesc.Builder clearHash() {
      hash = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'fileHash' field */
    public unknow.sync.proto.Hash getFileHash() {
      return fileHash;
    }
    
    /** Sets the value of the 'fileHash' field */
    public unknow.sync.proto.FileDesc.Builder setFileHash(unknow.sync.proto.Hash value) {
      validate(fields()[4], value);
      this.fileHash = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'fileHash' field has been set */
    public boolean hasFileHash() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'fileHash' field */
    public unknow.sync.proto.FileDesc.Builder clearFileHash() {
      fileHash = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    @Override
    public FileDesc build() {
      try {
        FileDesc record = new FileDesc();
        record.name = fieldSetFlags()[0] ? this.name : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.blocCount = fieldSetFlags()[1] ? this.blocCount : (java.lang.Integer) defaultValue(fields()[1]);
        record.roll = fieldSetFlags()[2] ? this.roll : (java.util.List<java.lang.Integer>) defaultValue(fields()[2]);
        record.hash = fieldSetFlags()[3] ? this.hash : (java.util.List<unknow.sync.proto.Hash>) defaultValue(fields()[3]);
        record.fileHash = fieldSetFlags()[4] ? this.fileHash : (unknow.sync.proto.Hash) defaultValue(fields()[4]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
