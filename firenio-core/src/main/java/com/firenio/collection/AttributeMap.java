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
package com.firenio.collection;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * use for static variable, eg:
 * <pre>
 * static final AttributeKey KEY_NAME = AttributeMap.valueOfKey(Channel.class, "KEY_NAME");
 * <pre/>
 */
public abstract class AttributeMap {

    private static final Object[]                  EMPTY_ATTRIBUTES = new Object[0];
    private static final int                       DEFAULT_CAP      = 4;
    private static final Map<Class, AttributeKeys> INDEX_MAPPING    = new ConcurrentHashMap<>();

    private final Object[] attributes;

    public AttributeMap() {
        AttributeKeys keys = getKeys();
        if (keys == null) {
            attributes = null;
            return;
        }
        keys.initialized = true;
        int keys_count = keys.index_counter.get();
        if (keys_count == 0) {
            attributes = null;
        } else {
            this.attributes = new Object[keys_count];
            for (AttributeKey key : keys.keys.values()) {
                AttributeInitFunction function = key.getFunction();
                if (function != null) {
                    setValue(key, function.initialize());
                }
            }
        }
    }

    public <T> T getValue(AttributeKey<T> key) {
        return (T) attributes[key.getIndex()];
    }

    public void setValue(AttributeKey key, Object value) {
        attributes[key.getIndex()] = value;
    }

    //        public <T> T getValue(AttributeKey<T> key) {
    //        int      index      = key.getIndex();
    //        Object[] attributes = this.attributes;
    //        if (index < attributes.length) {
    //            return (T) attributes[index];
    //        } else {
    //            return null;
    //        }
    //    }
    //
    //    public void setValue(AttributeKey key, Object value) {
    //        int      index      = key.getIndex();
    //        Object[] attributes = this.attributes;
    //        if (index < attributes.length) {
    //            attributes[index] = value;
    //        } else {
    //            expand_and_set(index, value);
    //        }
    //    }
    //
    //    private void expand_and_set(int index, Object value) {
    //        int new_cap = Math.max(DEFAULT_CAP, index + 1);
    //        this.attributes = Arrays.copyOf(attributes, new_cap);
    //        this.attributes[index] = value;
    //    }

    public static AttributeKeys getKeys(Class clazz) {
        return INDEX_MAPPING.get(clazz);
    }

    public static AttributeKey valueOfKey(Class clazz, String name) {
        return valueOfKey(clazz, name, null);
    }

    public static AttributeKey valueOfKey(Class clazz, String name, AttributeInitFunction function) {
        AttributeKeys attributeKeys = INDEX_MAPPING.get(clazz);
        if (attributeKeys == null) {
            synchronized (INDEX_MAPPING) {
                attributeKeys = INDEX_MAPPING.get(clazz);
                if (attributeKeys == null) {
                    attributeKeys = new AttributeKeys();
                    INDEX_MAPPING.put(clazz, attributeKeys);
                }
            }
        }
        return attributeKeys.valueOf(name, function);
    }

    protected abstract AttributeKeys getKeys();

    public static class AttributeKeys {

        final    AtomicInteger             index_counter = new AtomicInteger();
        final    Map<String, AttributeKey> keys          = new ConcurrentHashMap<>();
        final    Map<String, AttributeKey> ro_keys       = Collections.unmodifiableMap(keys);
        volatile boolean                   initialized   = false;

        AttributeKey valueOf(String name, AttributeInitFunction function) {
            if (initialized) {
                throw new RuntimeException("incorrect usage, AttributeKey can only be static constant");
            }
            AttributeKey key = keys.get(name);
            if (key == null) {
                synchronized (keys) {
                    key = keys.get(name);
                    if (key == null) {
                        int index = index_counter.getAndIncrement();
                        key = new AttributeKey(index, name, function);
                        keys.put(name, key);
                    }
                }
            }
            return key;
        }

        public Map<String, AttributeKey> getKeys() {
            return ro_keys;
        }

    }

    public interface AttributeInitFunction<T> {

        T initialize();

    }


}
