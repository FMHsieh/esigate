package org.esigate.cache;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.esigate.MockDriver;
import org.esigate.ResourceContext;
import org.esigate.output.Output;
import org.esigate.resource.Resource;
import org.esigate.test.MockHttpServletRequest;
import org.esigate.test.MockHttpServletResponse;

public class CacheTest extends TestCase{
	
	protected CacheStorage storage = new DefaultCacheStorage();
	
	private MockDriver mockDriver;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private ResourceContext resourceContext;

	@Override
	protected void setUp() throws Exception {
		mockDriver = new MockDriver("mock");
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		resourceContext = new ResourceContext(mockDriver,
				"/test", null, request, response);
	}
	
	public void testPutGetSelect() throws Exception {
		Cache cache = new Cache();
		cache.setStorage(storage);
		
		byte[] byteArray = "test".getBytes("utf-8");
		int statusCode = 200;
		String statusMessage = "OK";
		Map<String, Set<String>> headers = new HashMap<String, Set<String>>();
		headers.put("Cache-Control", Collections.singleton("private"));

		CachedResponse cachedResponse = new CachedResponse(byteArray, "utf-8",
				headers, statusCode, statusMessage);
		Resource newResource = new TestResource();
		
		assertNull(cache.get(resourceContext));

		cache.put(resourceContext, cachedResponse);
		
		assertEquals(cachedResponse, cache.get(resourceContext));
		assertNull(cache.get(new ResourceContext(mockDriver,"/test-1", null, request, response)));
		assertEquals(newResource, cache.select(resourceContext, cachedResponse, newResource));
	}
	
	public class TestResource extends Resource {
		
		@Override
		public void render(Output output) throws IOException {
		}

		@Override
		public void release() {
		}

		@Override
		public int getStatusCode() {
			return 200;
		}

		@Override
		public String getHeader(String name) {
			return null;
		}

		@Override
		public Collection<String> getHeaders(String name) {
			return null;
		}
		
		@Override
		public Collection<String> getHeaderNames() {
			return null;
		}

		@Override
		public String getStatusMessage() {
			return "OK";
		}

	};

}
