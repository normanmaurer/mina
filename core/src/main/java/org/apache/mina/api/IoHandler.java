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
package org.apache.mina.api;

public interface IoHandler {

    /**
     * Invoked when a connection has been opened.
     * 
     * @param session {@link IoSession} associated with the invocation
     */
    void sessionOpened(IoSession session);

    /**
     * Invoked when a connection is closed.
     * 
     * @param session {@link IoSession} associated with the invocation
     */
    void sessionClosed(IoSession session);

    /**
     * Invoked with the related {@link IdleStatus} when a connection becomes idle.
     * 
     * @param session {@link IoSession} associated with the invocation
     */
    void sessionIdle(IoSession session, IdleStatus status);

    /**
     * Invoked when a message is received.
     * 
     * @param session {@link IoSession} associated with the invocation
     * @param message the incoming message to process
     */
    void messageReceived(IoSession session, Object message);

    /**
     * Invoked when a high level message was written to the low level O/S buffer.
     * 
     * @param session {@link IoSession} associated with the invocation
     * @param message the incoming message to process
     */
    void messageSent(IoSession session, Object message);

    /**
     * Invoked when a new service is activated by an {@link IoService}.
     * 
     * @param service the {@link IoService}
     */
    void serviceActivated(IoService service);

    /**
     * Invoked when a service is inactivated by an {@link IoService}.
     * 
     * @param service the {@link IoService}
     */
    void serviceInactivated(IoService service);

    /**
     * Invoked when any runtime exception is thrown during session processing (filters, unexpected error, etc..).
     * 
     * @param session the session related to the exception
     * @param cause the caught exception
     */
    void exceptionCaught(IoSession session, Exception cause);
}
