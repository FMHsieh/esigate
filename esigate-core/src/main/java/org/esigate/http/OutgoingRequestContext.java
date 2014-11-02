/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.esigate.http;

import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

import java.util.LinkedList;
import java.util.Queue;

public class OutgoingRequestContext extends HttpClientContext {
    private static final String PROXY = "PROXY";
    private static final String OUTGOING_REQUEST = "OUTGOING_REQUEST";
    private static final String PHYSICAL_HOST = "PHYSICAL_HOST";

    public static OutgoingRequestContext adapt(final HttpContext context) {
        if (context instanceof OutgoingRequestContext) {
            return (OutgoingRequestContext) context;
        } else {
            return new OutgoingRequestContext(context);
        }
    }

    private OutgoingRequestContext(final HttpContext context) {
        super(context);
    }

    public OutgoingRequestContext() {
        super();
    }

    public boolean isProxy() {
        Boolean proxy = getAttribute(PROXY, Boolean.class);
        if (proxy == null) {
            return false;
        } else {
            return proxy.booleanValue();
        }
    }

    public void setProxy(boolean proxy) {
        setAttribute(PROXY, proxy);
    }

    public OutgoingRequest getOutgoingRequest() {
        return getAttribute(OUTGOING_REQUEST, OutgoingRequest.class);
    }

    public void setOutgoingRequest(OutgoingRequest outgoingRequest) {
        setAttribute(OUTGOING_REQUEST, outgoingRequest);
    }

    public HttpHost getPhysicalHost() {
        return getAttribute(PHYSICAL_HOST, HttpHost.class);
    }

    public void setPhysicalHost(HttpHost httpHost) {
        setAttribute(PHYSICAL_HOST, httpHost);
    }

    /**
     * 
     * @param name
     *            attribute name
     * @param o
     *            value
     * @param save
     *            save previous attribute value to restore later
     */
    public void setAttribute(String name, Object o, boolean save) {
        if (save) {
            String historyAttribute = name + "history";
            Queue history = (Queue) getAttribute(historyAttribute);
            if (history == null) {
                history = new LinkedList<Long>();
                setAttribute(historyAttribute, history);
            }
            if (this.getAttribute(name) != null) {
                history.add(getAttribute(name));
            }
        }
        setAttribute(name, o);
    }

    /**
     * remove attribute
     * 
     * @param name
     *            attribute name
     * @param restore
     *            restore previous attribute value
     * @return attribute value
     */
    public Object removeAttribute(String name, boolean restore) {
        Object result = removeAttribute(name);
        if (restore) {
            String historyAttribute = name + "history";
            Queue history = (Queue) getAttribute(historyAttribute);
            if (history != null && !history.isEmpty()) {
                Object previous = history.remove();
                setAttribute(name, previous);
            }
        }
        return result;
    }
}
