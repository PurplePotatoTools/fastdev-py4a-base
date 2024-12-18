package com.mz.fastapp;

import static com.mz.fastapp.Py2Java.objectPool;

import com.mz.fastapp.flatbuffers_py2java.*;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import com.google.flatbuffers.FlatBufferBuilder;
import com.mz.fastapp.flatbuffers_py2java.FlatBuffersJavaObject;
import com.mz.fastapp.flatbuffers_py2java.FlatBuffersRequest;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import com.android.dx.stock.ProxyBuilder;

class JavaIdRef {
    public int id;

    public JavaIdRef(Object object) {
        this.id = object.hashCode();
    }
}

class Py2JavaUtils {
    static boolean isDebug = false;

    public static int BUFFER_SIZE = 1024;

    public static void LogD(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
    }

    public static Constructor<?> getCompatibleDeclaredConstructor(
            Class<?> clazz, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> compatibleConstructor = null;
        for (int i = 0; i < constructors.length; i++)
            if (parameterTypesCampatible(parameterTypes,
                    constructors[i].getParameterTypes())
                    && ((compatibleConstructor == null) || parameterTypesCampatible(
                    constructors[i].getParameterTypes(),
                    compatibleConstructor.getParameterTypes())))
                compatibleConstructor = constructors[i];
        if (compatibleConstructor == null)
            throw new NoSuchMethodException(clazz.getName() + ".<init>"
                    + argumentTypesToString(parameterTypes));
        return compatibleConstructor;
    }

    public static Method getCompatibleMethod(Class<?> clazz, String methodName,
                                             Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            Method method = clazz.getMethod(methodName, parameterTypes);
            return method;
        } catch (Exception ignored) {

        }

        Method[] methods = clazz.getMethods();
        LogD("socketIPC", "getCompatibleMethod: " + methods.length);

