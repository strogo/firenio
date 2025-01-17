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
package test.others.algorithm;

/**
 * @author wangkai
 */
public class ByteArray {

    private byte[] data;

    private int length;

    private int off;

    public ByteArray() {}

    public ByteArray(byte[] data, int off, int length) {
        this.data = data;
        this.off = off;
        this.length = length;
    }

    private static boolean greater(ByteArray b1, ByteArray b2) {
        int len = b1.getLength();
        for (int i = 0; i < len; i++) {
            byte b11 = b1.getByte(i);
            byte b22 = b2.getByte(i);
            if (b11 != b22) {
                return b11 > b22;
            }
        }
        return false;
    }

    private static boolean greaterOrEquals(ByteArray b1, ByteArray b2) {
        int len = b1.getLength();
        for (int i = 0; i < len; i++) {
            byte b11 = b1.getByte(i);
            byte b22 = b2.getByte(i);
            if (b11 != b22) {
                return b11 > b22;
            }
        }
        return true;
    }

    private static boolean lessOrEquals(ByteArray b1, ByteArray b2) {
        int len = b1.getLength();
        for (int i = 0; i < len; i++) {
            byte b11 = b1.getByte(i);
            byte b22 = b2.getByte(i);
            if (b11 != b22) {
                return b11 < b22;
            }
        }
        return true;
    }

    public byte getByte(int pos) {
        return data[ix(pos)];
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return length;
    }

    public int getOff() {
        return off;
    }

    public boolean greater(ByteArray o) {
        if (getLength() > o.getLength()) {
            return true;
        }
        if (getLength() < o.getLength()) {
            return false;
        }
        return greater(this, o);
    }

    public boolean greaterOrEquals(ByteArray o) {
        if (getLength() > o.getLength()) {
            return true;
        }
        if (getLength() < o.getLength()) {
            return false;
        }
        return greaterOrEquals(this, o);
    }

    private int ix(int pos) {
        return off + pos;
    }

    public boolean lessOrEquals(ByteArray o) {
        if (getLength() > o.getLength()) {
            return false;
        }
        if (getLength() < o.getLength()) {
            return true;
        }
        return lessOrEquals(this, o);
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setOff(int off) {
        this.off = off;
    }

    public String toString1() {
        return new String(data, off, length);
    }
}
