

import logging
import os
import threading
import time
import traceback
from types import MethodType


from jni.ipc_generated import *

logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'

)
logger = logging.getLogger("jni")


config = {}


def enable_log():
    config['log'] = True


def LOG(*args):
    if config.get('log', False):
        print(*args)
    pass


def LOGE(*args):
    if config.get('log', False):
        print(*args)


import socket


localSocketClientThreadLocker = threading.Lock()


def bytes_to_int(bytes):
    return int.from_bytes(bytes, byteorder='little', signed=True)


def int_to_bytes(value):
    return value.to_bytes(4, byteorder='little', signed=True)

    # public static byte[] wrapperReadAllBytes(InputStream is) throws IOException {
    #     byte[] nByte = new byte[4];
    #     nByte[0] = (byte) is.read();
    #     nByte[1] = (byte) is.read();
    #     nByte[2] = (byte) is.read();
    #     nByte[3] = (byte) is.read();
    #     int bufferLength = Utils.bytesToInt(nByte);
    #     byte[] buffer = new byte[bufferLength];
    #     int totalBytesRead = 0;
    #     while (totalBytesRead < bufferLength) {
    #         int read = is.read(buffer, totalBytesRead, bufferLength - totalBytesRead);
    #         if (read == -1) {
    #             throw new IOException("Unexpected end of stream");
    #         }
    #         totalBytesRead += read;
    #     }
    #     return buffer;
    # }

    # public static byte[] wrapperWriteAllBytes(byte[] buffer) throws IOException {
    #     byte[] nByte = Utils.intToBytes(buffer.length);
    #     ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    #     byteArrayOutputStream.write(nByte);
    #     byteArrayOutputStream.write(buffer);
    #     return byteArrayOutputStream.toByteArray();
    # }


class ipcLocalSocketClient:
    socket = None

    def __init__(self, socket):
        self.socket = socket

    def __getattr__(self, name):
        return getattr(self.socket, name)

    def readAllBytes(self):
        bufferLength = bytes_to_int(self.recv(4))
        buffer = b""
        totalBytesRead = 0
        while totalBytesRead < bufferLength:
            read = self.recv(bufferLength - totalBytesRead)
            if len(read) == 0:
                raise Exception("Unexpected end of stream")
            buffer += read
            totalBytesRead += len(read)
        return buffer

    def writeAllBytes(self, buffer):
        nByte = int_to_bytes(len(buffer))
        self.send(nByte)
        self.send(buffer)


localSocketClientSendLocker = threading.Lock()

threadOccupation = None


def set_thread_occupation(client: ipcLocalSocketClient):
    localSocketClientSendLocker.acquire()
    global threadOccupation
    threadOccupation = client
    localSocketClientSendLocker.release()


def get_thread_occupation():
    return threadOccupation


def localSocketClientSend(msg):
    #     print("localSocketClientSend:", "start")
    try:
        localSocketClientSendLocker.acquire()
        localSocketClient = get_thread_occupation()
        is_new = False
        if localSocketClient is None or localSocketClient.fileno() == -1:
            localSocketClient = socket.socket(
                socket.AF_UNIX, socket.SOCK_STREAM)
            localSocketClient = ipcLocalSocketClient(localSocketClient)
            socketPath = os.environ.get("JAVA_CONTROL_SOCKET")
            localSocketClient.connect(socketPath)
            is_new = True
        else:
            # 需要先发送前置数据
            notify_buffer = flatbuffers.Builder(1024)
            FlatBuffersResponseStart(notify_buffer)
            FlatBuffersResponseAddType(
                notify_buffer, FlatBuffersResponseType.ThreadOccupation)
            response_offset = FlatBuffersResponseEnd(
                notify_buffer)
            notify_buffer.Finish(response_offset)
            req_buff = notify_buffer.Output()
            localSocketClient.writeAllBytes(req_buff)

        localSocketClient.writeAllBytes(msg)
        result = localSocketClient.readAllBytes()

        if is_new:
            localSocketClient.close()
    finally:
        localSocketClientSendLocker.release()
#     print("localSocketClientSend:", "end")
    if result is None or len(result) == 0:
        return None

    return result


import inspect


