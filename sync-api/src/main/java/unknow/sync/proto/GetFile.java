// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: sync.proto

package unknow.sync.proto;

/**
 * Protobuf type {@code unknow.sync.proto.GetFile}
 */
public  final class GetFile extends
    com.google.protobuf.GeneratedMessageLite<
        GetFile, GetFile.Builder> implements
    // @@protoc_insertion_point(message_implements:unknow.sync.proto.GetFile)
    GetFileOrBuilder {
  private GetFile() {
    file_ = "";
  }
  public static final int FILE_FIELD_NUMBER = 1;
  private java.lang.String file_;
  /**
   * <code>string file = 1;</code>
   * @return The file.
   */
  @java.lang.Override
  public java.lang.String getFile() {
    return file_;
  }
  /**
   * <code>string file = 1;</code>
   * @return The bytes for file.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getFileBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(file_);
  }
  /**
   * <code>string file = 1;</code>
   * @param value The file to set.
   */
  private void setFile(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    file_ = value;
  }
  /**
   * <code>string file = 1;</code>
   */
  private void clearFile() {
    
    file_ = getDefaultInstance().getFile();
  }
  /**
   * <code>string file = 1;</code>
   * @param value The bytes for file to set.
   */
  private void setFileBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    file_ = value.toStringUtf8();
    
  }

  public static final int OFFSET_FIELD_NUMBER = 2;
  private long offset_;
  /**
   * <code>uint64 offset = 2;</code>
   * @return The offset.
   */
  @java.lang.Override
  public long getOffset() {
    return offset_;
  }
  /**
   * <code>uint64 offset = 2;</code>
   * @param value The offset to set.
   */
  private void setOffset(long value) {
    
    offset_ = value;
  }
  /**
   * <code>uint64 offset = 2;</code>
   */
  private void clearOffset() {
    
    offset_ = 0L;
  }

  public static unknow.sync.proto.GetFile parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static unknow.sync.proto.GetFile parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static unknow.sync.proto.GetFile parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static unknow.sync.proto.GetFile parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static unknow.sync.proto.GetFile parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static unknow.sync.proto.GetFile parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static unknow.sync.proto.GetFile parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static unknow.sync.proto.GetFile parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static unknow.sync.proto.GetFile parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }
  public static unknow.sync.proto.GetFile parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static unknow.sync.proto.GetFile parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static unknow.sync.proto.GetFile parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(unknow.sync.proto.GetFile prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code unknow.sync.proto.GetFile}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        unknow.sync.proto.GetFile, Builder> implements
      // @@protoc_insertion_point(builder_implements:unknow.sync.proto.GetFile)
      unknow.sync.proto.GetFileOrBuilder {
    // Construct using unknow.sync.proto.GetFile.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>string file = 1;</code>
     * @return The file.
     */
    @java.lang.Override
    public java.lang.String getFile() {
      return instance.getFile();
    }
    /**
     * <code>string file = 1;</code>
     * @return The bytes for file.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getFileBytes() {
      return instance.getFileBytes();
    }
    /**
     * <code>string file = 1;</code>
     * @param value The file to set.
     * @return This builder for chaining.
     */
    public Builder setFile(
        java.lang.String value) {
      copyOnWrite();
      instance.setFile(value);
      return this;
    }
    /**
     * <code>string file = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearFile() {
      copyOnWrite();
      instance.clearFile();
      return this;
    }
    /**
     * <code>string file = 1;</code>
     * @param value The bytes for file to set.
     * @return This builder for chaining.
     */
    public Builder setFileBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setFileBytes(value);
      return this;
    }

    /**
     * <code>uint64 offset = 2;</code>
     * @return The offset.
     */
    @java.lang.Override
    public long getOffset() {
      return instance.getOffset();
    }
    /**
     * <code>uint64 offset = 2;</code>
     * @param value The offset to set.
     * @return This builder for chaining.
     */
    public Builder setOffset(long value) {
      copyOnWrite();
      instance.setOffset(value);
      return this;
    }
    /**
     * <code>uint64 offset = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearOffset() {
      copyOnWrite();
      instance.clearOffset();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:unknow.sync.proto.GetFile)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new unknow.sync.proto.GetFile();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "file_",
            "offset_",
          };
          java.lang.String info =
              "\u0000\u0002\u0000\u0000\u0001\u0002\u0002\u0000\u0000\u0000\u0001\u0208\u0002\u0003" +
              "";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<unknow.sync.proto.GetFile> parser = PARSER;
        if (parser == null) {
          synchronized (unknow.sync.proto.GetFile.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<unknow.sync.proto.GetFile>(
                      DEFAULT_INSTANCE);
              PARSER = parser;
            }
          }
        }
        return parser;
    }
    case GET_MEMOIZED_IS_INITIALIZED: {
      return (byte) 1;
    }
    case SET_MEMOIZED_IS_INITIALIZED: {
      return null;
    }
    }
    throw new UnsupportedOperationException();
  }


  // @@protoc_insertion_point(class_scope:unknow.sync.proto.GetFile)
  private static final unknow.sync.proto.GetFile DEFAULT_INSTANCE;
  static {
    GetFile defaultInstance = new GetFile();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      GetFile.class, defaultInstance);
  }

  public static unknow.sync.proto.GetFile getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<GetFile> PARSER;

  public static com.google.protobuf.Parser<GetFile> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

