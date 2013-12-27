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

import junit.framework.TestCase;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.esigate.HttpErrorPage;
import org.esigate.MockRequestExecutor;
import org.esigate.impl.DriverRequest;
import org.esigate.test.TestUtils;

public class VarsElementTest extends TestCase {

    private DriverRequest request;
    private EsiRenderer tested;
    private MockRequestExecutor provider;

    @Override
    protected void setUp() throws Exception {
        provider = MockRequestExecutor.createMockDriver("mock");
        provider.addResource("/test", "test");
        provider.addResource("http://www.foo.com/test", "test");

        request = TestUtils.createRequest(provider.getDriver());
        tested = new EsiRenderer();
    }

    public void testHttpHost() throws IOException, HttpErrorPage {
        String page = "begin <esi:vars>$(HTTP_HOST)</esi:vars> end";
        request = TestUtils.createRequest("http://www.foo.com", provider.getDriver());
        StringBuilderWriter out = new StringBuilderWriter();
        tested.render(request, page, out);
        assertEquals("begin www.foo.com end", out.toString());
    }

    public void testCookie() throws IOException, HttpErrorPage {
        String page = "begin <esi:vars>"
                + "<img src=\"http://www.example.com/$(HTTP_COOKIE{cookieName})/hello.gif\"/ >" + "</esi:vars> end";
        TestUtils.addCookie(new BasicClientCookie("cookieName", "value"), request);
        StringBuilderWriter out = new StringBuilderWriter();
        tested.render(request, page, out);
        assertEquals("begin <img src=\"http://www.example.com/value/hello.gif\"/ > end", out.toString());
    }

    public void testQueryString() throws IOException, HttpErrorPage {
        String page = "begin <esi:vars>" + "<img src=\"http://www.example.com/$(QUERY_STRING{param1})/hello.gif\"/ >"
                + "</esi:vars> end";
        request = TestUtils.createRequest("http://localhost/?param1=param1value", provider.getDriver());
        StringBuilderWriter out = new StringBuilderWriter();
        tested.render(request, page, out);
        assertEquals("begin <img src=\"http://www.example.com/param1value/hello.gif\"/ > end", out.toString());
    }

    public void testHttpReferer() throws IOException, HttpErrorPage {
        String page = "begin <esi:vars>" + "$(HTTP_REFERER)" + "</esi:vars> end";
        request.setHeader("Referer", "http://www.example.com");
        StringBuilderWriter out = new StringBuilderWriter();
        tested.render(request, page, out);
        assertEquals("begin http://www.example.com end", out.toString());
    }

    public void testUserAgent() throws IOException, HttpErrorPage {
        String page = "begin <esi:vars>" + "$(HTTP_USER_AGENT{os})" + "</esi:vars> end";
        request.setHeader("User-Agent",
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.10) Gecko/20100914 Firefox/3.6.10 GTB7.1 "
                        + "( .NET CLR 3.5.30729)");
        StringBuilderWriter out = new StringBuilderWriter();
        tested.render(request, page, out);
        assertEquals("begin WIN end", out.toString());
    }

    public void testAcceptLanguage() throws IOException, HttpErrorPage {
        String page = "begin <esi:vars>" + "$(HTTP_ACCEPT_LANGUAGE{en-us})" + "</esi:vars> end";
        request.setHeader("Accept-Language", "en-us,en;q=0.5");
        StringBuilderWriter out = new StringBuilderWriter();
        tested.render(request, page, out);
        assertEquals("begin true end", out.toString());
    }

}