def localSocketServerNotifyTask():
    try:
        socketPath = os.environ.get("JAVA_NOTIFY_SOCKET")
        LOG("localSocketServerNotifyTask:", socketPath)

        server = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        server.bind(socketPath)
        server.listen(1)

        while True:
            # print("localSocketServerNotifyTask wait")
            connect, addr = server.accept()
            # print("localSocketServerNotifyTask accept")
            connect = ipcLocalSocketClient(connect)
            set_thread_occupation(connect)

            try:
                result = connect.readAllBytes()

                # print("localSocketServerNotifyTask readAllBytes1")
                notify_message = FlatBuffersRequest.GetRootAs(result)

                if notify_message.Type() == FlatBuffersRequestType.ProxyInvoke:

                    hash_code = notify_message.HashCode()
                    method_name = notify_message.MethodName().decode("utf-8")
                    args = []
                    for i in range(notify_message.ArgsLength()):
                        args.append(to_py_object(
                            notify_message.Args(i)))

                    if GLOBAL_OBJECT_POOL.get(hash_code, None) is not None:
                        proxy_object = GLOBAL_OBJECT_POOL.get(
                            hash_code)
                        result_obj = None
                        if inspect.isfunction(proxy_object._python_object):
                            result_obj = proxy_object._python_object(
                                method_name, *args)
                        else:
                            # 调用对象方法
                            if hasattr(proxy_object._python_object, method_name):
                                method = getattr(
                                    proxy_object._python_object, method_name)
                                result_obj = method(*args)
                            else:
                                LOGE(
                                    "localSocketServerNotifyTask method not found")

                        # 回调完成,结束线程占用
                        set_thread_occupation(None)
                        # print("localSocketServerNotifyTask call finish")

                        notify_buffer = flatbuffers.Builder(1024)
                        result_args = get_obj_flatbuffers_arg(
                            notify_buffer, result_obj)
                        result_offset = createFlatBuffersJavaObject(
                            notify_buffer,
                            result_args[0],
                            result_args[1],
                            result_args[2],
                            result_args[3],
                            result_args[4],
                            result_args[5],
                            result_args[6],
                            result_args[7],
                            result_args[8],
                            result_args[9],
                            result_args[10],
                            result_args[11],
                        )
                        FlatBuffersResponseStart(notify_buffer)
                        FlatBuffersResponseAddData(
                            notify_buffer, result_offset)
                        response_offset = FlatBuffersResponseEnd(
                            notify_buffer)
                        notify_buffer.Finish(response_offset)
                        req_buff = notify_buffer.Output()
                        LOG("localSocketServerNotifyTask response:",
                            len(req_buff))

                        connect.writeAllBytes(req_buff)
                        # print("localSocketServerNotifyTask writeAllBytes end")
                    else:
                        LOGE(
                            "localSocketServerNotifyTask GLOBAL_OBJECT_POOL not found")
                        for key, value in GLOBAL_OBJECT_POOL.items():
                            LOG(key, value)

            except Exception as e:
                LOGE("localSocketServerNotifyTask error:", traceback.format_exc())

            # print("localSocketServerNotifyTask close")
            set_thread_occupation(None)
            connect.close()
            # print("localSocketServerNotifyTask close end")
    except Exception as e:
        LOGE("localSocketServerNotifyTask error:", e)
        time.sleep(1)
        threading.Thread(target=localSocketServerNotifyTask).start()


if "localSocketServerNotifyTaskThread" not in globals() and "localSocketServerNotifyTaskThread" not in locals():
    localSocketServerNotifyTaskThread = threading.Thread(
        target=localSocketServerNotifyTask)
    localSocketServerNotifyTaskThread.start()


def import_class(class_name):
    FlatBuffersrequest = new_flat_request(
        type=FlatBuffersRequestType.GetClass, hash_code=0,
        class_name=class_name, field_name="", method_name="", args=[])
    result = localSocketClientSend(FlatBuffersrequest)
    result = FlatBuffersResponse.GetRootAs(result)
    if result is None:
        return None
    if result.Exception() is not None:
        raise Exception(result.Exception().decode("utf-8"))
    return to_py_object(result.Data())


def new_proxy_object(class_name):
    # result = json.loads(localSocketClientSend(json.dumps(
    #     {'type': 'new_proxy_object', 'class_name': class_name})))
    FlatBuffersrequest = new_flat_request(
        type=FlatBuffersRequestType.NewProxyObject, hash_code=0,
        class_name=class_name, field_name="", method_name="", args=[])
    result = localSocketClientSend(FlatBuffersrequest)
    if result is None:
        return None
    result = FlatBuffersResponse.GetRootAs(result)
    if result.Exception() is not None:
        raise Exception(result.Exception().decode("utf-8"))
    return to_py_object(result.Data())


