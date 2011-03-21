package net.webassembletool.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class HttpClientResponseTest extends TestCase {
	private HttpClientResponse tested;

	private IMocksControl control;
	private HttpHost httpHost;
	private HttpRequest httpRequest;
	private HttpClient httpClient;
	private HttpContext httpContext;
	private HttpResponse httpResponse;

	@Override
	protected void setUp() {
		control = EasyMock.createControl();
		httpRequest = control.createMock(HttpRequest.class);
		httpClient = control.createMock(HttpClient.class);
		httpContext = control.createMock(HttpContext.class);
		httpResponse = control.createMock(HttpResponse.class);

		httpHost = new HttpHost("httpHost", 1234);
	}

	@Override
	protected void tearDown() {
		control = null;
		httpRequest = null;
		httpClient = null;
		httpContext = null;
		httpResponse = null;
		httpHost = null;

		tested = null;
	}

	public void testCreate() throws IOException {
		// initial set up
		StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("p", 1, 2), HttpServletResponse.SC_MOVED_PERMANENTLY,
				"reason");
		EasyMock.expect(httpClient.execute(httpHost, httpRequest, httpContext)).andReturn(httpResponse);
		EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine).anyTimes();
		EasyMock.expect(httpResponse.getEntity()).andReturn(null);
		EasyMock.expect(httpResponse.getFirstHeader(HttpHeaders.LOCATION)).andReturn(new BasicHeader("h", "value"));
		control.replay();

		tested = HttpClientResponse.create(httpHost, httpRequest, httpClient, httpContext);
		assertNotNull(tested);
	}

	public void testGetHeaderNames() throws IOException {
		// initial set up
		StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("p", 1, 2), HttpServletResponse.SC_MOVED_PERMANENTLY,
				"reason");
		EasyMock.expect(httpResponse.getStatusLine()).andReturn(statusLine).anyTimes();
		EasyMock.expect(httpResponse.getEntity()).andReturn(null);
		// getHeaderNames behaviour
		Header[] headers = new Header[] { new BasicHeader("h1", "value 1"), new BasicHeader("h1", "value 2"),
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

	private String[] toArray(Collection<String> src) {
		String[] result = src.toArray(new String[src.size()]);
		Arrays.sort(result);
		return result;
	}

	public void testBuildLocation() {
		HttpContext context = control.createMock(HttpContext.class);
		HttpRequest request = control.createMock(HttpRequest.class);
		RequestLine requestLine = control.createMock(RequestLine.class);
		HttpHost host = new HttpHost("host", 123);

		EasyMock.expect(context.getAttribute(ExecutionContext.HTTP_TARGET_HOST)).andReturn(host);
		EasyMock.expect(context.getAttribute(ExecutionContext.HTTP_REQUEST)).andReturn(request);
		EasyMock.expect(request.getRequestLine()).andReturn(requestLine);
		EasyMock.expect(requestLine.getUri()).andReturn("/some/uri");
		control.replay();

		assertEquals("http://host:123/some/uri", HttpClientResponse.buildLocation(context));
		control.verify();
	}

}