        Method compatibleMethod = null;
        for (int i = 0; i < methods.length; i++) {
            //
            // LogD("socketIPC", "getCompatibleMethod1: " + methods[i].getName() + " " +
            // methodName);
            // LogD("socketIPC", "getCompatibleMethod2: " + methods[i].getParameterTypes() +
            // " " + parameterTypes);
            // if (compatibleMethod != null) {
            // LogD("socketIPC", "getCompatibleMethod3: " + methods[i].getParameterTypes() +
            // " "
            // + compatibleMethod.getParameterTypes());
            // }
            if (methods[i].getName().equals(methodName)
                    && parameterTypesCampatible(parameterTypes,
                    methods[i].getParameterTypes())
                    && ((compatibleMethod == null) || parameterTypesCampatible(
                    methods[i].getParameterTypes(),
                    compatibleMethod.getParameterTypes()))) {
                compatibleMethod = methods[i];
            }
        }
        if (compatibleMethod == null)
            throw new NoSuchMethodException(clazz.getName() + "." + methodName
                    + argumentTypesToString(parameterTypes));
        return compatibleMethod;
    }

    private static boolean forcedCompatibility(Class<?> a1, Class<?> a2) {

        if (a1 == null) {
            return true;
        }

        if (a1.equals(a2)) {
            return true;
        }

        // 如果a1是a2的子类或实现类，则兼容
        if (a2.isAssignableFrom(a1)) {
            return true;
        }

        // 处理基本类型和包装类型之间的兼容性
        if ((a1 == int.class && a2 == Integer.class) || (a1 == Integer.class && a2 == int.class)) {
            return true;
        }
        if ((a1 == long.class && a2 == Long.class) || (a1 == Long.class && a2 == long.class)) {
            return true;
        }
        if ((a1 == double.class && a2 == Double.class) || (a1 == Double.class && a2 == double.class)) {
            return true;
        }
        if ((a1 == float.class && a2 == Float.class) || (a1 == Float.class && a2 == float.class)) {
            return true;
        }
        if ((a1 == boolean.class && a2 == Boolean.class) || (a1 == Boolean.class && a2 == boolean.class)) {
            return true;
        }
        if ((a1 == char.class && a2 == Character.class) || (a1 == Character.class && a2 == char.class)) {
            return true;
        }
        if ((a1 == byte.class && a2 == Byte.class) || (a1 == Byte.class && a2 == byte.class)) {
            return true;
        }
        if ((a1 == short.class && a2 == Short.class) || (a1 == Short.class && a2 == short.class)) {
            return true;
        }

        // 如果没有其他兼容性规则，则不兼容
        return false;
    }

    private static boolean parameterTypesCampatible(Class<?>[] a1, Class<?>[] a2) {
        LogD("socketIPC",
                "parameterTypesCampatible: " + a1 + " " + a2 + " " + a1.length + " " + a2.length);
        if (a1 == null) {

            return a2 == null || a2.length == 0;
        }
        if (a2 == null)
            return a1.length == 0;
        if (a1.length != a2.length)
            return false;
        for (int i = 0; i < a1.length; i++) {
            LogD("socketIPC",
                    "parameterTypesCampatible: " + a1[i] + " " + a2[i]);
            if (forcedCompatibility(a1[i], a2[i])) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static String argumentTypesToString(Class<?>[] argTypes) {
        StringBuilder buf = new StringBuilder("(");
        if (argTypes != null) {
            for (int i = 0; i < argTypes.length; i++) {
                buf.append(i > 0 ? ", " : "");
                buf.append((argTypes[i] == null) ? "null"
                        : argTypes[i]
                        .getName());
            }
        }
        return buf.append(")").toString();
    }

    public static byte[] newNotifyRequest(byte type, int hashCode, String className, String fieldName,
                                          String methodName,
                                          Object[] args) {

        FlatBufferBuilder builder = new FlatBufferBuilder(BUFFER_SIZE);

        int classNameOffset = builder.createString(className);
        int fieldNameOffset = builder.createString(fieldName);
        int methodNameOffset = builder.createString(methodName);

        int[] argsOffsetArray = new int[args.length];
        for (int i = 0; i < args.length; i++) {
            int argOffset = newFBJavaObject(builder, args[i]);
            argsOffsetArray[i] = argOffset;
        }
        int argsOffset = FlatBuffersRequest.createArgsVector(builder, argsOffsetArray);
        FlatBuffersRequest.startFlatBuffersRequest(builder);
        FlatBuffersRequest.addType(builder, type);
        FlatBuffersRequest.addHashCode(builder, hashCode);
        FlatBuffersRequest.addClassName(builder, classNameOffset);
        FlatBuffersRequest.addFieldName(builder, fieldNameOffset);
        FlatBuffersRequest.addMethodName(builder, methodNameOffset);
        FlatBuffersRequest.addArgs(builder, argsOffset);
        int requestOffset = FlatBuffersRequest.endFlatBuffersRequest(builder);
        builder.finish(requestOffset);
        return builder.sizedByteArray();

    }

    public static int newFBJavaObject(FlatBufferBuilder builder, Object data) {
        byte vType;
        int refValue = 0;
        int bytesValueOffset = 0;
        int stringValueOffset = 0;
        byte byteValue = 0;
        short shortValue = 0;
        int intValue = 0;
        long longValue = 0;
        float floatValue = 0;
        double doubleValue = 0;
        boolean booleanValue = false;
        int arrayValueOffset = 0;

        if (data instanceof byte[]) {
            byte[] value = (byte[]) data;
            vType = FlatBuffersJavaTypeEnum.Bytes;
            // bytesValueOffset = builder.createByteVector(value);
            stringValueOffset = builder.createString(ByteBuffer.wrap(value));
        } else if (data instanceof String) {
            vType = FlatBuffersJavaTypeEnum.String;
            stringValueOffset = builder.createString((String) data);
        } else if (data instanceof Integer) {
            vType = FlatBuffersJavaTypeEnum.Int;
            intValue = (int) data;
        } else if (data instanceof Long) {
            vType = FlatBuffersJavaTypeEnum.Long;
            longValue = (long) data;
        } else if (data instanceof Float) {
            vType = FlatBuffersJavaTypeEnum.Float;
            floatValue = (float) data;
        } else if (data instanceof Double) {
            vType = FlatBuffersJavaTypeEnum.Double;
            doubleValue = (double) data;
        } else if (data instanceof Boolean) {
            vType = FlatBuffersJavaTypeEnum.Boolean;
            booleanValue = (boolean) data;
        } else if (data instanceof Character) {
            vType = FlatBuffersJavaTypeEnum.Short;
            shortValue = (short) data;
        } else if (data instanceof Short) {
            vType = FlatBuffersJavaTypeEnum.Short;
            shortValue = (short) data;
        } else if (data == null) {
            vType = FlatBuffersJavaTypeEnum.Null;
        } else if (data instanceof JavaIdRef) {
            vType = FlatBuffersJavaTypeEnum.Ref;
            JavaIdRef javaIdRef = (JavaIdRef) data;
            refValue = javaIdRef.id;

        } else if (data.getClass().isArray()) {
            vType = FlatBuffersJavaTypeEnum.Array;
            int size = Array.getLength(data);
            int[] arrayValue = new int[size];
            for (int i = 0; i < size; i++) {
                arrayValue[i] = newFBJavaObject(builder, Array.get(data, i));
            }
            arrayValueOffset = FlatBuffersJavaObject.createArrayValueVector(builder, arrayValue);
        } else {
            // throw new IllegalArgumentException("Unsupported type: " + data.getClass());
            JavaIdRef javaIdRef = new JavaIdRef(data);
            vType = FlatBuffersJavaTypeEnum.Ref;
            refValue = javaIdRef.id;
            objectPool.put(javaIdRef.id, data);
        }

        return FlatBuffersJavaObject.createFlatBuffersJavaObject(
                builder,
                vType,
                refValue,
                bytesValueOffset,
                stringValueOffset,
                byteValue,
                shortValue,
                intValue,
                longValue,
                floatValue,
                doubleValue,
                booleanValue,
                arrayValueOffset);
    }

    public static boolean isBaseType(Object obj) {
        if (obj == null) {
            return true;
        }

        if (obj.getClass().isArray()) {
            return true;
        }

        return obj instanceof String || obj instanceof Integer || obj instanceof Long || obj instanceof Float
                || obj instanceof Double || obj instanceof Boolean || obj instanceof Character || obj instanceof Short
                || obj instanceof byte[];
    }

    public static byte[] newResponse(Object result) {
        FlatBufferBuilder builder = new FlatBufferBuilder(BUFFER_SIZE);
        if (result instanceof Exception) {
            String exceptionString = Utils.getStackTraceString((Exception) result);
            int exceptionOffset = builder.createString(exceptionString);
            FlatBuffersResponse.startFlatBuffersResponse(builder);
            FlatBuffersResponse.addException(builder, exceptionOffset);
            int responseOffset = FlatBuffersResponse.endFlatBuffersResponse(builder);
            builder.finish(responseOffset);
            return builder.sizedByteArray();
        } else {
            int dataOffset = newFBJavaObject(builder, result);
            FlatBuffersResponse.startFlatBuffersResponse(builder);
            FlatBuffersResponse.addData(builder, dataOffset);
            int responseOffset = FlatBuffersResponse.endFlatBuffersResponse(builder);
            builder.finish(responseOffset);
            return builder.sizedByteArray();
        }
    }

    public static Object fbToObject(FlatBuffersJavaObject fbObject) {
        switch (fbObject.type()) {
            case FlatBuffersJavaTypeEnum.Null:
                return null;
            case FlatBuffersJavaTypeEnum.Ref:
                return objectPool.get(fbObject.refValue());
            case FlatBuffersJavaTypeEnum.String:
                return fbObject.stringValue();
            case FlatBuffersJavaTypeEnum.Int:
                return fbObject.intValue();
            case FlatBuffersJavaTypeEnum.Long:
                return fbObject.longValue();
            case FlatBuffersJavaTypeEnum.Float:
                return fbObject.floatValue();
            case FlatBuffersJavaTypeEnum.Double:
                return fbObject.doubleValue();
            case FlatBuffersJavaTypeEnum.Boolean:
                return fbObject.booleanValue();
            case FlatBuffersJavaTypeEnum.Short:
                return fbObject.shortValue();
            case FlatBuffersJavaTypeEnum.Bytes:
                ByteBuffer buffer = fbObject.stringValueAsByteBuffer();
                byte[] output = new byte[buffer.remaining()];
                buffer.get(output);
                return output;
            case FlatBuffersJavaTypeEnum.Array:
                int size = fbObject.arrayValueLength();
                //
                // Object[] objects = new Object[size];
                // for (int i = 0; i < size; i++) {
                // objects[i] = fbToObject(fbObject.arrayValue(i));
                // }
                // return objects;

                LogD("socketIPC", "FlatBuffersJavaTypeEnum.Array fbToObject size: " + size);
                if (size == 0) {
                    return null;
                }
                Class clazz = fbToClass(fbObject.arrayValue(0));
                LogD("socketIPC", "FlatBuffersJavaTypeEnum.Array fbToObject clazz: " + clazz);
                Object array = Array.newInstance(clazz, size);

                for (int i = 0; i < size; i++) {
                    Array.set(array, i, fbToObject(fbObject.arrayValue(i)));
                }
                return array;

        }
        return null;
    }

    public static Object[] fbArgsToObjects(FlatBuffersRequest request) {
        Object[] args = new Object[request.argsLength()];
        for (int i = 0; i < request.argsLength(); i++) {
            FlatBuffersJavaObject arg = request.args(i);
            args[i] = fbToObject(arg);
        }
        return args;
    }

    public static Class fbToClass(FlatBuffersJavaObject fbObject) {
        switch (fbObject.type()) {
            case FlatBuffersJavaTypeEnum.Null:
                return null;
            case FlatBuffersJavaTypeEnum.Ref:
                return objectPool.get(fbObject.refValue()).getClass();
            case FlatBuffersJavaTypeEnum.String:
                return String.class;
            case FlatBuffersJavaTypeEnum.Int:
                return int.class;
            case FlatBuffersJavaTypeEnum.Long:
                return long.class;
            case FlatBuffersJavaTypeEnum.Float:
                return float.class;
            case FlatBuffersJavaTypeEnum.Double:
                return double.class;
            case FlatBuffersJavaTypeEnum.Boolean:
                return boolean.class;
            case FlatBuffersJavaTypeEnum.Short:
                return short.class;
            case FlatBuffersJavaTypeEnum.Bytes:
                return byte[].class;
            case FlatBuffersJavaTypeEnum.Array:
                int size = fbObject.arrayValueLength();
                if (size == 0) {
                    return null;
                }

                Class clazz = null;
                for (int i = 0; i < size; i++) {
                    clazz = fbToClass(fbObject.arrayValue(i));
                    if (clazz != null) {
                        break;
                    }
                }

                // 初始化 clazz 类型的数组
                if (clazz != null) {
                    return Array.newInstance(clazz, 0).getClass();
                }

        }
        return null;
    }

    public static Class[] fbArgsToClass(FlatBuffersRequest request) {
        Class[] args = new Class[request.argsLength()];
        for (int i = 0; i < request.argsLength(); i++) {
            FlatBuffersJavaObject arg = request.args(i);
            args[i] = fbToClass(arg);
        }
        return args;
    }
}

public class Py2Java {

    public static ConcurrentHashMap<Integer, Object> objectPool = new ConcurrentHashMap<>();

    public static byte[] execute(byte[] data) {

        FlatBuffersRequest request = FlatBuffersRequest.getRootAsFlatBuffersRequest(ByteBuffer.wrap(data));

        switch (request.type()) {
            case FlatBuffersRequestType.GetClass:
                try {
                    String className = request.className();
                    Class clazz = Class.forName(className);
                    return Py2JavaUtils.newResponse(clazz);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Py2JavaUtils.newResponse(e);
                }

            case FlatBuffersRequestType.GetFieldObject:
                Py2JavaUtils.LogD("socketIPC", "execute: get_field_object");
                try {
                    Integer id = request.hashCode();

                    String objectName = request.fieldName();
                    if (objectPool.get(id) == null) {
                        Py2JavaUtils.LogD("socketIPC",
                                "execute: get_field_object: " + id + " is null, field name: " + objectName);
                        throw new Exception("execute: get_field_object: " + id + " is null, field name: " + objectName);
                    }
                    Class clazz1;
                    Object _this = null;
                    if (!Class.class.isInstance(objectPool.get(id))) {
                        _this = objectPool.get(id);
                        clazz1 = _this.getClass();
                    } else {
                        clazz1 = (Class) objectPool.get(id);
                    }
                    Object object = clazz1.getField(objectName).get(_this);
                    if (object == null) {
                        Py2JavaUtils.LogD("socketIPC", "execute: get_field " + objectName + " is null");
                    }
                    return Py2JavaUtils.newResponse(object);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Py2JavaUtils.newResponse(e);
                }
            case FlatBuffersRequestType.SetFieldObject:
                Py2JavaUtils.LogD("socketIPC", "execute: set_field_object");
                try {
                    Integer id = request.hashCode();

                    if (objectPool.get(id) == null) {
                        Py2JavaUtils.LogD("socketIPC",
                                "execute: set_field_object: " + id + " is null");
                        throw new Exception("execute: set_field_object: " + id + " is null");
                    }

                    String objectName = request.fieldName();
                    Class clazz1;
                    Object _this = null;
                    if (!Class.class.isInstance(objectPool.get(id))) {
                        _this = objectPool.get(id);
                        clazz1 = _this.getClass();
                    } else {
                        clazz1 = (Class) objectPool.get(id);
                    }

                    Object[] args = Py2JavaUtils.fbArgsToObjects(request);

                    Object value = args[0];

                    clazz1.getField(objectName).set(_this, value);

                    return Py2JavaUtils.newResponse(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Py2JavaUtils.newResponse(e);
                }

            case FlatBuffersRequestType.CallMethod:

                try {
                    String methodName = request.methodName();
                    Integer id = request.hashCode();

                    Object[] args = Py2JavaUtils.fbArgsToObjects(request);
                    Class[] parameterTypes = Py2JavaUtils.fbArgsToClass(request);

                    Class clazz2;
                    Object _this = null;

                    if (objectPool.get(id) == null) {
                        Py2JavaUtils.LogD("socketIPC",
                                "execute: call_method: " + id + " is null");
                        throw new Exception("execute: call_method: " + id + " is null");
                    }
                    if (!Class.class.isInstance(objectPool.get(id))) {
                        _this = objectPool.get(id);
                        clazz2 = _this.getClass();
                    } else {
                        clazz2 = (Class) objectPool.get(id);
                    }

                    Method method = Py2JavaUtils.getCompatibleMethod(clazz2, methodName, parameterTypes);
                    Py2JavaUtils.LogD("socketIPC", "execute: call_method method_name: " + method);
                    Object result = method.invoke(_this, args);
                    if (result == null) {
                        Py2JavaUtils.LogD("socketIPC", "execute: call_method " + methodName + " is null");
                        return Py2JavaUtils.newResponse(null);
                    }

                    return Py2JavaUtils.newResponse(result);

                } catch (Exception e) {
                    e.printStackTrace();
                    return Py2JavaUtils.newResponse(e);
                }
            case FlatBuffersRequestType.NewObject:
                Py2JavaUtils.LogD("socketIPC", "execute: new_object");
                try {
                    Integer classId = request.hashCode();
                    Class clazz3 = (Class) objectPool.get(classId);
                    Object[] args = Py2JavaUtils.fbArgsToObjects(request);
                    Class[] parameterTypes = Py2JavaUtils.fbArgsToClass(request);

                    Constructor<?> constructor = Py2JavaUtils.getCompatibleDeclaredConstructor(clazz3, parameterTypes);
                    Object result = constructor.newInstance(args);
                    return Py2JavaUtils.newResponse(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Py2JavaUtils.newResponse(e);
                }
            case FlatBuffersRequestType.NewProxyObject:
                Py2JavaUtils.LogD("socketIPC", "execute: new_proxy");
                try {
                    String className = request.className();
                    Object result = newProxy(className);
                    Py2JavaUtils.LogD("socketIPC", "execute: new_proxy: " + result + " className:" + className);

                    return Py2JavaUtils.newResponse(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Py2JavaUtils.newResponse(e);
                }

            case FlatBuffersRequestType.GetValue:
                Py2JavaUtils.LogD("socketIPC", "execute: get_value");
                try {
                    // Integer id = Utils.castToInt(map.get("id"));
                    Integer id = request.hashCode();
                    Object result = objectPool.get(id);
                    Py2JavaUtils.LogD("socketIPC", "execute: get_value: id: " + id + " object: " + result);
                    return Py2JavaUtils.newResponse(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Py2JavaUtils.newResponse(e);
                }
            case FlatBuffersRequestType.DeleteObject:
                Py2JavaUtils.LogD("socketIPC", "execute: delete_object");
                try {
                    Integer id = request.hashCode();
                    objectPool.remove(id);
                    return Py2JavaUtils.newResponse(null);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Py2JavaUtils.newResponse(e);
                }

            default:
                return Py2JavaUtils.newResponse(null);
        }
    }

    public static Object interfaceUnifiedCallback(Object proxy, String methodName, Object... args)
            throws Exception {
        int hashCode = System.identityHashCode(proxy);

        if (args == null) {
            args = new Object[]{};
        }

        switch (methodName) {
            case "toString":
                return proxy.getClass() + "";
            case "hashCode":
                return hashCode;
            case "equals":
                return false;
        }

        if (args != null) {
            byte[] serializedData = Py2JavaUtils.newNotifyRequest(FlatBuffersRequestType.ProxyInvoke, hashCode, "", "",
                    methodName, args);
            // byte[] unserializedData = SocketIPC.sendNotifyMessage(serializedData);

            LocalSocket localSocket = new LocalSocket();
            localSocket.connect(
                    new LocalSocketAddress(GlobalVariable.notifySocketPath, LocalSocketAddress.Namespace.FILESYSTEM));
            OutputStream os = localSocket.getOutputStream();
            InputStream is = localSocket.getInputStream();
            Utils.wrapperWriteAllBytes(os, serializedData);
            byte[] unserializedData = Utils.wrapperReadAllBytes(is);

            FlatBuffersResponse response = FlatBuffersResponse
                    .getRootAsFlatBuffersResponse(ByteBuffer.wrap(unserializedData));
            while (response.type() == FlatBuffersResponseType.ThreadOccupation) {
                SocketIPC.receiverStart(is, os);
                unserializedData = Utils.wrapperReadAllBytes(is);
                response = FlatBuffersResponse.getRootAsFlatBuffersResponse(ByteBuffer.wrap(unserializedData));
            }

            String exception = response.exception();
            if (exception != null && !exception.isEmpty()) {
                throw new RuntimeException(exception);
            }
            FlatBuffersJavaObject data = response.data();
            return Py2JavaUtils.fbToObject(data);
        }
        return null;
    }

    public static Object newProxy(String className) throws Exception {


        Class clazz5 = Class.forName(className);
        // 判断是否是接口
        if (!clazz5.isInterface()) {
            Object proxy = ProxyBuilder.forClass(clazz5)
                    .dexCache(GlobalVariable.dexmakerCacheDir)
                    .handler(new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Py2JavaUtils.LogD("socketIPC", "abs invoke: clazz5: " + method);
                            return interfaceUnifiedCallback(proxy,  method.getName(), args);
                        }
                    }).build();

            return proxy;

        } else {

            Py2JavaUtils.LogD("socketIPC", "execute: clazz5: " + clazz5);
            Object proxy = Proxy.newProxyInstance(
                    clazz5.getClassLoader(),
                    new Class<?>[]{clazz5},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {

                            return interfaceUnifiedCallback(proxy, method.getName(), args);
                        }
                    });

            Py2JavaUtils.LogD("socketIPC", "execute: proxy: " + proxy);
            if (proxy == null) {
                throw new RuntimeException("execute: new_proxy: " + className + " is null");
            }
            return proxy;
        }

    }

}