def get_field_object(id, object_name):
    # result = json.loads(localSocketClientSend(json.dumps(
    #     {'type': 'get_field_object', 'id': id, 'object_name': object_name})))
    FlatBuffersrequest = new_flat_request(
        type=FlatBuffersRequestType.GetFieldObject, hash_code=id,
        class_name="", field_name=object_name, method_name="", args=[])
    result = localSocketClientSend(FlatBuffersrequest)
    result = FlatBuffersResponse.GetRootAs(result)
    if result is None:
        return None
    if result.Exception() is not None:
        raise Exception(result.Exception().decode("utf-8"))
    return to_py_object(result.Data())


def set_field_object(id, object_name, value):
    FlatBuffersrequest = new_flat_request(
        type=FlatBuffersRequestType.SetFieldObject, hash_code=id,
        class_name="", field_name=object_name, method_name="", args=[value])
    localSocketClientSend(FlatBuffersrequest)


def call_method(id, method_name, id_args=[]):
    # result = json.loads(localSocketClientSend(json.dumps(
    #     {'type': 'call_method', 'id': id, 'method_name': method_name, 'id_args': id_args})))
    FlatBuffersrequest = new_flat_request(
        type=FlatBuffersRequestType.CallMethod, hash_code=id,
        class_name="", field_name="", method_name=method_name, args=id_args)
    result = localSocketClientSend(FlatBuffersrequest)
    if result is None:
        return None
    result = FlatBuffersResponse.GetRootAs(result)
    if result.Exception() is not None:
        raise Exception(result.Exception().decode("utf-8"))

    LOG(f"call_method method_name:{method_name} result: ",
        result.Data().Type())
    return to_py_object(result.Data())


def new_object(class_id, id_args=[]):
    # result = json.loads(localSocketClientSend(json.dumps(
    #     {'type': 'new_object', 'class_id': class_id, 'id_args': id_args})))
    FlatBuffersrequest = new_flat_request(
        type=FlatBuffersRequestType.NewObject, hash_code=class_id,
        class_name="", field_name="", method_name="", args=id_args)
    result = localSocketClientSend(FlatBuffersrequest)
    if result is None:
        return None
    result = FlatBuffersResponse.GetRootAs(result)
    if result.Exception() is not None:
        raise Exception(result.Exception().decode("utf-8"))
    return to_py_object(result.Data())


GLOBAL_OBJECT_POOL = {}

jni_inline_filed = [
    "_java_object_type",
    "_java_object_hashcode_int",
    "_java_attr_lazy_name",
    "_python_object",
    "_parent_java_object",
]


class JavaObject:
    _java_object_type = -1
    _java_object_hashcode_int = None
    _java_attr_lazy_name = None

    def __init__(self, id=None, lazy_name=None, parent=None):
        self._java_object_hashcode_int = id
        self._java_attr_lazy_name = lazy_name
        self._parent_java_object = parent

    def _jni_internal_load_object(self):
        LOG("load_object ", self._java_object_hashcode_int,
            self._java_attr_lazy_name)
        if self._java_attr_lazy_name is not None:
            new_obj = get_field_object(
                self._java_object_hashcode_int, self._java_attr_lazy_name)
            LOG("load_object", new_obj)
            if isinstance(new_obj, JavaObject):
                setattr(new_obj, "_parent_java_object", self)
            return new_obj
        return self

    def __call__(self, *args):
        LOG("JavaObject __call__ ", self._java_object_hashcode_int,
            self._java_attr_lazy_name, args)
        if self._java_object_type != -1 and self._java_object_type != 2:
            raise Exception("JavaObject can't call")
        self._java_object_type = 2
        result = call_method(
            self._java_object_hashcode_int, self._java_attr_lazy_name, args)
        LOG("call result_id:", type(result), result)
        return result

    def __getattr__(self, attr):
        if self._java_object_type != -1 and self._java_object_type != 1:
            raise Exception("JavaObject can't get attr")
        self._java_object_type = 1
        LOG("JavaObject __getattr__", attr, self._java_object_hashcode_int)

        load_object = self._jni_internal_load_object()

        try:
            # 尝试获取字段值
            field_value = get_field_object(load_object._java_object_hashcode_int, attr)
            return field_value
        except Exception as e:
            pass

        if isinstance(load_object, JavaObject):
            return JavaObject(load_object._java_object_hashcode_int, attr, self)
        else:
            return getattr(load_object, attr)

    def __setattr__(self, attr, value):
        if attr in self.__class__.__dict__ or attr in jni_inline_filed:
            object.__setattr__(self, attr, value)
            return

        LOG("JavaObject __setattr__", attr, value)
        load_object = self._jni_internal_load_object()
        if isinstance(load_object, JavaObject):
            set_field_object(
                load_object._java_object_hashcode_int, attr, value)
        else:
            raise Exception("JavaObject can't set attr")

    # 被gc回收时调用
    def __del__(self):
        if self._java_attr_lazy_name is not None:
            return
        if self._java_object_hashcode_int is not None and self._java_object_type != -1:
            # localSocketClientSend(json.dumps(
            #     {'type': 'delete_object', 'id': self._java_object_hashcode_int}))
            LOG("JavaObject __del__", self._java_object_hashcode_int)
            FlatBuffersrequest = new_flat_request(
                type=FlatBuffersRequestType.DeleteObject, hash_code=self._java_object_hashcode_int,
                class_name="", field_name="", method_name="", args=[])
            localSocketClientSend(FlatBuffersrequest)


