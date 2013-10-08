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

package org.esigate.extension;

import java.util.Properties;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.esigate.Driver;
import org.esigate.events.Event;
import org.esigate.events.EventDefinition;
import org.esigate.events.EventManager;
import org.esigate.events.IEventListener;
import org.esigate.events.impl.FetchEvent;
import org.esigate.http.HttpClientHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This extension logs requests to remote systems.
 * <p>
 * Be sure to put this extension as the first extension in order to log the
 * whole request time, including all extension processing.
 * 
 * <p>
 * Log level is :
 * <ul>
 * <li>WARN for status codes >= 400</li>
 * <li>INFO for other codes</li>
 * </ul>
 * <p>
 * Logged data are :
 * 
 * <ul>
 * <li>Request status line</li>
 * <li>Request headers</li>
 * <li>Response status line</li>
 * <li>Response headers</li>
 * <li>Cache status (HIT, MISS, ...)</li>
 * <li>Request time</li>
 * 
 * </ul>
 * 
 * 
 * @author Nicolas Richeton
 * 
 */
public class FetchLogging implements Extension, IEventListener {
	private static final String TIME = "org.esigate.time.external";
	private static final Logger LOG = LoggerFactory.getLogger(FetchLogging.class);

	public void init(Driver driver, Properties properties) {
		driver.getEventManager().register(EventManager.EVENT_FETCH_POST, this);
		driver.getEventManager().register(EventManager.EVENT_FETCH_PRE, this);
	}

	public boolean event(EventDefinition id, Event event) {

		FetchEvent e = (FetchEvent) event;

		if (EventManager.EVENT_FETCH_POST.equals(id)) {
			int statusCode = e.httpResponse.getStatusLine().getStatusCode();

			// Log only if info or issue
			if (LOG.isInfoEnabled() || statusCode >= 400) {
				HttpRequest lastRequest = e.httpRequest;

				// Create log message
				HttpHost targetHost = (HttpHost) lastRequest.getParams().getParameter(HttpClientHelper.TARGET_HOST);

				String requestLine = lastRequest.getRequestLine().toString();
				String statusLine = e.httpResponse.getStatusLine().toString();

				String reqHeaders = ArrayUtils.toString(lastRequest.getAllHeaders());
				String respHeaders = ArrayUtils.toString(e.httpResponse.getAllHeaders());

				long time = System.currentTimeMillis() - (Long) e.httpContext.removeAttribute(TIME);

				StringBuilder logMessage = new StringBuilder();
			
				// Display target host, protocol and port
				if (targetHost != null) {
					logMessage.append(targetHost.getSchemeName());
					logMessage.append("://");
					logMessage.append(targetHost.getHostName());
					
					if (targetHost.getPort() != -1) {
						logMessage.append(":");
						logMessage.append(targetHost.getPort());
					}
					
					logMessage.append(" - ");
				}
				// Append request information
				logMessage.append(  requestLine + " " + reqHeaders + " -> " + statusLine + " (" + time + " ms) " + " "
						+ respHeaders);

				if (statusCode >= 400)
					LOG.warn(logMessage.toString());
				else
					LOG.info(logMessage.toString());
			}
		} else {
			e.httpContext.setAttribute(TIME, System.currentTimeMillis());
		}

		// Continue processing
		return true;
	}

}
