// automatically generated by the FlatBuffers compiler, do not modify

package com.mz.fastapp.flatbuffers_py2java;

import com.google.flatbuffers.BaseVector;
import com.google.flatbuffers.BooleanVector;
import com.google.flatbuffers.ByteVector;
import com.google.flatbuffers.Constants;
import com.google.flatbuffers.DoubleVector;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FloatVector;
import com.google.flatbuffers.IntVector;
import com.google.flatbuffers.LongVector;
import com.google.flatbuffers.ShortVector;
import com.google.flatbuffers.StringVector;
import com.google.flatbuffers.Struct;
import com.google.flatbuffers.Table;
import com.google.flatbuffers.UnionVector;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("unused")
public final class FlatBuffersRequest extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_24_3_25(); }
  public static FlatBuffersRequest getRootAsFlatBuffersRequest(ByteBuffer _bb) { return getRootAsFlatBuffersRequest(_bb, new FlatBuffersRequest()); }
  public static FlatBuffersRequest getRootAsFlatBuffersRequest(ByteBuffer _bb, FlatBuffersRequest obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public FlatBuffersRequest __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public byte type() { int o = __offset(4); return o != 0 ? bb.get(o + bb_pos) : 0; }
  public int hashCode() { int o = __offset(6); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public String className() { int o = __offset(8); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer classNameAsByteBuffer() { return __vector_as_bytebuffer(8, 1); }
  public ByteBuffer classNameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 8, 1); }
  public String fieldName() { int o = __offset(10); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer fieldNameAsByteBuffer() { return __vector_as_bytebuffer(10, 1); }
  public ByteBuffer fieldNameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 10, 1); }
  public String methodName() { int o = __offset(12); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer methodNameAsByteBuffer() { return __vector_as_bytebuffer(12, 1); }
  public ByteBuffer methodNameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 12, 1); }
  public com.mz.fastapp.flatbuffers_py2java.FlatBuffersJavaObject args(int j) { return args(new com.mz.fastapp.flatbuffers_py2java.FlatBuffersJavaObject(), j); }
  public com.mz.fastapp.flatbuffers_py2java.FlatBuffersJavaObject args(com.mz.fastapp.flatbuffers_py2java.FlatBuffersJavaObject obj, int j) { int o = __offset(14); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int argsLength() { int o = __offset(14); return o != 0 ? __vector_len(o) : 0; }
  public com.mz.fastapp.flatbuffers_py2java.FlatBuffersJavaObject.Vector argsVector() { return argsVector(new com.mz.fastapp.flatbuffers_py2java.FlatBuffersJavaObject.Vector()); }
  public com.mz.fastapp.flatbuffers_py2java.FlatBuffersJavaObject.Vector argsVector(com.mz.fastapp.flatbuffers_py2java.FlatBuffersJavaObject.Vector obj) { int o = __offset(14); return o != 0 ? obj.__assign(__vector(o), 4, bb) : null; }

  public static int createFlatBuffersRequest(FlatBufferBuilder builder,
      byte type,
      int hashCode,
      int classNameOffset,
      int fieldNameOffset,
      int methodNameOffset,
      int argsOffset) {
    builder.startTable(6);
    FlatBuffersRequest.addArgs(builder, argsOffset);
    FlatBuffersRequest.addMethodName(builder, methodNameOffset);
    FlatBuffersRequest.addFieldName(builder, fieldNameOffset);
    FlatBuffersRequest.addClassName(builder, classNameOffset);
    FlatBuffersRequest.addHashCode(builder, hashCode);
    FlatBuffersRequest.addType(builder, type);
    return FlatBuffersRequest.endFlatBuffersRequest(builder);
  }

  public static void startFlatBuffersRequest(FlatBufferBuilder builder) { builder.startTable(6); }
  public static void addType(FlatBufferBuilder builder, byte type) { builder.addByte(0, type, 0); }
  public static void addHashCode(FlatBufferBuilder builder, int hashCode) { builder.addInt(1, hashCode, 0); }
  public static void addClassName(FlatBufferBuilder builder, int classNameOffset) { builder.addOffset(2, classNameOffset, 0); }
  public static void addFieldName(FlatBufferBuilder builder, int fieldNameOffset) { builder.addOffset(3, fieldNameOffset, 0); }
  public static void addMethodName(FlatBufferBuilder builder, int methodNameOffset) { builder.addOffset(4, methodNameOffset, 0); }
  public static void addArgs(FlatBufferBuilder builder, int argsOffset) { builder.addOffset(5, argsOffset, 0); }
  public static int createArgsVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startArgsVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endFlatBuffersRequest(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public FlatBuffersRequest get(int j) { return get(new FlatBuffersRequest(), j); }
    public FlatBuffersRequest get(FlatBuffersRequest obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

