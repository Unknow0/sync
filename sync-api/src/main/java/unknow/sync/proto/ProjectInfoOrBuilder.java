// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: sync.proto

package unknow.sync.proto;

public interface ProjectInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:unknow.sync.proto.ProjectInfo)
    com.google.protobuf.MessageLiteOrBuilder {

  /**
   * <code>uint32 blocSize = 1;</code>
   * @return The blocSize.
   */
  int getBlocSize();

  /**
   * <code>repeated .unknow.sync.proto.FileInfo file = 2;</code>
   */
  java.util.List<unknow.sync.proto.FileInfo> 
      getFileList();
  /**
   * <code>repeated .unknow.sync.proto.FileInfo file = 2;</code>
   */
  unknow.sync.proto.FileInfo getFile(int index);
  /**
   * <code>repeated .unknow.sync.proto.FileInfo file = 2;</code>
   */
  int getFileCount();
}
