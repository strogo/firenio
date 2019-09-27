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
package test.io.load.http11;

import java.io.IOException;
import java.util.Arrays;

import com.firenio.Options;
import com.firenio.buffer.ByteBuf;
import com.firenio.codec.http11.HttpCodec;
import com.firenio.codec.http11.HttpConnection;
import com.firenio.codec.http11.HttpContentType;
import com.firenio.codec.http11.HttpDateUtil;
import com.firenio.codec.http11.HttpFrame;
import com.firenio.codec.http11.HttpStatus;
import com.firenio.collection.AttributeKey;
import com.firenio.collection.AttributeMap;
import com.firenio.collection.ByteTree;
import com.firenio.common.Util;
import com.firenio.component.Channel;
import com.firenio.component.ChannelAcceptor;
import com.firenio.component.ChannelEventListener;
import com.firenio.component.ChannelEventListenerAdapter;
import com.firenio.component.FastThreadLocal;
import com.firenio.component.Frame;
import com.firenio.component.IoEventHandle;
import com.firenio.component.NioEventLoopGroup;
import com.firenio.component.ProtocolCodec;
import com.firenio.component.SocketOptions;
import com.firenio.log.DebugUtil;
import com.firenio.log.LoggerFactory;
import com.jsoniter.output.JsonStream;
import com.jsoniter.output.JsonStreamPool;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.Slice;

/**
 * @author wangkai
 */
public class TestHttpLoadServerTFB {

    static final AttributeKey<ByteBuf> JSON_BUF         = newByteBufKey();
    static final byte[]                STATIC_PLAINTEXT = "Hello, World!".getBytes();

    public static void main(String[] args) throws Exception {
        boolean lite       = Util.getBooleanProperty("lite");
        boolean read       = Util.getBooleanProperty("read");
        boolean pool       = true;
        boolean direct     = true;
        boolean inline     = true;
        boolean print_open = false;
        int     core       = Util.getIntProperty("core", 1);
        int     frame      = Util.getIntProperty("frame", 16);
        int     level      = Util.getIntProperty("level", 1);
        int     readBuf    = Util.getIntProperty("readBuf", 16);
        LoggerFactory.setEnableSLF4JLogger(false);
        LoggerFactory.setLogLevel(LoggerFactory.LEVEL_INFO);
        Options.setDebugError(false);
        Options.setChannelReadFirst(read);
        Options.setBufAutoExpansion(false);
        Options.setEnableEpoll(true);
        Options.setEnableUnsafeBuf(true);
        Options.setSysClockStep(0);
        DebugUtil.info("lite: {}", lite);
        DebugUtil.info("read: {}", read);
        DebugUtil.info("pool: {}", pool);
        DebugUtil.info("core: {}", core);
        DebugUtil.info("frame: {}", frame);
        DebugUtil.info("level: {}", level);
        DebugUtil.info("direct: {}", direct);
        DebugUtil.info("readBuf: {}", readBuf);

        HttpDateUtil.start();

        IoEventHandle eventHandle = new IoEventHandle() {

            @Override
            public void accept(Channel ch, Frame frame) throws Exception {
                HttpFrame f      = (HttpFrame) frame;
                String    action = f.getRequestURL();
                if ("/plaintext".equals(action)) {
                    f.setContent(STATIC_PLAINTEXT);
                    f.setContentType(HttpContentType.text_plain);
                    f.setConnection(HttpConnection.NONE);
                } else if ("/json".equals(action)) {
                    ByteBuf    temp   = FastThreadLocal.get().getValue(JSON_BUF);
                    JsonStream stream = JsonStreamPool.borrowJsonStream();
                    try {
                        stream.reset(null);
                        stream.writeVal(Message.class, new Message("Hello, World!"));
                        Slice slice = stream.buffer();
                        temp.reset(slice.data(), slice.head(), slice.tail());
                        f.setContent(temp);
                        f.setContentType(HttpContentType.application_json);
                        f.setConnection(HttpConnection.NONE);
                        f.setDate(HttpDateUtil.getDateLine());
                        ch.writeAndFlush(f);
                        ch.release(f);
                    } catch (IOException e) {
                        throw new JsonException(e);
                    } finally {
                        JsonStreamPool.returnJsonStream(stream);
                    }
                    return;
                } else {
                    System.err.println("404");
                    f.setString("404,page not found!", ch);
                    f.setContentType(HttpContentType.text_plain);
                    f.setStatus(HttpStatus.C404);
                }
                f.setDate(HttpDateUtil.getDateLine());
                ch.writeAndFlush(f);
                ch.release(f);
            }

        };

        int fcache    = 1024 * 16;
        int pool_cap  = 1024 * 128;
        int pool_unit = 256;
        if (inline) {
            pool_cap = 1024 * 8;
            pool_unit = 256 * 16;
        }
        ByteTree cachedUrls = new ByteTree();
        cachedUrls.add("/plaintext");
        cachedUrls.add("/json");
        ProtocolCodec     codec   = new HttpCodec("firenio", fcache, lite, inline, cachedUrls);
        NioEventLoopGroup group   = new NioEventLoopGroup();
        ChannelAcceptor   context = new ChannelAcceptor(group, 8080);
        group.setMemoryPoolCapacity(pool_cap);
        group.setEnableMemoryPoolDirect(direct);
        group.setEnableMemoryPool(pool);
        group.setMemoryPoolUnit(pool_unit);
        group.setWriteBuffers(32);
        group.setEventLoopSize(Util.availableProcessors() * core);
        group.setConcurrentFrameStack(false);
        context.addProtocolCodec(codec);
        if (print_open) {
            context.addChannelEventListener(new ChannelEventListener() {

                @Override
                public void channelOpened(Channel ch) {
                    System.out.println("open " + Util.now_f());
                }

                @Override
                public void channelClosed(Channel ch) {
                    System.out.println("close " + Util.now_f());
                }
            });
        }
        context.addChannelEventListener(new ChannelEventListenerAdapter() {

            @Override
            public void channelOpened(Channel ch) throws Exception {
                ch.setOption(SocketOptions.TCP_NODELAY, 1);
                ch.setOption(SocketOptions.SO_KEEPALIVE, 0);
            }
        });
        context.setIoEventHandle(eventHandle);
        context.bind();
    }

    private static byte[] serializeMsg(Message obj) {
        JsonStream stream = JsonStreamPool.borrowJsonStream();
        try {
            stream.reset(null);
            stream.writeVal(Message.class, obj);
            Slice slice = stream.buffer();
            return Arrays.copyOfRange(slice.data(), 0, slice.tail());
        } catch (IOException e) {
            throw new JsonException(e);
        } finally {
            JsonStreamPool.returnJsonStream(stream);
        }
    }

    static AttributeKey<ByteBuf> newByteBufKey() {
        return AttributeMap.valueOfKey(FastThreadLocal.class, "JSON_BUF", () -> {
            return ByteBuf.heap(0);
        });
    }

    static class Message {

        private final String message;

        public Message(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

    }

}
