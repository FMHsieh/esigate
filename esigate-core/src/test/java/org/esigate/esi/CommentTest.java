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

package org.esigate.esi;

import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.esigate.Driver;
import org.esigate.HttpErrorPage;
import org.esigate.MockDriver;
import org.esigate.test.MockHttpRequest;
import org.esigate.test.MockHttpResponse;

public class CommentTest extends TestCase {

	private MockHttpRequest request;
	private EsiRenderer tested;

	@Override
	protected void setUp() throws Exception {
		Driver provider = new MockDriver();
		request = new MockHttpRequest();
		tested = new EsiRenderer();
		provider.initHttpRequestParams(request, new MockHttpResponse(), null);
		MockDriver provider1 = new MockDriver("provider1");
		provider1.addResource("/test", "replacement");
	}

	public void testComment() throws IOException, HttpErrorPage {
		String page = "begin <!--esi<sometag> some text</sometag>--> end";
		StringWriter out = new StringWriter();
		tested.render(request, page, out);
		assertEquals("begin <sometag> some text</sometag> end", out.toString());
	}

	public void testCommentVars() throws IOException, HttpErrorPage {
		String page = "<!--esi <p><esi:vars>Hello, $(HTTP_COOKIE{name})!</esi:vars></p> -->";
		request.addCookie(new BasicClientCookie("name", "world"));
		StringWriter out = new StringWriter();
		tested.render(request, page, out);
		assertEquals(" <p>Hello, world!</p> ", out.toString());
	}

	/**
	 * http://sourceforge.net/apps/mantisbt/webassembletool/view.php?id=126
	 * @throws Exception 
	 */
	public void testCommentedEsiTags() throws Exception {
		String page = "begin <!--esi<esi:include src=\"$(PROVIDER{provider1})/test\">--> content <!--esi</esi:include>--> end";
		StringWriter out = new StringWriter();
		tested.render(request, page, out);
		assertEquals("begin replacement end", out.toString());
	}

}
