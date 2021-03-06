/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.jaxrs.websocket;

import java.util.List;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerWebSocketTest extends AbstractBusClientServerTestBase {
    private static final String PORT = BookServerWebSocket.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(new BookServerWebSocket()));
        createStaticBus();
    }
    
    @Test
    public void testBookWithWebSocket() throws Exception {
        String address = "ws://localhost:" + getPort() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address, 1);
        wsclient.connect();
        try {
            // call the GET service
            wsclient.sendMessage("GET /websocket/web/bookstore/booknames".getBytes());
            assertTrue("one book must be returned", wsclient.await(3));
            List<Object> received = wsclient.getReceived();
            assertEquals(1, received.size());
            Response resp = new Response(received.get(0));
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            String value = resp.getTextEntity();
            assertEquals("CXF in Action", value);
            
            // call the same GET service in the text mode
            wsclient.reset(1);
            wsclient.sendTextMessage("GET /websocket/web/bookstore/booknames");
            assertTrue("one book must be returned", wsclient.await(3));
            received = wsclient.getReceived();
            assertEquals(1, received.size());
            resp = new Response(received.get(0));
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            value = resp.getTextEntity();
            assertEquals("CXF in Action", value);

            // call another GET service
            wsclient.reset(1);
            wsclient.sendMessage("GET /websocket/web/bookstore/books/123".getBytes());
            assertTrue("response expected", wsclient.await(3));
            received = wsclient.getReceived();
            resp = new Response(received.get(0));
            assertEquals(200, resp.getStatusCode());
            assertEquals("application/xml", resp.getContentType());
            value = resp.getTextEntity();
            assertTrue(value.startsWith("<?xml ") && value.endsWith("</Book>"));
            
            // call the POST service
            wsclient.reset(1);
            wsclient.sendMessage(
                "POST /websocket/web/bookstore/booksplain\r\nContent-Type: text/plain\r\n\r\n123"
                    .getBytes());
            assertTrue("response expected", wsclient.await(3));
            received = wsclient.getReceived();
            resp = new Response(received.get(0));
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            value = resp.getTextEntity();
            assertEquals("123", value);
            
            // call the same POST service in the text mode 
            wsclient.reset(1);
            wsclient.sendTextMessage(
                "POST /websocket/web/bookstore/booksplain\r\nContent-Type: text/plain\r\n\r\n123");
            assertTrue("response expected", wsclient.await(3));
            received = wsclient.getReceived();
            resp = new Response(received.get(0));
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            value = resp.getTextEntity();
            assertEquals("123", value);

            // call the GET service returning a continous stream output
            wsclient.reset(6);
            wsclient.sendMessage("GET /websocket/web/bookstore/bookbought".getBytes());
            assertTrue("response expected", wsclient.await(5));
            received = wsclient.getReceived();
            assertEquals(6, received.size());
            resp = new Response(received.get(0));
            assertEquals(200, resp.getStatusCode());
            assertEquals("application/octet-stream", resp.getContentType());
            value = resp.getTextEntity();
            assertTrue(value.startsWith("Today:"));
            for (int r = 2, i = 1; i < 6; r *= 2, i++) {
                // subsequent data should not carry the headers nor the status.
                resp = new Response(received.get(i));
                assertEquals(0, resp.getStatusCode());
                assertEquals(r, Integer.parseInt(resp.getTextEntity()));
            }
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testBookWithWebSocketAndHTTP() throws Exception {
        String address = "ws://localhost:" + getPort() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address, 1);
        wsclient.connect();
        try {
            // call the GET service
            wsclient.sendMessage("GET /websocket/web/bookstore/booknames".getBytes());
            assertTrue("one book must be returned", wsclient.await(3));
            List<Object> received = wsclient.getReceived();
            assertEquals(1, received.size());
            Response resp = new Response(received.get(0));
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            String value = resp.getTextEntity();
            assertEquals("CXF in Action", value);
           
            testGetBookHTTPFromWebSocketEndpoint();
            
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testGetBookHTTPFromWebSocketEndpoint() throws Exception {
        String address = "http://localhost:" + getPort() + "/websocket/web/bookstore/books/1";
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        Book book = wc.get(Book.class);
        assertEquals(1L, book.getId());
    }
    
    @Test
    public void testBookWithWebSocketServletStream() throws Exception {
        String address = "ws://localhost:" + getPort() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address, 1);
        wsclient.connect();
        try {
            wsclient.sendMessage("GET /websocket/web/bookstore/booknames/servletstream".getBytes());
            assertTrue("one book must be returned", wsclient.await(3));
            List<Object> received = wsclient.getReceived();
            assertEquals(1, received.size());
            Response resp = new Response(received.get(0));
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            String value = resp.getTextEntity();
            assertEquals("CXF in Action", value);
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testWrongMethod() throws Exception {
        String address = "ws://localhost:" + getPort() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address, 1);
        wsclient.connect();
        try {
            // call the GET service using POST
            wsclient.sendMessage("POST /websocket/web/bookstore/booknames".getBytes());
            assertTrue("error response expected", wsclient.await(3));
            List<Object> received = wsclient.getReceived();
            assertEquals(1, received.size());
            Response resp = new Response(received.get(0));
            assertEquals(405, resp.getStatusCode());
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testPathRestriction() throws Exception {
        String address = "ws://localhost:" + getPort() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address, 1);
        wsclient.connect();
        try {
            // call the GET service over the different path
            wsclient.sendMessage("GET /websocket/bookstore2".getBytes());
            assertTrue("error response expected", wsclient.await(3));
            List<Object> received = wsclient.getReceived();
            assertEquals(1, received.size());
            Response resp = new Response(received.get(0));
            assertEquals(400, resp.getStatusCode());
        } finally {
            wsclient.close();
        }
    }
    
    //TODO this is a temporary way to verify the response; we should come up with something better.
    private static class Response {
        private Object data;
        private int pos; 
        private int statusCode;
        private String contentType;
        private Object entity;
        
        public Response(Object data) {
            this.data = data;
            String line = readLine();
            if (line != null) {
                statusCode = Integer.parseInt(line);
                while ((line = readLine()) != null) {
                    if (line.length() > 0) {
                        int del = line.indexOf(':');
                        String h = line.substring(0, del).trim();
                        String v = line.substring(del + 1).trim();
                        if ("Content-Type".equalsIgnoreCase(h)) {
                            contentType = v;
                        }
                    }
                }
            }
            if (data instanceof String) {
                entity = ((String)data).substring(pos);
            } else if (data instanceof byte[]) {
                entity = new byte[((byte[])data).length - pos];
                System.arraycopy((byte[])data, pos, (byte[])entity, 0, ((byte[])entity).length);
            }
        }
                
            
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        @SuppressWarnings("unused")
        public Object getEntity() {
            return entity;
        }
        
        public String getTextEntity() {
            return gettext(entity);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Status: ").append(statusCode).append("\r\n");
            sb.append("Type: ").append(contentType).append("\r\n");
            sb.append("Entity: ").append(gettext(entity)).append("\r\n");
            return sb.toString();
        }
        
        private String readLine() {
            StringBuilder sb = new StringBuilder();
            while (pos < length(data)) {
                int c = getchar(data, pos++);
                if (c == '\n') {
                    break;
                } else if (c == '\r') {
                    continue;
                } else {
                    sb.append((char)c);
                }
            }
            if (sb.length() == 0) {
                return null;
            }
            return sb.toString();
        }

        private int length(Object o) {
            return o instanceof char[] ? ((String)o).length() : (o instanceof byte[] ? ((byte[])o).length : 0);
        }

        private int getchar(Object o, int p) {
            return 0xff & (o instanceof String ? ((String)o).charAt(p) : (o instanceof byte[] ? ((byte[])o)[p] : -1));
        }

        private String gettext(Object o) {
            return o instanceof String ? (String)o : (o instanceof byte[] ? new String((byte[])o) : null);
        }
    }
    
    protected String getPort() {
        return PORT;
    }
}