class JavaClass:
    _java_object_hashcode_int = None

    def __init__(self, class_name):
        clazz = import_class(class_name)
        assert isinstance(clazz, JavaObject)
        self._java_object_hashcode_int = clazz._java_object_hashcode_int

    def __call__(self, *args):
        return new_object(self._java_object_hashcode_int, args)

    def __getattr__(self, attr):
        # LOG("JavaClass __getattr__", attr, self._java_object_hashcode_int)
        
        try:
            # 尝试获取字段值
            field_value = get_field_object(self._java_object_hashcode_int, attr)
            return field_value
        except Exception as e:
            pass
        return JavaObject(self._java_object_hashcode_int, attr, self)

    def __setattr__(self, attr, value):
        if attr in self.__class__.__dict__ or attr in jni_inline_filed:
            object.__setattr__(self, attr, value)
            return

        LOG("JavaObject __setattr__", attr, value)
        set_field_object(
            self._java_object_hashcode_int, attr, value)

    # 被gc回收时调用

    def __del__(self):
        if self._java_object_hashcode_int is not None:
            # localSocketClientSend(json.dumps(
            #     {'type': 'delete_object', 'id': self._java_object_hashcode_int}))
            FlatBuffersrequest = new_flat_request(
                type=FlatBuffersRequestType.DeleteObject, hash_code=self._java_object_hashcode_int,
                class_name="", field_name="", method_name="", args=[])
            localSocketClientSend(FlatBuffersrequest)


class JavaProxy(JavaObject):
    _python_object = None

    def __init__(self, class_name, py_obj):
        proxy = new_proxy_object(class_name)
        assert isinstance(proxy, JavaObject)

        if proxy._java_object_hashcode_int is None:
            raise Exception("JavaProxy create error")

        super().__init__(
            id=proxy._java_object_hashcode_int
        )
        self._python_object = py_obj
        GLOBAL_OBJECT_POOL.update({self._java_object_hashcode_int: self})

    def _jni_internal_release(self):
        if self._java_object_hashcode_int is not None:
            # localSocketClientSend(json.dumps(
            #     {'type': 'delete_object', 'id': self._java_object_hashcode_int}))
            FlatBuffersrequest = new_flat_request(
                type=FlatBuffersRequestType.DeleteObject, hash_code=self._java_object_hashcode_int,
                class_name="", field_name="", method_name="", args=[])
            localSocketClientSend(FlatBuffersrequest)
            GLOBAL_OBJECT_POOL.pop(self._java_object_hashcode_int)


def createFlatBuffersJavaObject(builder, type, refValue, bytesValueOffset, stringValueOffset, byteValue, shortValue, intValue, longValue, floatValue, doubleValue, booleanValue, arrayValueOffset):
    FlatBuffersJavaObjectStart(builder)
    FlatBuffersJavaObjectAddDoubleValue(builder, doubleValue)
    FlatBuffersJavaObjectAddLongValue(builder, longValue)
    FlatBuffersJavaObjectAddFloatValue(builder, floatValue)
    FlatBuffersJavaObjectAddIntValue(builder, intValue)
    FlatBuffersJavaObjectAddStringValue(builder, stringValueOffset)
    FlatBuffersJavaObjectAddBytesValue(builder, bytesValueOffset)
    FlatBuffersJavaObjectAddRefValue(builder, refValue)
    FlatBuffersJavaObjectAddShortValue(builder, shortValue)
    FlatBuffersJavaObjectAddBooleanValue(builder, booleanValue)
    FlatBuffersJavaObjectAddByteValue(builder, byteValue)
    FlatBuffersJavaObjectAddType(builder, type)
    FlatBuffersJavaObjectAddArrayValue(builder, arrayValueOffset)
    return FlatBuffersJavaObjectEnd(builder)


