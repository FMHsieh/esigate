package org.esigate.events.impl;

import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.esigate.events.Event;
import org.esigate.http.OutgoingRequest;

/**
 * Fetch event : when a new HTTP call is made to get a new block/template (Cache miss).
 * 
 * @author Nicolas Richeton
 * 
 */
public class FetchEvent extends Event {
    /**
     * The response returned by the remote server.
     * <p>
     * May be null if the request has not been executed yet. If this case, setting a response cancels the HTTP call and
     * use the given object instead.
     */
    public HttpResponse httpResponse;
    /**
     * The request context.
     */
    public HttpClientContext httpContext;
    /**
     * The new HTTP call details.
     */
    public OutgoingRequest httpRequest = null;

    private final boolean proxy;

    public FetchEvent(boolean proxy) {
        this.proxy = proxy;
    }

    /**
     * Proxy or include mode.
     * 
     * @return true if proxy mode
     */
    public boolean isProxy() {
        return proxy;
    }

}
