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

package org.esigate.extension.parallelesi;

import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.http.HttpEntityEnclosingRequest;
import org.esigate.HttpErrorPage;
import org.esigate.MockDriver;
import org.esigate.test.TestUtils;

public class InlineElementTest extends TestCase {
	private MockDriver provider;
	private HttpEntityEnclosingRequest request;

	@Override
	protected void setUp() throws Exception {
		provider = new MockDriver("mock");
		provider.addResource("/test", "test");
		provider.addResource("http://www.foo.com/test", "test");
		request = TestUtils.createRequest();
		provider.initHttpRequestParams(request, null);
	}

	public void testInlineElement() throws IOException, HttpErrorPage {
		String page = "begin <esi:inline name=\"someUri\" fetchable=\"yes\">inside inline</esi:inline>end";
		EsiRenderer tested = new EsiRenderer();
		StringWriter out = new StringWriter();
		tested.render(request, page, out);
		assertEquals("begin end", out.toString());
		InlineCache actual = InlineCache.getFragment("someUri");
		assertNotNull(actual);
		assertEquals(true, actual.isFetchable());
		assertEquals(false, actual.isExpired());
		assertEquals("inside inline", actual.getFragment());
	}

}