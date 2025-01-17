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
package test.io.lenthvalue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.firenio.Options;
import com.firenio.codec.lengthvalue.LengthValueCodec;
import com.firenio.codec.lengthvalue.LengthValueFrame;
import com.firenio.common.Util;
import com.firenio.component.Channel;
import com.firenio.component.ChannelAcceptor;
import com.firenio.component.ChannelConnector;
import com.firenio.component.ChannelEventListenerAdapter;
import com.firenio.component.Frame;
import com.firenio.component.IoEventHandle;
import com.firenio.component.LoggerChannelOpenListener;
import com.firenio.component.SocketOptions;
import com.firenio.concurrent.Waiter;

import junit.framework.Assert;

public class TestLengthValueServerJunit {

    static final String hello = "hello server!";
    static final String res   = "yes server already accept your message:";

    static {
        Options.setEnableEpoll(true);
    }

    ChannelAcceptor context = new ChannelAcceptor(8300);

    static void v(String r) {
        Assert.assertEquals(r, res + hello);
    }

    @After
    public void clean() {
        Util.unbind(context);
    }

    @Before
    public void server() throws Exception {
        IoEventHandle eventHandle = new IoEventHandle() {
            @Override
            public void accept(Channel ch, Frame f) throws Exception {
                String text = f.getStringContent();
                f.setContent(ch.allocate());
                f.write("yes server already accept your message:", ch);
                f.write(text, ch);
                ch.writeAndFlush(f);
            }
        };
        context.addChannelEventListener(new LoggerChannelOpenListener());
        context.setIoEventHandle(eventHandle);
        context.addProtocolCodec(new LengthValueCodec());
        context.addChannelEventListener(new ChannelEventListenerAdapter() {

            @Override
            public void channelOpened(Channel ch) throws Exception {
                System.out.println(ch.getOption(SocketOptions.TCP_NODELAY));
                System.out.println(ch.getOption(SocketOptions.SO_RCVBUF));
                ch.setOption(SocketOptions.TCP_NODELAY, 1);
                ch.setOption(SocketOptions.SO_RCVBUF, 1028);
                System.out.println(ch.getOption(SocketOptions.TCP_NODELAY));
                System.out.println(ch.getOption(SocketOptions.SO_RCVBUF));
            }

        });
        context.bind();
    }

    @Test
    public void test() throws Exception {
        testClient();
        testClientAsync();
    }

    public void testClient() throws Exception {
        Waiter<String>   w       = new Waiter<>();
        ChannelConnector context = new ChannelConnector(8300);
        IoEventHandle eventHandle = new IoEventHandle() {
            @Override
            public void accept(Channel ch, Frame f) throws Exception {
                System.out.println();
                System.out.println("____________________" + f.getStringContent());
                System.out.println();
                context.close();
                w.call(f.getStringContent(), null);
            }
        };

        context.setIoEventHandle(eventHandle);
        context.addChannelEventListener(new LoggerChannelOpenListener());
        context.addProtocolCodec(new LengthValueCodec());
        Channel          ch    = context.connect();
        LengthValueFrame f = new LengthValueFrame();
        f.setString(hello, ch);
        ch.writeAndFlush(f);
        w.await(1000);
        v(w.getResponse());
    }

    public void testClientAsync() throws Exception {
        Waiter<String>   w       = new Waiter<>();
        ChannelConnector context = new ChannelConnector(8300);
        IoEventHandle eventHandle = new IoEventHandle() {
            @Override
            public void accept(Channel ch, Frame f) throws Exception {
                System.out.println();
                System.out.println("____________________" + f.getStringContent());
                System.out.println();
                context.close();
                w.call(f.getStringContent(), null);
            }
        };

        context.setIoEventHandle(eventHandle);
        context.addChannelEventListener(new LoggerChannelOpenListener());
        context.addProtocolCodec(new LengthValueCodec());
        context.connect((ch, ex) -> {
            LengthValueFrame f = new LengthValueFrame();
            f.setString(hello, ch);
            try {
                ch.writeAndFlush(f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        w.await(1000);
        v(w.getResponse());
    }

}
