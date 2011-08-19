package org.esigate;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.http.cookie.Cookie;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.esigate.UserContext;
import org.esigate.cookie.CustomCookieStore;

public class UserContextTest extends TestCase {
	private UserContext tested;
	private IMocksControl control;
	private CustomCookieStore cookieStore;

	@Override
	protected void setUp() {
		control = EasyMock.createControl();
		cookieStore = control.createMock(CustomCookieStore.class);

		tested = new UserContext(cookieStore);
	}

	@Override
	protected void tearDown() {
		tested = null;

		control = null;
		cookieStore = null;
	}

	public void testCreation() {
		try {
			new UserContext(null);
			fail("should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertEquals("cookieStore implementation not set", e.getMessage());
		}
	}

	public void testGetCookies() {
		Cookie cookie = control.createMock(Cookie.class);

		EasyMock.expect(cookieStore.getCookies()).andReturn(
				Arrays.asList(cookie));
		control.replay();

		List<Cookie> actual = tested.getCookies();
		assertNotNull(actual);
		assertEquals(1, actual.size());
		assertSame(cookie, actual.get(0));
		control.verify();
	}
}
