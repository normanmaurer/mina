/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.transport.tcp;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.mina.api.AbstractIoFilter;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.filterchain.ReadFilterChainController;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.nio.tcp.NioTcpClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class test the event dispatching of {@link NioTcpClient}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class NioTcpClientFilterEventTest {

    private static final Logger LOG = LoggerFactory.getLogger(NioTcpClientFilterEventTest.class);

    private static final int CLIENT_COUNT = 10;

    private static final int WAIT_TIME = 30000;

    private final CountDownLatch msgSentLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch msgReadLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch openLatch = new CountDownLatch(CLIENT_COUNT);

    private final CountDownLatch closedLatch = new CountDownLatch(CLIENT_COUNT);

    /**
     * Create an old IO server and use a bunch of MINA client on it. Test if the events occurs correctly in the
     * different IoFilters.
     */
    @Test
    public void generate_all_kind_of_client_event() throws IOException, InterruptedException, ExecutionException {
        NioTcpClient client = new NioTcpClient();
        client.setFilters(new MyCodec(), new Handler());

        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(null);
        int port = serverSocket.getLocalPort();

        // warm up
        Thread.sleep(100);
        final long t0 = System.currentTimeMillis();

        // now connect the clients

        List<IoFuture<IoSession>> cf = new ArrayList<IoFuture<IoSession>>();
        for (int i = 0; i < CLIENT_COUNT; i++) {
            cf.add(client.connect(new InetSocketAddress("localhost", port)));
        }

        Socket[] clientSockets = new Socket[CLIENT_COUNT];
        for (int i = 0; i < CLIENT_COUNT; i++) {
            clientSockets[i] = serverSocket.accept();
        }

        // does the session open message was fired ?
        assertTrue(openLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // gather sessions from futures
        IoSession[] sessions = new IoSession[CLIENT_COUNT];
        for (int i = 0; i < CLIENT_COUNT; i++) {
            sessions[i] = cf.get(i).get();
            assertNotNull(sessions[i]);
        }

        // write some messages
        for (int i = 0; i < CLIENT_COUNT; i++) {
            clientSockets[i].getOutputStream().write(("test:" + i).getBytes());
            clientSockets[i].getOutputStream().flush();
        }

        // test is message was received by the server
        assertTrue(msgReadLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // does response was wrote and sent ?
        assertTrue(msgSentLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        // read the echos
        final byte[] buffer = new byte[1024];

        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int bytes = clientSockets[i].getInputStream().read(buffer);
            final String text = new String(buffer, 0, bytes);
            assertEquals("test:" + i, text);
        }

        // close the session
        assertEquals(CLIENT_COUNT, closedLatch.getCount());
        for (int i = 0; i < CLIENT_COUNT; i++) {
            clientSockets[i].close();
        }

        // does the session close event was fired ?
        assertTrue(closedLatch.await(WAIT_TIME, TimeUnit.MILLISECONDS));

        long t1 = System.currentTimeMillis();

        System.out.println("Delta = " + (t1 - t0));

        serverSocket.close();
    }

    private class MyCodec extends AbstractIoFilter {

        @Override
        public void messageReceived(final IoSession session, final Object message,
                final ReadFilterChainController controller) {
            if (message instanceof ByteBuffer) {
                final ByteBuffer in = (ByteBuffer) message;
                final byte[] buffer = new byte[in.remaining()];
                in.get(buffer);
                controller.callReadNextFilter(new String(buffer));
            } else {
                fail();
            }
        }

        @Override
        public void messageWriting(IoSession session, WriteRequest writeRequest, WriteFilterChainController controller) {
            writeRequest.setMessage(ByteBuffer.wrap(writeRequest.getMessage().toString().getBytes()));
            controller.callWriteNextFilter(writeRequest);
        }
    }

    private class Handler extends AbstractIoFilter {

        @Override
        public void sessionOpened(final IoSession session) {
            LOG.info("** session open");
            openLatch.countDown();
        }

        @Override
        public void sessionClosed(final IoSession session) {
            LOG.info("** session closed");
            closedLatch.countDown();
        }

        @Override
        public void messageReceived(final IoSession session, final Object message,
                final ReadFilterChainController controller) {
            LOG.info("** message received {}", message);
            msgReadLatch.countDown();
            session.write(message.toString());
        }

        @Override
        public void messageSent(final IoSession session, final Object message) {
            LOG.info("** message sent {}", message);
            msgSentLatch.countDown();
        }
    }
}
