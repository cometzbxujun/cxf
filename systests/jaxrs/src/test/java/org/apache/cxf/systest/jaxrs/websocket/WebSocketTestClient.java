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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketByteListener;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

import org.apache.cxf.common.logging.LogUtils;



/**
 * Test client to do websocket calls.
 * @see JAXRSClientServerWebSocketTest
 * 
 * we may put this in test-tools so that other systests can use this code.
 * for now keep it here to experiment jaxrs websocket scenarios.
 */
class WebSocketTestClient {
    private static final Logger LOG = LogUtils.getL7dLogger(WebSocketTestClient.class);

    private List<Object> received;
    private List<Object> fragments;
    private CountDownLatch latch;
    private AsyncHttpClient client;
    private WebSocket websocket;
    private String url;
    
    public WebSocketTestClient(String url, int count) {
        this.received = new ArrayList<Object>();
        this.fragments = new ArrayList<Object>();
        this.latch = new CountDownLatch(count);
        this.client = new AsyncHttpClient();
        this.url = url;
    }
    
    public void connect() throws InterruptedException, ExecutionException, IOException {
        websocket = client.prepareGet(url).execute(
            new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WsSocketListener()).build()).get();
    }

    public void sendTextMessage(String message) {
        websocket.sendTextMessage(message);
    }

    public void sendMessage(byte[] message) {
        websocket.sendMessage(message);
    }
    
    public boolean await(int secs) throws InterruptedException {
        return latch.await(secs, TimeUnit.SECONDS);
    }
    
    public void reset(int count) {
        latch = new CountDownLatch(count);
        received.clear();
    }

    public List<Object> getReceived() {
        return received;
    }

    public void close() {
        websocket.close();
        client.close();
    }

    class WsSocketListener implements WebSocketTextListener, WebSocketByteListener {

        public void onOpen(WebSocket ws) {
            LOG.info("[ws] opened");            
        }

        public void onClose(WebSocket ws) {
            LOG.info("[ws] closed");            
        }

        public void onError(Throwable t) {
            LOG.info("[ws] error: " + t);                        
        }

        public void onMessage(byte[] message) {
            received.add(message);
            LOG.info("[ws] received bytes --> " + makeString(message));
            latch.countDown();
        }

        public void onFragment(byte[] fragment, boolean last) {
            processFragments(fragment, last);
        }

        public void onMessage(String message) {
            received.add(message);
            LOG.info("[ws] received --> " + message);
            latch.countDown();
        }

        public void onFragment(String fragment, boolean last) {
            processFragments(fragment, last);
        }
        
        private void processFragments(Object f, boolean last) {
            synchronized (fragments) {
                fragments.add(f);
                if (last) {
                    if (f instanceof String) {
                        // string
                        StringBuilder sb = new StringBuilder();
                        for (Iterator<Object> it = fragments.iterator(); it.hasNext();) {
                            Object o = it.next();
                            if (o instanceof String) {
                                sb.append((String)o);
                                it.remove();
                            }
                        }
                        received.add(sb.toString());
                    } else {
                        // byte[]
                        ByteArrayOutputStream bao = new ByteArrayOutputStream();
                        for (Iterator<Object> it = fragments.iterator(); it.hasNext();) {
                            Object o = it.next();
                            if (o instanceof byte[]) {
                                bao.write((byte[])o, 0, ((byte[])o).length);
                                it.remove();
                            }
                        }
                        received.add(bao.toByteArray());
                    }
                }
            }
        }
    }
    
    private static String makeString(byte[] data) {
        return data == null ? null : makeString(data, 0, data.length).toString();
    }

    private static StringBuilder makeString(byte[] data, int offset, int length) {
        if (data .length > 256) {
            return makeString(data, offset, 256).append("...");
        }
        StringBuilder xbuf = new StringBuilder().append("\nHEX: ");
        StringBuilder cbuf = new StringBuilder().append("\nASC: ");
        for (byte b : data) {
            writeHex(xbuf, 0xff & b);
            writePrintable(cbuf, 0xff & b);
        }
        return xbuf.append(cbuf);
    }
    
    private static void writeHex(StringBuilder buf, int b) {
        buf.append(Integer.toHexString(0x100 | (0xff & b)).substring(1)).append(' ');
    }
    
    private static void writePrintable(StringBuilder buf, int b) {
        if (b == 0x0d) {
            buf.append("\\r");
        } else if (b == 0x0a) {
            buf.append("\\n");
        } else if (b == 0x09) {
            buf.append("\\t");
        } else if ((0x80 & b) != 0) {
            buf.append('.').append(' ');
        } else {
            buf.append((char)b).append(' ');
        }
        buf.append(' ');
    }
}
