// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: grpc/table/table_master.proto

package alluxio.grpc.table;

public interface TableInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:alluxio.grpc.table.TableInfo)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional string db_name = 1;</code>
   */
  boolean hasDbName();
  /**
   * <code>optional string db_name = 1;</code>
   */
  java.lang.String getDbName();
  /**
   * <code>optional string db_name = 1;</code>
   */
  com.google.protobuf.ByteString
      getDbNameBytes();

  /**
   * <code>optional string table_name = 2;</code>
   */
  boolean hasTableName();
  /**
   * <code>optional string table_name = 2;</code>
   */
  java.lang.String getTableName();
  /**
   * <code>optional string table_name = 2;</code>
   */
  com.google.protobuf.ByteString
      getTableNameBytes();

  /**
   * <code>optional .alluxio.grpc.table.TableInfo.TableType type = 3;</code>
   */
  boolean hasType();
  /**
   * <code>optional .alluxio.grpc.table.TableInfo.TableType type = 3;</code>
   */
  alluxio.grpc.table.TableInfo.TableType getType();

  /**
   * <code>optional string owner = 4;</code>
   */
  boolean hasOwner();
  /**
   * <code>optional string owner = 4;</code>
   */
  java.lang.String getOwner();
  /**
   * <code>optional string owner = 4;</code>
   */
  com.google.protobuf.ByteString
      getOwnerBytes();

  /**
   * <code>optional .alluxio.grpc.table.Schema schema = 5;</code>
   */
  boolean hasSchema();
  /**
   * <code>optional .alluxio.grpc.table.Schema schema = 5;</code>
   */
  alluxio.grpc.table.Schema getSchema();
  /**
   * <code>optional .alluxio.grpc.table.Schema schema = 5;</code>
   */
  alluxio.grpc.table.SchemaOrBuilder getSchemaOrBuilder();

  /**
   * <code>optional .alluxio.grpc.table.Layout layout = 6;</code>
   */
  boolean hasLayout();
  /**
   * <code>optional .alluxio.grpc.table.Layout layout = 6;</code>
   */
  alluxio.grpc.table.Layout getLayout();
  /**
   * <code>optional .alluxio.grpc.table.Layout layout = 6;</code>
   */
  alluxio.grpc.table.LayoutOrBuilder getLayoutOrBuilder();

  /**
   * <code>map&lt;string, string&gt; parameters = 7;</code>
   */
  int getParametersCount();
  /**
   * <code>map&lt;string, string&gt; parameters = 7;</code>
   */
  boolean containsParameters(
      java.lang.String key);
  /**
   * Use {@link #getParametersMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, java.lang.String>
  getParameters();
  /**
   * <code>map&lt;string, string&gt; parameters = 7;</code>
   */
  java.util.Map<java.lang.String, java.lang.String>
  getParametersMap();
  /**
   * <code>map&lt;string, string&gt; parameters = 7;</code>
   */

  java.lang.String getParametersOrDefault(
      java.lang.String key,
      java.lang.String defaultValue);
  /**
   * <code>map&lt;string, string&gt; parameters = 7;</code>
   */

  java.lang.String getParametersOrThrow(
      java.lang.String key);

  /**
   * <pre>
   * partitioning scheme
   * </pre>
   *
   * <code>repeated .alluxio.grpc.table.FieldSchema partition_cols = 8;</code>
   */
  java.util.List<alluxio.grpc.table.FieldSchema> 
      getPartitionColsList();
  /**
   * <pre>
   * partitioning scheme
   * </pre>
   *
   * <code>repeated .alluxio.grpc.table.FieldSchema partition_cols = 8;</code>
   */
  alluxio.grpc.table.FieldSchema getPartitionCols(int index);
  /**
   * <pre>
   * partitioning scheme
   * </pre>
   *
   * <code>repeated .alluxio.grpc.table.FieldSchema partition_cols = 8;</code>
   */
  int getPartitionColsCount();
  /**
   * <pre>
   * partitioning scheme
   * </pre>
   *
   * <code>repeated .alluxio.grpc.table.FieldSchema partition_cols = 8;</code>
   */
  java.util.List<? extends alluxio.grpc.table.FieldSchemaOrBuilder> 
      getPartitionColsOrBuilderList();
  /**
   * <pre>
   * partitioning scheme
   * </pre>
   *
   * <code>repeated .alluxio.grpc.table.FieldSchema partition_cols = 8;</code>
   */
  alluxio.grpc.table.FieldSchemaOrBuilder getPartitionColsOrBuilder(
      int index);

  /**
   * <code>optional int64 previous_version = 9;</code>
   */
  boolean hasPreviousVersion();
  /**
   * <code>optional int64 previous_version = 9;</code>
   */
  long getPreviousVersion();

  /**
   * <code>optional int64 version = 10;</code>
   */
  boolean hasVersion();
  /**
   * <code>optional int64 version = 10;</code>
   */
  long getVersion();

  /**
   * <code>optional int64 version_creation_time = 11;</code>
   */
  boolean hasVersionCreationTime();
  /**
   * <code>optional int64 version_creation_time = 11;</code>
   */
  long getVersionCreationTime();
}
