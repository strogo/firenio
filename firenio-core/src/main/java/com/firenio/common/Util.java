/*
 * Copyright 2015 The FireNio Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.firenio.common;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.firenio.LifeCycle;
import com.firenio.Releasable;
import com.firenio.collection.IntMap;
import com.firenio.component.ChannelAcceptor;
import com.firenio.component.FastThreadLocal;
import com.firenio.log.Logger;
import com.firenio.log.LoggerFactory;

/**
 * @author wangkai
 */
public class Util {

    public static final  Charset  ASCII       = StandardCharsets.US_ASCII;
    public static final  Charset  GBK         = Charset.forName("GBK");
    public static final  Charset  UTF8        = StandardCharsets.UTF_8;
    private static final Logger   logger      = LoggerFactory.getLogger(Util.class);
    private static final String[] int_strings = new String[2048];

    static {
        for (int i = 0; i < int_strings.length; i++) {
            int_strings[i] = String.valueOf(i);
        }
    }

    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void close(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public static void clear(Collection<?> collection) {
        if (collection != null) {
            collection.clear();
        }
    }

    public static void clear(IntMap<byte[]> map) {
        if (map != null) {
            map.clear();
        }
    }

    public static void clear(Map<?, ?> map) {
        if (map != null) {
            map.clear();
        }
    }

    public static String int2String(int i) {
        if ((((i >>> 1) & (1 << 30)) | (i & 0x7fffffff)) < int_strings.length) {
            return int_strings[i];
        }
        return String.valueOf(i);
    }

    public static Thread exec(Runnable runnable, String name) {
        return exec(runnable, name, false);
    }

    public static Thread exec(Runnable runnable) {
        return exec(runnable, null);
    }

    public static Thread exec(Runnable runnable, String name, boolean daemon) {
        if (isNullOrBlank(name)) {
            name = "exec-" + randomUUID();
        }
        Thread t = new Thread(runnable, name);
        t.setDaemon(daemon);
        t.start();
        return t;
    }

    public static boolean getBooleanProperty(String key) {
        return getBooleanProperty(key, false);
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String v = System.getProperty(key);
        if (!isNullOrBlank(v)) {
            try {
                return isTrueValue(v);
            } catch (Throwable e) {
            }
        }
        return defaultValue;
    }

    public static Field getDeclaredField(Class<?> clazz, String name) {
        if (clazz == null) {
            return null;
        }
        Field[] fs = clazz.getDeclaredFields();
        for (Field f : fs) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    public static Field getDeclaredFieldFC(Class<?> clazz, String name) {
        Class<?> c = clazz;
        for (; ; ) {
            if (c == null) {
                return null;
            }
            Field f = getDeclaredField(c, name);
            if (f == null) {
                c = c.getSuperclass();
                continue;
            }
            return f;
        }
    }

    public static int getIntProperty(String key) {
        return getIntProperty(key, 0);
    }

    public static int getIntProperty(String key, int defaultValue) {
        String v = System.getProperty(key);
        if (!isNullOrBlank(v)) {
            try {
                return Integer.parseInt(v);
            } catch (Throwable e) {
            }
        }
        return defaultValue;
    }

    public static String getStringProperty(String key) {
        return getStringProperty(key, null);
    }

    public static String getStringProperty(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (!isNullOrBlank(v)) {
            return v;
        }
        return defaultValue;
    }

    public static String getValueFromArray(String[] args, int index) {
        return getValueFromArray(args, index, null);
    }

    public static String getValueFromArray(String[] args, int index, String defaultValue) {
        if (index < 0 || args == null) {
            return defaultValue;
        }
        if (index >= args.length) {
            return defaultValue;
        }
        return args[index];
    }

    public static Object getValueOfLast(Object target, String fieldName) {
        try {
            Object c = target;
            for (; ; ) {
                Field fieldNext = getDeclaredFieldFC(c.getClass(), fieldName);
                if (fieldNext == null) {
                    return c;
                }
                trySetAccessible(fieldNext);
                Object next = fieldNext.get(c);
                if (next == null) {
                    return c;
                }
                c = next;
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasLength(String text) {
        return text != null && text.length() > 0;
    }

    public static boolean hasText(String text) {
        return text != null && text.trim().length() > 0;
    }

    public static int indexOf(CharSequence sb, char ch) {
        return indexOf(sb, ch, 0);
    }

    public static int indexOf(CharSequence sb, char ch, int index) {
        int count = sb.length();
        for (int i = index; i < count; i++) {
            if (ch == sb.charAt(i)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.size() == 0;
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.size() == 0;
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNullOrBlank(String value) {
        return value == null || value.length() == 0;
    }

    public static boolean isTrueValue(String value) {
        return "true".equals(value) || "1".equals(value);
    }

    public static int lastIndexOf(CharSequence sb, char ch) {
        int count = sb.length();
        for (int i = count - 1; i > -1; i--) {
            if (ch == sb.charAt(i)) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(String str, char ch, int length) {
        int end = -1;
        if (str.length() > length) {
            end = str.length() - length - 1;
        }
        for (int i = str.length() - 1; i > end; i--) {
            if (str.charAt(i) == ch) {
                return i;
            }
        }
        return -1;
    }

    public static Object newInstance(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static long now() {
        return SysClock.now();
    }

    public static long now_f() {
        return SysClock.now_f();
    }

    public static long past(long start) {
        return now_f() - start;
    }

    public static void printArray(int[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i]);
            System.out.print(",");
        }
        System.out.println();
    }

    public static void printArray(Object[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i]);
            System.out.print(",");
        }
        System.out.println();
    }

    public static String randomLeastSignificantBits() {
        UUID   uuid  = UUID.randomUUID();
        byte[] array = new byte[8];
        ByteUtil.putLong(array, uuid.getLeastSignificantBits(), 0);
        return ByteUtil.getHexString(array);
    }

    public static String randomMostSignificantBits() {
        UUID   uuid  = UUID.randomUUID();
        byte[] array = new byte[8];
        ByteUtil.putLong(array, uuid.getMostSignificantBits(), 0);
        return ByteUtil.getHexString(array);
    }

    public static String randomUUID() {
        UUID   uuid  = UUID.randomUUID();
        byte[] array = new byte[16];
        ByteUtil.putLong(array, uuid.getMostSignificantBits(), 0);
        ByteUtil.putLong(array, uuid.getLeastSignificantBits(), 8);
        return ByteUtil.getHexString(array);
    }

    public static void releaseObject(Object releasable) {
        if (releasable instanceof Releasable) {
            try {
                ((Releasable) releasable).release();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static void release(Releasable releasable) {
        if (releasable != null) {
            try {
                releasable.release();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static void setObjectValue(Object target, Object value, String fieldName) {
        try {
            Field field = getDeclaredFieldFC(target.getClass(), fieldName);
            if (field == null) {
                throw new NoSuchFieldException(fieldName);
            }
            trySetAccessible(field);
            field.set(target, value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void setSystemPropertiesIfNull(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    public static void setValueOfLast(Object target, Object value, String fieldName) {
        Object last = getValueOfLast(target, fieldName);
        setObjectValue(last, value, fieldName);
    }

    public static int skip(CharSequence sb, char ch) {
        return skip(sb, ch, 0);
    }

    public static int skip(CharSequence sb, char ch, int index) {
        int count = sb.length();
        for (int i = index; i < count; i++) {
            if (ch != sb.charAt(i)) {
                return i;
            }
        }
        return -1;
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static String stackTraceToString(Throwable cause) {
        return stackTraceToString(cause, false);
    }

    public static String stackTraceToString(Throwable cause, boolean lineFeed) {
        return stackTraceToString(cause, lineFeed, FastThreadLocal.get().getStringBuilder());
    }

    public static String stackTraceToString(Throwable cause, boolean lineFeed, StringBuilder sb) {
        if (lineFeed) {
            sb.append('\n');
        }
        sb.append(cause.getClass().getName());
        sb.append(':');
        sb.append(' ');
        sb.append(cause.getMessage());
        StackTraceElement[] sts = cause.getStackTrace();
        for (StackTraceElement st : sts) {
            sb.append("\n\tat ");
            sb.append(st.getClassName());
            sb.append('.');
            sb.append(st.getMethodName());
            sb.append('(');
            {
                if (st.isNativeMethod()) {
                    sb.append("Native Method");
                } else {
                    if (st.getFileName() != null) {
                        if (st.getLineNumber() > 0) {
                            sb.append(st.getFileName());
                            sb.append(':');
                            sb.append(int2String(st.getLineNumber()));
                        } else {
                            sb.append(st.getFileName());
                        }
                    } else {
                        sb.append("Unknown Source");
                    }
                }
            }
            sb.append(')');
        }
        if (cause.getCause() != null) {
            sb.append("\nCause By: ");
            return stackTraceToString(cause.getCause(), false, sb);
        }
        return sb.toString();
    }

    public static void start(LifeCycle lifeCycle) throws Exception {
        if (lifeCycle != null && !lifeCycle.isRunning()) {
            lifeCycle.start();
        }
    }

    public static void stop(LifeCycle lifeCycle) {
        if (lifeCycle != null) {
            try {
                lifeCycle.stop();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static List<String> toList(int initialListSize, String... protocols) {
        if (protocols == null) {
            return null;
        }
        List<String> result = new ArrayList<>(initialListSize);
        for (String p : protocols) {
            if (p == null || p.isEmpty()) {
                throw new IllegalArgumentException("protocol cannot be null or empty");
            }
            result.add(p);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("protocols cannot empty");
        }
        return result;
    }

    public static List<String> toList(String... protocols) {
        return toList(16, protocols);
    }

    public static Throwable trySetAccessible(AccessibleObject object) {
        try {
            object.setAccessible(true);
            return null;
        } catch (Throwable e) {
            return e;
        }
    }

    public static void unbind(ChannelAcceptor unbindable) {
        if (unbindable != null) {
            try {
                unbindable.unbind();
            } catch (Throwable e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public static <T extends Throwable> T unknownStackTrace(T cause, Class<?> clazz, String method) {
        cause.setStackTrace(new StackTraceElement[]{new StackTraceElement(clazz.getName(), method, null, -1)});
        return cause;
    }

    public static int valueOf(int value, byte[] data) {
        int v = value;
        for (int i = data.length - 1; i > -1; i--) {
            data[i] = (byte) ((v % 10) + ((byte) '0'));
            v = v / 10;
            if (v == 0) {
                return i;
            }
        }
        return -1;
    }

    public static void wait(Object o) {
        wait(o, 0);
    }

    public static void wait(Object o, long timeout) {
        try {
            o.wait(timeout);
        } catch (InterruptedException e) {
        }
    }

    public static int clothCover(int v) {
        int n = 2;
        for (; n < v; )
            n <<= 1;
        return n;
    }

}