def createOffsetVector(builder: flatbuffers.Builder, data):
    builder.StartVector(4, len(data), 4)
    for i in range(len(data) - 1, -1, -1):
        builder.PrependUOffsetTRelative(data[i])
    return builder.EndVector(len(data))


def get_obj_flatbuffers_arg(builder, py_obj):
    value_type = 0
    ref_value = 0
    bytes_value_offset = 0
    string_value_offset = 0
    byte_value = 0
    short_value = 0
    int_value = 0
    long_value = 0
    float_value = 0
    double_value = 0
    boolean_value = False
    array_value_offset = 0
    if isinstance(py_obj, bytes):
        value_type = FlatBuffersJavaTypeEnum.Bytes
        string_value_offset = builder.CreateString(py_obj)
        LOG("new_flat_request args_type:Bytes :", string_value_offset)
    elif isinstance(py_obj, str) and type(py_obj) == str:
        value_type = FlatBuffersJavaTypeEnum.String
        string_value_offset = builder.CreateString(py_obj)
        LOG("new_flat_request args_type:String :", string_value_offset)
    elif isinstance(py_obj, bool) and type(py_obj) == bool:
        value_type = FlatBuffersJavaTypeEnum.Boolean
        boolean_value = py_obj
        LOG("new_flat_request args_type:Boolean :", boolean_value)
    elif isinstance(py_obj, int) and type(py_obj) == int:
        value_type = FlatBuffersJavaTypeEnum.Int
        int_value = py_obj
        LOG("new_flat_request args_type:Int :", int_value)
    elif isinstance(py_obj, float) and type(py_obj) == float:
        value_type = FlatBuffersJavaTypeEnum.Float
        float_value = py_obj
        LOG("new_flat_request args_type:Float :", float_value)
    elif py_obj is None:
        value_type = FlatBuffersJavaTypeEnum.Null
        LOG("new_flat_request args_type:Null")
    elif isinstance(py_obj, JavaObject):
        new_object = py_obj._jni_internal_load_object()
        if isinstance(new_object, JavaObject):
            value_type = FlatBuffersJavaTypeEnum.Ref
            ref_value = new_object._java_object_hashcode_int
            LOG("new_flat_request args_type:Ref :", ref_value)
        else:
            return get_obj_flatbuffers_arg(builder, new_object)

    elif isinstance(py_obj, JavaClass):
        value_type = FlatBuffersJavaTypeEnum.Ref
        ref_value = py_obj._java_object_hashcode_int
        LOG("new_flat_request args_type:Ref :", ref_value)
    elif isinstance(py_obj, JavaProxy):
        value_type = FlatBuffersJavaTypeEnum.Ref
        ref_value = py_obj._java_object_hashcode_int
        LOG("new_flat_request args_type:Ref :", ref_value)
    elif isinstance(py_obj, list):
        value_type = FlatBuffersJavaTypeEnum.Array
        obj_array = []
        for item in py_obj:
            arg_out = get_obj_flatbuffers_arg(builder, item)
            args_type = arg_out[0]
            ref_value = arg_out[1]
            bytes_value_offset = arg_out[2]
            string_value_offset = arg_out[3]
            byte_value = arg_out[4]
            short_value = arg_out[5]
            int_value = arg_out[6]
            long_value = arg_out[7]
            float_value = arg_out[8]
            double_value = arg_out[9]
            boolean_value = arg_out[10]
            array_value_offset = arg_out[11]

            arg_offset = createFlatBuffersJavaObject(builder, args_type, ref_value, bytes_value_offset, string_value_offset,
                                                     byte_value, short_value, int_value, long_value, float_value, double_value, boolean_value, array_value_offset)

            obj_array.append(arg_offset)
        array_value_offset = createOffsetVector(builder, obj_array)

    else:
        raise Exception("not support type")

    return (value_type, ref_value, bytes_value_offset, string_value_offset, byte_value, short_value, int_value, long_value, float_value, double_value, boolean_value, array_value_offset)


