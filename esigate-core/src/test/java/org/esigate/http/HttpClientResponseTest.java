package org.esigate.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class HttpClientResponseTest extends TestCase {
	private HttpClientResponse tested;

	private IMocksControl control;
	private HttpHost httpHost;
	private HttpRequest httpRequest;
	private HttpClient httpClient;
	private CookieStore cookieStore;
	private HttpResponse httpResponse;
	private HttpEntity httpEntity;

	@Override
	protected void setUp() {
		control = EasyMock.createControl();
		httpRequest = control.createMock(HttpRequest.class);
		httpClient = control.createMock(HttpClient.class);
		cookieStore = control.createMock(CookieStore.class);
		cookieStore = control.createMock(CookieStore.class);
		httpResponse = control.createMock(HttpResponse.class);
		httpEntity = control.createMock(HttpEntity.class);
		httpHost = new HttpHost("httpHost", 1234);
	}

	@Override
	protected void tearDown() {
		control = null;
		httpRequest = null;
		httpClient = null;
		cookieStore = null;
		httpResponse = null;
		httpHost = null;
		httpEntity = null;

		tested = null;
	}

	public void testCreate() throws IOException {
		// initial set up
		StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("p", 1,
				2), HttpServletResponse.SC_MOVED_PERMANENTLY, "reason");
		EasyMock.expect(
				httpClient.execute(EasyMock.anyObject(HttpHost.class),
						EasyMock.anyObject(HttpRequest.class),
						EasyMock.anyObject(HttpContext.class))).andReturn(
				httpResponse);
		EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine)
				.anyTimes();
		EasyMock.expect(httpResponse.getEntity()).andReturn(null);
		EasyMock.expect(httpResponse.getFirstHeader(HttpHeaders.LOCATION))
				.andReturn(new BasicHeader("h", "value"));
		control.replay();

		tested = HttpClientResponse.create(httpHost, httpRequest, httpClient,
				cookieStore);
		assertNotNull(tested);
	}

	public void testGetHeaderNames() throws IOException {
		// initial set up
		StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("p", 1,
				2), HttpServletResponse.SC_MOVED_PERMANENTLY, "reason");
		EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine)
				.anyTimes();
		EasyMock.expect(httpResponse.getEntity()).andReturn(null);
		// getHeaderNames behaviour
		Header[] headers = new Header[] { new BasicHeader("h1", "value 1"),
				new BasicHeader("h1", "value 2"),
				new BasicHeader("h2", "value 3") };
		EasyMock.expect(httpResponse.getAllHeaders()).andReturn(headers);
		control.replay();

		tested = new HttpClientResponse(httpResponse, null);
		Collection<String> actual = tested.getHeaderNames();

		assertNotNull(actual);
		String names[] = toArray(actual);
		assertEquals(2, names.length);
		assertEquals("h1", names[0]);
		assertEquals("h2", names[1]);
		control.verify();
	}

	public void testGetHeaders() throws IOException {
		// initial set up
		StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("p", 1,
				2), HttpServletResponse.SC_MOVED_PERMANENTLY, "reason");
		EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine)
				.anyTimes();
		EasyMock.expect(httpResponse.getEntity()).andReturn(null);
		// getHeaderNames behaviour
		Header[] headers = new Header[] { new BasicHeader("h1", "value 1"),
				new BasicHeader("h1", "value 2"),
				new BasicHeader("h2", "value 3") };
		EasyMock.expect(httpResponse.getHeaders("header-name")).andReturn(
				headers);
		control.replay();

		tested = new HttpClientResponse(httpResponse, null);
		String[] actual = tested.getHeaders("header-name");

		assertNotNull(actual);
		assertEquals(3, actual.length);
		assertEquals("value 1", actual[0]);
		assertEquals("value 2", actual[1]);
		assertEquals("value 3", actual[2]);
		control.verify();
	}

	private String[] toArray(Collection<String> src) {
		String[] result = src.toArray(new String[src.size()]);
		Arrays.sort(result);
		return result;
	}

	public void testDecompressStream() throws ClientProtocolException,
			IOException {

		String content = "To be compressed";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		byte[] uncompressedBytes = content.getBytes();
		gzos.write(uncompressedBytes, 0, uncompressedBytes.length);
		gzos.close();
		byte[] compressedBytes = baos.toByteArray();
		StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("p", 1,
				2), HttpServletResponse.SC_MOVED_PERMANENTLY, "reason");
		EasyMock.expect(
				httpClient.execute(EasyMock.anyObject(HttpHost.class),
						EasyMock.anyObject(HttpRequest.class),
						EasyMock.anyObject(HttpContext.class))).andReturn(
				httpResponse);
		EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine)
				.anyTimes();
		EasyMock.expect(httpResponse.getEntity()).andReturn(httpEntity);
		EasyMock.expect(
				httpResponse.getFirstHeader(HttpHeaders.CONTENT_ENCODING))
				.andReturn(
						new BasicHeader(HttpHeaders.CONTENT_ENCODING, "gzip"));
		EasyMock.expect(httpEntity.getContent()).andReturn(
				new ByteArrayInputStream(compressedBytes));

		EasyMock.expect(httpResponse.getFirstHeader(HttpHeaders.LOCATION))
				.andReturn(new BasicHeader(HttpHeaders.LOCATION, "value"));

		control.replay();
		tested = HttpClientResponse.create(httpHost, httpRequest, httpClient,
				cookieStore);
		assertNotNull(tested);
		InputStream decompressed = tested.openStream();
		assertNotNull(decompressed);
		String res = IOUtils.toString(decompressed);
		assertEquals(content, res);

	}

}
