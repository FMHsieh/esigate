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

import junit.framework.TestCase;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.esigate.Driver;
import org.esigate.DriverFactory;
import org.esigate.Parameters;
import org.esigate.cookie.CookieManager;
import org.esigate.events.EventManager;
import org.esigate.http.HttpClientHelper;
import org.esigate.test.TestUtils;
import org.esigate.test.conn.MockConnectionManager;

public class DefaultCharsetTest extends TestCase {


	private Driver createMockDriver(Properties properties, HttpClientConnectionManager connectionManager) {
		return createMockDriver(properties, connectionManager, "tested");
	}

	private Driver createMockDriver(Properties properties, HttpClientConnectionManager connectionManager, String name) {
		CookieManager cookieManager = ExtensionFactory.getExtension(properties, Parameters.COOKIE_MANAGER, null);

		HttpClientHelper httpClientHelper = new HttpClientHelper(new EventManager(name), cookieManager, properties, connectionManager);
		Driver driver = new Driver(name, properties, httpClientHelper);
		DriverFactory.put(name, driver);
		return driver;
	}

	public void testDefaultCharsetExt() throws Exception {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost/");
		properties.put(Parameters.EXTENSIONS.name,
				"org.esigate.extension.DefaultCharset");
		properties.put(Parameters.USE_CACHE.name, "true");

		MockConnectionManager mockHttpClient = new MockConnectionManager();
		BasicHttpResponse response = new BasicHttpResponse(new ProtocolVersion(
				"HTTP", 1, 1), HttpStatus.SC_OK, "Ok");
		response.addHeader("Date", "Thu, 13 Dec 2012 08:55:37 GMT");
		response.addHeader("Content-Type", "text/html");
		response.setEntity(new StringEntity("test"));
		mockHttpClient.setResponse(response);

		Driver driver = createMockDriver(properties, mockHttpClient);

		HttpEntityEnclosingRequest request = TestUtils
				.createRequest("http://test.mydomain.fr/foobar/");

		driver.proxy("/foobar/", request);

		assertEquals("Encoding should be added", "text/html; charset=ISO-8859-1",
				TestUtils.getResponse(request).getFirstHeader("Content-Type")
						.getValue());
	}

	public void testDefaultCharsetExtConfig() throws Exception {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost/");
		properties.put(Parameters.EXTENSIONS.name,
				"org.esigate.extension.DefaultCharset");
		// Does not work because of
		// https://sourceforge.net/apps/mantisbt/webassembletool/view.php?id=185
		// properties.put(Parameters.USE_CACHE.name, "false");
		properties.put("defaultCharset", "utf-8");

		MockConnectionManager mockHttpClient = new MockConnectionManager();
		BasicHttpResponse response = new BasicHttpResponse(new ProtocolVersion(
				"HTTP", 1, 1), HttpStatus.SC_OK, "Ok");
		response.addHeader("Date", "Thu, 13 Dec 2012 08:55:37 GMT");
		response.addHeader("Content-Type", "text/html");
		response.setEntity(new StringEntity("test"));
		mockHttpClient.setResponse(response);

		Driver driver = createMockDriver(properties, mockHttpClient);

		HttpEntityEnclosingRequest request = TestUtils
				.createRequest("http://test.mydomain.fr/foobar/");

		driver.proxy("/foobar/", request);

		assertEquals("Encoding should be added",
				"text/html; charset=utf-8", TestUtils.getResponse(request)
						.getFirstHeader("Content-Type").getValue());
	}

	public void testDefaultCharsetExtNonParsable() throws Exception {
		Properties properties = new Properties();
		properties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost/");
		properties.put(Parameters.EXTENSIONS.name,
				"org.esigate.extension.DefaultCharset");
		properties.put(Parameters.USE_CACHE.name, "true");

		MockConnectionManager mockHttpClient = new MockConnectionManager();
		BasicHttpResponse response = new BasicHttpResponse(new ProtocolVersion(
				"HTTP", 1, 1), HttpStatus.SC_OK, "Ok");
		response.addHeader("Date", "Thu, 13 Dec 2012 08:55:37 GMT");
		response.addHeader("Content-Type", "text/xml");
		response.setEntity(new StringEntity("test"));
		mockHttpClient.setResponse(response);

		Driver driver = createMockDriver(properties, mockHttpClient);

		HttpEntityEnclosingRequest request = TestUtils
				.createRequest("http://test.mydomain.fr/foobar/");

		driver.proxy("/foobar/", request);

		assertEquals("Encoding should be added", "text/xml", TestUtils
				.getResponse(request).getFirstHeader("Content-Type").getValue());
	}

}