def new_flat_request(type, hash_code, class_name, field_name, method_name, args=[]):
    builder = flatbuffers.Builder(1024)
    class_name_offset = builder.CreateString(class_name)
    field_name_offset = builder.CreateString(field_name)
    method_name_offset = builder.CreateString(method_name)

    args_offset_array = []
    for arg in args:
        arg_out = get_obj_flatbuffers_arg(
            builder, arg)
        args_type = arg_out[0]
        ref_value = arg_out[1]
        bytes_value_offset = arg_out[2]
        string_value_offset = arg_out[3]
        byte_value = arg_out[4]
        short_value = arg_out[5]
        int_value = arg_out[6]
        long_value = arg_out[7]
        float_value = arg_out[8]
        double_value = arg_out[9]
        boolean_value = arg_out[10]
        array_value_offset = arg_out[11]

        arg_offset = createFlatBuffersJavaObject(builder, args_type, ref_value, bytes_value_offset, string_value_offset,
                                                 byte_value, short_value, int_value, long_value, float_value, double_value, boolean_value, array_value_offset)
        args_offset_array.append(arg_offset)

    args_offset = createOffsetVector(builder, args_offset_array)

    FlatBuffersRequestStart(builder)
    FlatBuffersRequestAddType(builder, type)
    FlatBuffersRequestAddHashCode(builder, hash_code)
    FlatBuffersRequestAddClassName(builder, class_name_offset)
    FlatBuffersRequestAddFieldName(builder, field_name_offset)
    FlatBuffersRequestAddMethodName(builder, method_name_offset)
    FlatBuffersRequestAddArgs(builder, args_offset)
    request_offset = FlatBuffersRequestEnd(builder)
    builder.Finish(request_offset)
    return builder.Output()


def to_py_object(data):

    # FlatBuffersJavaTypeEnum.Ref:
    # FlatBuffersJavaTypeEnum.Bytes:
    # FlatBuffersJavaTypeEnum.String:
    # FlatBuffersJavaTypeEnum.Null:
    # FlatBuffersJavaTypeEnum.Byte:
    # FlatBuffersJavaTypeEnum.Short:
    # FlatBuffersJavaTypeEnum.Int:
    # FlatBuffersJavaTypeEnum.Long:
    # FlatBuffersJavaTypeEnum.Float:
    # FlatBuffersJavaTypeEnum.Double:
    # FlatBuffersJavaTypeEnum.Boolean:
    # FlatBuffersJavaTypeEnum.Char:

    if data is None:
        return None

    if data.Type() == FlatBuffersJavaTypeEnum.Ref:
        LOG("to_py_object ref")
        ref = data.RefValue()
        if ref == 0:
            return None
        return JavaObject(id=ref)
    elif data.Type() == FlatBuffersJavaTypeEnum.Bytes:
        LOG("to_py_object bytes")
        return data.StringValue()
    elif data.Type() == FlatBuffersJavaTypeEnum.String:
        LOG("to_py_object string")
        return data.StringValue().decode("utf-8")
    elif data.Type() == FlatBuffersJavaTypeEnum.Null:
        LOG("to_py_object null")
        return None
    elif data.Type() == FlatBuffersJavaTypeEnum.Byte:
        LOG("to_py_object byte")
        return data.ByteValue()
    elif data.Type() == FlatBuffersJavaTypeEnum.Short:
        LOG("to_py_object short")
        return data.ShortValue()
    elif data.Type() == FlatBuffersJavaTypeEnum.Int:
        LOG("to_py_object int")
        return data.IntValue()
    elif data.Type() == FlatBuffersJavaTypeEnum.Long:
        LOG("to_py_object long")
        return data.LongValue()
    elif data.Type() == FlatBuffersJavaTypeEnum.Float:
        LOG("to_py_object float")
        return data.FloatValue()
    elif data.Type() == FlatBuffersJavaTypeEnum.Double:
        LOG("to_py_object double")
        return data.DoubleValue()
    elif data.Type() == FlatBuffersJavaTypeEnum.Boolean:
        LOG("to_py_object boolean")
        return data.BooleanValue()
    elif data.Type() == FlatBuffersJavaTypeEnum.Char:
        LOG("to_py_object char")
        return data.ByteValue()
    elif data.Type() == FlatBuffersJavaTypeEnum.Array:
        LOG("to_py_object array")
        result = []
        for i in range(data.ArrayValueLength()):
            result.append(to_py_object(data.ArrayValue(i)))
        return result
    else:
        raise Exception("not support type")
