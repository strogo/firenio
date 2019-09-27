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
package com.firenio.codec.http11;

import static com.firenio.codec.http11.HttpHeader.Content_Length;
import static com.firenio.common.ByteUtil.b;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.firenio.buffer.ByteBuf;
import com.firenio.collection.IntMap;
import com.firenio.common.Util;
import com.firenio.component.Channel;
import com.firenio.component.FastThreadLocal;
import com.firenio.component.Frame;

/**
 * @author wangkai
 */
public class ClientHttpCodec extends HttpCodec {

    private static final byte[] PROTOCOL = b(" HTTP/1.1\r\nContent-Length: ");

    @Override
    ClientHttpFrame new_frame() {
        return new ClientHttpFrame();
    }

    @Override
    int decode_remain_body(Channel ch, ByteBuf src, HttpFrame frame) {
        ClientHttpFrame f = (ClientHttpFrame) frame;
        if (f.isChunked()) {
            //TODO chunked support
            return decode_state_complete;
        } else {
            return super.decode_remain_body(ch, src, f);
        }
    }

    @Override
    public ByteBuf encode(Channel ch, Frame frame) {
        ClientHttpFrame f            = (ClientHttpFrame) frame;
        Object          content      = f.getContent();
        ByteBuf         contentBuf   = null;
        byte[]          contentArray = null;
        boolean         isArray      = false;
        int             write_size   = 0;
        if (content instanceof ByteBuf) {
            contentBuf = ((ByteBuf) content);
            write_size = contentBuf.writeIndex();
        } else if (content instanceof byte[]) {
            isArray = true;
            contentArray = (byte[]) content;
            write_size = contentArray.length;
        }
        byte[]         byte32      = FastThreadLocal.get().getBytes32();
        byte[]         url_bytes   = getRequestURI(f).getBytes();
        byte[]         mtd_bytes   = f.getMethod().getBytes();
        int            len_idx     = Util.valueOf(write_size, byte32);
        int            len_len     = 32 - len_idx;
        int            len         = mtd_bytes.length + 1 + url_bytes.length + PROTOCOL.length + len_len + 2;
        int            header_size = 0;
        List<byte[]>   bytes_array = (List<byte[]>) FastThreadLocal.get().getList();
        IntMap<String> headers     = f.getRequestHeaders();
        if (headers != null) {
            headers.remove(HttpHeader.Content_Length.getId());
            for (headers.scan(); headers.hasNext(); ) {
                byte[] k = HttpHeader.get(headers.key()).getBytes();
                byte[] v = headers.value().getBytes();
                if (v == null) {
                    continue;
                }
                header_size++;
                bytes_array.add(k);
                bytes_array.add(v);
                len += 4;
                len += k.length;
                len += v.length;
            }
            headers.finishScan();
        }
        len += 2;
        if (isArray) {
            len += write_size;
        }
        ByteBuf buf = ch.alloc().allocate(len);
        buf.writeBytes(mtd_bytes);
        buf.writeByte(SPACE);
        buf.writeBytes(url_bytes);
        buf.writeBytes(PROTOCOL);
        buf.writeBytes(byte32, len_idx, len_len);
        buf.writeByte(R);
        buf.writeByte(N);
        int j = 0;
        for (int i = 0; i < header_size; i++) {
            buf.writeBytes(bytes_array.get(j++));
            buf.writeByte((byte) ':');
            buf.writeByte(SPACE);
            buf.writeBytes(bytes_array.get(j++));
            buf.writeByte(R);
            buf.writeByte(N);
        }
        buf.writeByte(R);
        buf.writeByte(N);
        if (write_size > 0) {
            if (isArray) {
                buf.writeBytes(contentArray);
            } else {
                ch.write(buf);
                ch.write(contentBuf);
                return null;
            }
        }
        return buf;
    }

    private String getRequestURI(HttpFrame frame) {
        Map<String, String> params = frame.getRequestParams();
        if (params == null || params.isEmpty()) {
            return frame.getRequestURL();
        }
        String        url = frame.getRequestURL();
        StringBuilder u   = new StringBuilder(url);
        u.append("?");
        Set<Entry<String, String>> ps = params.entrySet();
        for (Entry<String, String> p : ps) {
            u.append(p.getKey());
            u.append("=");
            u.append(p.getValue());
            u.append("&");
        }
        return u.toString();
    }

    int header_complete(HttpFrame frame) throws IOException {
        ClientHttpFrame f         = (ClientHttpFrame) frame;
        int             c_len     = 0;
        String          c_len_str = f.getResponse(Content_Length);
        if (!Util.isNullOrBlank(c_len_str)) {
            c_len = Integer.parseInt(c_len_str);
            f.setContentLength(c_len);
        }
        if (c_len < 1) {
            if (f.isChunked()) {
                return decode_state_body;
            } else {
                return decode_state_complete;
            }
        } else {
            if (c_len > getBodyLimit()) {
                throw OVER_LIMIT;
            }
            return decode_state_body;
        }
    }

    @Override
    protected void parse_line_one(HttpFrame f, CharSequence line) {
        int index  = Util.indexOf(line, ' ');
        int status = Integer.parseInt((String) line.subSequence(index + 1, index + 4));
        f.setStatus(HttpStatus.get(status));
    }

}
