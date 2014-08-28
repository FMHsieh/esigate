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
package org.esigate.extension.surrogate;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.strip;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.esigate.Driver;
import org.esigate.Parameters;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.FetchEvent;
import org.esigate.events.impl.ProxyEvent;
import org.esigate.extension.Extension;
import org.esigate.http.DeleteResponseHeader;
import org.esigate.http.MoveResponseHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Edge-Arch specification 1.0.
 * 
 * <p>
 * See :
 * <ul>
 * <li>http://www.w3.org/TR/edge-arch</li>
 * <li>http://docs.oracle.com/cd/E17904_01/web.1111/e10143/esi.htm</li>
 * </ul>
 * 
 * <p>
 * This extension allows control of esigate features from the provider application.
 * 
 * <p>
 * This extension cooperates with other extensions to get the currently supported capabilities. To participate to
 * capabilities collection, and extension should register to EVENT_SURROGATE_CAPABILITIES and provide capabilities when
 * the event is fired.
 * <p>
 * 
 * <pre>
 * driver.getEventManager().register(Surrogate.EVENT_SURROGATE_CAPABILITIES, new IEventListener() {
 *     public boolean event(EventDefinition id, Event event) {
 *         CapabilitiesEvent capEvent = (CapabilitiesEvent) event;
 *         capEvent.capabilities.add(&quot;ESI/1.0&quot;);
 *         capEvent.capabilities.add(&quot;ESI-Inline/1.0&quot;);
 *         capEvent.capabilities.add(&quot;ESIGATE/4.0&quot;);
 *         return true;
 *     }
 * });
 * </pre>
 * <p>
 * The provider may respond with control directive. In that case, the extension should ensure that its capability are
 * requested and do not process response otherwise.
 * <p>
 * 
 * <pre>
 *  if (renderEvent.httpResponse.containsHeader(Surrogate.H_X_ENABLED_CAPABILITIES)) {
 *      String capabilities = renderEvent.httpResponse.getFirstHeader(Surrogate.H_X_ENABLED_CAPABILITIES).getValue();
 * 
 *      if (!containsIgnoreCase(capabilities, "ESI/1.0") {
 *          // Cancel processing
 *      }
 *  }
 * </pre>
 * 
 * <p>
 * Targeting is supported.
 * 
 * <p>
 * NOTE:
 * <ul>
 * <li>no-store-remote is not honored since esigate is generally not used as a CDN.</li>
 * </ul>
 * 
 * @author Nicolas Richeton
 * 
 */
public class Surrogate implements Extension, IEventListener {

    private static final String H_SURROGATE_CONTROL = "Surrogate-Control";

    private static final String H_SURROGATE_CAPABILITIES = "Surrogate-Capabilities";

    private static final Logger LOG = LoggerFactory.getLogger(Surrogate.class);

    /**
     * This is an internal header, used to track the requested capabilities.
     * <p>
     * Value is the value of the content directive from the Surrogate-Control header.
     */
    public static final String H_X_ENABLED_CAPABILITIES = "X-Esigate-Internal-Enabled-Capabilities";
    /**
     * This header is used to flag the presence of another Surrogate in front of esigate.
     */
    private static final String H_X_SURROGATE = "X-Esigate-Internal-Surrogate";
    private static final String H_X_ORIGINAL_CACHE_CONTROL = "X-Esigate-Int-Surrogate-OCC";
    /**
     * This header is used to store the esigate instance id for the current request.
     */
    private static final String H_X_SURROGATE_ID = "X-Esigate-Int-Surrogate-Id";
    /**
     * This is an internal header used to store the value of the Surrogate-Control header which will be set to the next
     * surrogate. This is based on the original header, but with the removal of Capabilities processed on the current
     * hop.
     */
    private static final String H_X_NEXT_SURROGATE_CONTROL = "X-Esigate-Int-Surrogate-NSC";
    private String[] capabilities;
    /**
     * Pre-generated esigate token including all capabilities reported by extensions.
     */
    private String esigateToken;
    /**
     * This event is fired on startup to collect all installed capabilities.
     */
    public static final EventDefinition EVENT_SURROGATE_CAPABILITIES = new EventDefinition(
            "org.esigate.surrogate.capabilities", EventDefinition.TYPE_DEFAULT);
    private static final String CAP_SURROGATE = "Surrogate/1.0";

    @Override
    public void init(Driver driver, Properties properties) {
        CapabilitiesEvent capEvent = new CapabilitiesEvent();
        capEvent.getCapabilities().add(CAP_SURROGATE);

        // Build all supported capabilities
        driver.getEventManager().fire(EVENT_SURROGATE_CAPABILITIES, capEvent);
        this.capabilities = capEvent.getCapabilities().toArray(new String[] {});
        LOG.info("Surrogate capabilities: {}", join(this.capabilities, " "));

        // Build esigate token
        this.esigateToken = "=\"" + join(this.capabilities, " ") + "\"";

        // Register for events.
        driver.getEventManager().register(EventManager.EVENT_FETCH_PRE, this);
        driver.getEventManager().register(EventManager.EVENT_FETCH_POST, this);
        driver.getEventManager().register(EventManager.EVENT_PROXY_PRE, this);
        driver.getEventManager().register(EventManager.EVENT_PROXY_POST, this);

        // Restore original Cache-Control header
        driver.getEventManager().register(EventManager.EVENT_FRAGMENT_POST,
                new MoveResponseHeader(H_X_ORIGINAL_CACHE_CONTROL, "Cache-Control"));

        // Delete internal header
        driver.getEventManager().register(EventManager.EVENT_PROXY_POST,
                new DeleteResponseHeader(H_X_ENABLED_CAPABILITIES));

    }

    /**
     * Return a new token, unique for the current Surrogate-Capability header.
     * <p>
     * Uses "esigate" and appends a number if necessary.
     * 
     * @param currentCapabilitiesHeader
     *            existing header which may contains tokens of other proxies (including other esigate instances).
     * @return unique token
     */
    private static String getUniqueToken(String currentCapabilitiesHeader) {
        String token = "esigate";

        if (currentCapabilitiesHeader != null && currentCapabilitiesHeader.contains(token + "=\"")) {
            int id = 2;
            while (currentCapabilitiesHeader.contains(token + id + "=\"")) {
                id++;
            }
            token = token + id;
        }

        return token;
    }

    @Override
    public boolean event(EventDefinition id, Event event) {

        if (EventManager.EVENT_FETCH_PRE.equals(id)) {
            // Add Surrogate-Capabilities or append to existing header.
            FetchEvent e = (FetchEvent) event;
            Header h = e.getHttpRequest().getFirstHeader(H_SURROGATE_CAPABILITIES);

            StringBuilder archCapabilities = new StringBuilder(Parameters.SMALL_BUFFER_SIZE);
            if (h != null && !isEmpty(h.getValue())) {
                archCapabilities.append(defaultString(h.getValue()));
                archCapabilities.append(", ");
            }
            String currentCapabilitiesHeader = null;
            if (h != null) {
                currentCapabilitiesHeader = h.getValue();
            }
            String uniqueId = getUniqueToken(currentCapabilitiesHeader);
            e.getHttpRequest().setHeader(H_X_SURROGATE_ID, uniqueId);
            archCapabilities.append(uniqueId);
            archCapabilities.append(this.esigateToken);
            e.getHttpRequest().setHeader(H_SURROGATE_CAPABILITIES, archCapabilities.toString());

            // This header is used internally, and should not be forwarded.
            e.getHttpRequest().removeHeaders(H_X_SURROGATE);
        } else if (EventManager.EVENT_FETCH_POST.equals(id)) {
            onPostFetch(event);
        } else if (EventManager.EVENT_PROXY_PRE.equals(id)) {
            ProxyEvent e = (ProxyEvent) event;
            // Do we have another surrogate in front of esigate
            if (e.getOriginalRequest().containsHeader(H_SURROGATE_CAPABILITIES)) {
                e.getOriginalRequest().setHeader(H_X_SURROGATE, "true");
            }
        } else if (EventManager.EVENT_PROXY_POST.equals(id)) {
            // Remove Surrogate Control content
            ProxyEvent e = (ProxyEvent) event;

            if (e.getResponse() != null) {
                processSurrogateControlContent(e.getResponse(), e.getOriginalRequest().containsHeader(H_X_SURROGATE));
            } else if (e.getErrorPage() != null) {
                processSurrogateControlContent(e.getErrorPage().getHttpResponse(), e.getOriginalRequest()
                        .containsHeader(H_X_SURROGATE));
            }

        }

        return true;
    }

    /**
     * <ul>
     * <li>Inject H_X_ENABLED_CAPABILITIES into response.</li>
     * <li>Consume capabilities. Does not support targeting yet.</li>
     * <li>Update caching directives.</li>
     * </ul>
     * 
     * @param event
     *            Incoming fetch event.
     */
    private void onPostFetch(Event event) {
        // Update caching policies
        FetchEvent e = (FetchEvent) event;

        if (!e.getHttpResponse().containsHeader(H_SURROGATE_CONTROL)) {
            return;
        }

        List<String> enabledCapabilities = new ArrayList<String>();
        List<String> remainingCapabilities = new ArrayList<String>();
        List<String> newSurrogateControlL = new ArrayList<String>();
        List<String> newCacheContent = new ArrayList<String>();

        String controlHeader = e.getHttpResponse().getFirstHeader(H_SURROGATE_CONTROL).getValue();
        String[] control = split(controlHeader, ",");

        for (String directiveAndTarget : control) {
            String directive = strip(directiveAndTarget);

            // Is directive targeted
            int targetIndex = directive.lastIndexOf(';');
            String target = null;

            if (targetIndex > 0) {
                target = directive.substring(targetIndex + 1);
                directive = directive.substring(0, targetIndex);
            }

            if (target != null && !target.equals(e.getHttpRequest().getFirstHeader(H_X_SURROGATE_ID))) {
                // If directive is not targeted to current instance.
                newSurrogateControlL.add(directiveAndTarget);

            } else if (directive.startsWith("content=\"")) {
                // Handle content

                String[] content = split(directive.substring("content=\"".length(), directive.length() - 1), " ");

                for (String contentCap : content) {
                    contentCap = strip(contentCap);
                    if (contains(this.capabilities, contentCap)) {
                        enabledCapabilities.add(contentCap);
                    } else {
                        remainingCapabilities.add(contentCap);
                    }
                }
                if (remainingCapabilities.size() > 0) {
                    newSurrogateControlL.add("content=\"" + join(remainingCapabilities, " ") + "\"");
                }
            } else if (directive.startsWith("max-age=")) {
                String[] maxAge = split(directive, "+");
                newCacheContent.add(maxAge[0]);
                // Freshness extension
                if (maxAge.length > 1) {
                    newCacheContent.add("stale-while-revalidate=" + maxAge[1]);
                    newCacheContent.add("stale-if-error=" + maxAge[1]);
                }

                newSurrogateControlL.add(directive);
            } else if (directive.startsWith("no-store")) {
                newSurrogateControlL.add(directive);
                newCacheContent.add(directive);
            } else {
                newSurrogateControlL.add(directive);
            }
        }

        e.getHttpResponse().setHeader(H_X_ENABLED_CAPABILITIES, join(enabledCapabilities, " "));
        e.getHttpResponse().setHeader(H_X_NEXT_SURROGATE_CONTROL, join(newSurrogateControlL, ", "));

        // If cache control must be updated.
        if (newCacheContent.size() > 0) {
            MoveResponseHeader.moveHeader(e.getHttpResponse(), "Cache-Control", H_X_ORIGINAL_CACHE_CONTROL);
            e.getHttpResponse().setHeader("Cache-Control", join(newCacheContent, ", "));
        }
    }

    /**
     * Remove Surrogate-Control header or replace by its new value.
     * 
     * @param response
     *            backend HTTP response.
     * @param keepHeader
     *            should the Surrogate-Control header be forwarded to the client.
     */
    private static void processSurrogateControlContent(HttpResponse response, boolean keepHeader) {
        if (!response.containsHeader(H_SURROGATE_CONTROL)) {
            return;
        }

        if (!keepHeader) {
            response.removeHeaders(H_SURROGATE_CONTROL);
            return;
        }

        MoveResponseHeader.moveHeader(response, H_X_NEXT_SURROGATE_CONTROL, H_SURROGATE_CONTROL);
    }
}
