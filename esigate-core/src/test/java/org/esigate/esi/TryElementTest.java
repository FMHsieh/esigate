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
 */

package org.esigate.esi;

import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.esigate.HttpErrorPage;
import org.esigate.MockDriver;
import org.esigate.test.MockHttpRequest;
import org.esigate.test.MockHttpResponse;

public class TryElementTest extends TestCase {

    private EsiRenderer tested;

    private MockHttpRequest request;

    @Override
    protected void setUp() throws IOException, HttpErrorPage {
        MockDriver provider = new MockDriver("mock");
        provider.addResource("/test", "test");
        provider.addResource("http://www.foo.com/test", "test");
        provider.addResource("http://www.foo.com/testFragment", "before fragment <esi:fragment name=\"fragmentFound\">FRAGMENT FOUND</esi:fragment> after fragment");
        provider.addResource("http://www.foo.com/testWithoutFragment", "no fragment here");
        request = new MockHttpRequest();
        provider.initHttpRequestParams(request, new MockHttpResponse(), null);
        tested = new EsiRenderer();
    }

    public void testTry() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>" + "<esi:attempt><esi:include src=\"http://www.foo.com/test\" /></esi:attempt>"
                + "</esi:try> end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin test end", out.toString());
    }

    public void testAttempt1() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>" + "<esi:attempt>abc <esi:include src=\"http://www.foo.com/test\" /> cba</esi:attempt>"
                + "<esi:except>inside except</esi:except>" + "</esi:try> end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin abc test cba end", out.toString());
    }

    public void testAttempt2() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>" + "<esi:attempt>abc " + "<esi:include src=\"http://www.foo.com/test\" />"
                + "<esi:include src='http://www.foo.com/not-found' onerror='continue' />"
                + " cba</esi:attempt>" + "<esi:except>inside except</esi:except>" + "</esi:try> end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin abc test cba end", out.toString());
    }

    public void testExcept1() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>" + "<esi:attempt>abc <esi:include src=\"http://www.foo2.com/test\" /> cba</esi:attempt>"
                + "<esi:except>inside except</esi:except>" + "</esi:try> end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin inside except end", out.toString());
    }

    public void testExcept2() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>"
                + "<esi:attempt> "
                + "<esi:include src='http://www.foo.com/test' /> abc <esi:include src=\"http://www.foo2.com/test\" /> cba"
                + "</esi:attempt>"
                + "<esi:except>inside except</esi:except>" + "</esi:try> end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin inside except end", out.toString());
    }

    public void testMultipleExcept() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>" + "<esi:attempt> "
                + "<esi:attempt>abc <esi:include src='http://www.foo2.com/test' /> cba</esi:attempt>"
                + "</esi:attempt>"
                + "<esi:except code='500'>inside incorrect except</esi:except>"
                + "<esi:except code='404'>inside correct except</esi:except>"
                + "<esi:except code='412'>inside incorrect except</esi:except>"
                + "<esi:except>inside default except</esi:except>" + "</esi:try> end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin inside correct except end", out.toString());
    }

    public void testDefaultExcept() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>" + "<esi:attempt> "
                + "<esi:attempt>abc <esi:include src='http://www.foo2.com/test' /> cba</esi:attempt>"
                + "</esi:attempt>"
                + "<esi:except code='500'>inside incorrect except</esi:except>"
                + "<esi:except code='412'>inside incorrect except</esi:except>"
                + "<esi:except>inside default except</esi:except>"
                + "</esi:try> end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin inside default except end", out.toString());
    }

    public void testTryCatchFragmentNotFound() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>"
                + "<esi:attempt> "
                + "<esi:attempt>abc <esi:include src='http://www.foo2.com/test' fragment='fragmentNotFound'/> cba</esi:attempt>"
                + "</esi:attempt>"
                + "<esi:except>NOT FOUND</esi:except>"
                + "</esi:try> end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin NOT FOUND end", out.toString());
    }

    public void testTryFragmentNotFound() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>"
                + "<esi:attempt> "
                + "<esi:attempt>abc<esi:include src='http://www.foo.com/testFragment' fragment='fragmentFound'/> cba</esi:attempt>"
                + "</esi:attempt>"
                + "</esi:try>end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin  abcFRAGMENT FOUND cbaend", out.toString());
    }
    
    public void testTryFragmentNotFound2() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>"
                + "<esi:attempt> "
                + "<esi:attempt>abc<esi:include src='http://www.foo.com/testWithoutFragment' fragment='fragmentFound'/> cba</esi:attempt>"
                + "</esi:attempt>"
                + "</esi:try>end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin end", out.toString());
    }
    
    public void testTryCatchFragmentNotFound2() throws IOException, HttpErrorPage {
        String page =
            "begin <esi:try>"
                + "<esi:attempt> "
                + "<esi:attempt>abc<esi:include src='http://www.foo.com/testWithoutFragment' fragment='fragmentFound'/> cba</esi:attempt>"
                + "</esi:attempt>"
                + "<esi:except>NOT FOUND</esi:except>"
                + "</esi:try>end";
        StringWriter out = new StringWriter();
        tested.render(request, page, out);
        assertEquals("begin NOT FOUNDend", out.toString());
    }
    
    
}
