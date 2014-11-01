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

package org.esigate.impl;

import java.util.Properties;

import junit.framework.TestCase;

import org.esigate.Parameters;

/**
 * Tests on UrlRewriter.
 * 
 * @author Francois-Xavier Bonnet
 * 
 */
public class UrlRewriterTest extends TestCase {

    public static final UrlRewriter createUrlRewriter(String visibleBaseUrl, String mode) {
        Properties properties = new Properties();
        properties.put(Parameters.VISIBLE_URL_BASE, visibleBaseUrl);
        properties.put(Parameters.FIX_MODE, mode);
        return new UrlRewriter(properties);
    }

    public void testRenderBlock1() {
        String base = "http://myapp/context";
        String page = "templates/template1.html";
        final String input =
                "  <img src=\"images/logo.png\"/> <a href=\"/context/page/page1.htm\">link</a> "
                        + "<img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputRelative =
                "  <img src=\"/context/templates/images/logo.png\"/> "
                        + "<a href=\"/context/page/page1.htm\">link</a> <img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputAbsolute =
                "  <img src=\"http://myapp/context/templates/images/logo.png\"/> "
                        + "<a href=\"http://myapp/context/page/page1.htm\">link</a> "
                        + "<img src=\"http://www.google.com/logo.com\"/>";

        UrlRewriter tested = createUrlRewriter(base, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);

        tested = createUrlRewriter(base, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputRelative, result);
    }

    /**
     * Ensure CDATA does not match replacement rules.
     * 
     * @see "https://sourceforge.net/apps/mantisbt/webassembletool/view.php?id=120"
     */
    public void testComments() {
        String base = "http://myapp/context";
        String page = "templates/template1.html";
        final String input = "<![CDATA[   var src=\"test\" ]]>";
        final String expectedOutputRelative = "<![CDATA[   var src=\"test\" ]]>";
        final String expectedOutputAbsolute = "<![CDATA[   var src=\"test\" ]]>";

        UrlRewriter tested = createUrlRewriter(base, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);

        tested = createUrlRewriter(base, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputRelative, result);
    }

    public void testUrlReplaceContext() {
        String base = "http://myapp/context/";
        String newBase = "http://myapp/newcontext/";
        String page = "templates/template1.html";
        final String input =
                "  <img src=\"images/logo.png\"/> <a href=\"/context/page/page1.htm\">link</a> "
                        + "<img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputRelative =
                "  <img src=\"/newcontext/templates/images/logo.png\"/> "
                        + "<a href=\"/newcontext/page/page1.htm\">link</a> <img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputAbsolute =
                "  <img src=\"http://myapp/newcontext/templates/images/logo.png\"/> "
                        + "<a href=\"http://myapp/newcontext/page/page1.htm\">link</a> "
                        + "<img src=\"http://www.google.com/logo.com\"/>";

        UrlRewriter tested = createUrlRewriter(newBase, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);

        tested = createUrlRewriter(newBase, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputRelative, result);
    }

    public void testUrlSanitizing() {
        String base = "http://myapp/context/";
        String page = "templates/template1.html";
        final String input =
                "  <img src=\"images/logo.png\"/> <a href=\"/context/page/page1.htm\">link</a> "
                        + "<img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputRelative =
                "  <img src=\"/context/templates/images/logo.png\"/> "
                        + "<a href=\"/context/page/page1.htm\">link</a> <img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputAbsolute =
                "  <img src=\"http://myapp/context/templates/images/logo.png\"/> "
                        + "<a href=\"http://myapp/context/page/page1.htm\">link</a> "
                        + "<img src=\"http://www.google.com/logo.com\"/>";

        UrlRewriter tested = createUrlRewriter(base, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);

        tested = createUrlRewriter(base, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputRelative, result);
    }

    public void testUrlSanitizing2() {
        String base = "http://myapp/context/";
        String visibleBase = "http://app2/";
        String page = "/page/";
        final String input =
                "  <a href=\"../styles/style.css\"/> <img src=\"images/logo.png\"/> "
                        + "<a href=\"/context/page/page1.htm\">link</a> <img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputRelative =
                "  <a href=\"/styles/style.css\"/> " + "<img src=\"/page/images/logo.png\"/>"
                        + " <a href=\"/page/page1.htm\">link</a> <img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputAbsolute =
                "  <a href=\"http://app2/styles/style.css\"/> "
                        + "<img src=\"http://app2/page/images/logo.png\"/> "
                        + "<a href=\"http://app2/page/page1.htm\">link</a> <img src=\"http://www.google.com/logo.com\"/>";

        UrlRewriter tested = createUrlRewriter(visibleBase, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);

        tested = createUrlRewriter(visibleBase, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputRelative, result);
    }

    public void testDollarSignReplacement() {
        String base = "http://myapp/context/";
        String visibleBase = "http://app2/";
        String page = "/page/";
        final String input =
                "  <a href=\"../styles/style$.css\"/> <img src=\"images/logo$.png\"/></a> "
                        + "<img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputAbsolute =
                "  <a href=\"http://app2/styles/style$.css\"/> "
                        + "<img src=\"http://app2/page/images/logo$.png\"/></a>"
                        + " <img src=\"http://www.google.com/logo.com\"/>";

        UrlRewriter tested = createUrlRewriter(visibleBase, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);
    }

    public void testCaseInsensitiveReplacement() {
        String base = "http://myapp/context/";
        String visibleBase = "http://app2/";
        String page = "/page/";
        final String input =
                "  <a HREF=\"../styles/style.css\"/> <img SrC=\"images/logo.png\"/></a> "
                        + "<img src=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputAbsolute =
                "  <a HREF=\"http://app2/styles/style.css\"/> "
                        + "<img SrC=\"http://app2/page/images/logo.png\"/></a> <img src=\"http://www.google.com/logo.com\"/>";

        UrlRewriter tested = createUrlRewriter(visibleBase, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);
    }

    public void testBackgroundReplacement() {
        String base = "http://myapp/context/";
        String visibleBase = "http://app2/";
        String page = "/page/";
        final String input =
                "  <a background=\"../styles/style.css\"/> <img background=\"images/logo.png\"/></a> "
                        + "<img background=\"http://www.google.com/logo.com\"/>";
        final String expectedOutputAbsolute =
                "  <a background=\"http://app2/styles/style.css\"/> "
                        + "<img background=\"http://app2/page/images/logo.png\"/></a> "
                        + "<img background=\"http://www.google.com/logo.com\"/>";

        UrlRewriter tested = createUrlRewriter(visibleBase, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);
    }

    /**
     * Ensures links like &lt;a href="?test=true">link&lt;a/> are correctly fixed, with both RELATIVE and ABSOLUTE
     * settings.
     * 
     */
    public void testSimpleUrlWithParamsOnly() {
        String base = "http://myapp/";
        String page = "/context/status";
        final String input = "<a href=\"?p=services\">test</a>";
        final String expectedOutputRelative = "<a href=\"/context/status?p=services\">test</a>";
        final String expectedOutputAbsolute = "<a href=\"http://myapp/context/status?p=services\">test</a>";

        // Relative test
        UrlRewriter tested = createUrlRewriter(base, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);

        // Absolute test
        tested = createUrlRewriter(base, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputRelative, result);
    }

    /**
     * Ensures links like &lt;a href="../../path">link&lt;a/> are correctly fixed with ABSOLUTE settings.
     * 
     */
    public void testSimpleUrlWithRelativePath() {
        String base = "http://myapp/";
        String page = "/context/status/page1";
        final String input = "<a href=\"../path/to/page\">test</a>";
        final String expectedOutputRelative = "<a href=\"/context/path/to/page\">test</a>";
        final String expectedOutputAbsolute = "<a href=\"http://myapp/context/path/to/page\">test</a>";

        // Relative test
        UrlRewriter tested = createUrlRewriter(base, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);

        // Absolute test
        tested = createUrlRewriter(base, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputRelative, result);
    }

    /**
     * Test for 0000186: ResourceFixup : StringIndexOutOfBoundsException: String index out of range: -1.
     * 
     * @see "https://sourceforge.net/apps/mantisbt/webassembletool/view.php?id=186"
     * 
     */
    public void testBug186() {
        String base = "http://localhost:8084/applicationPath/";
        String visible = "http://localhost:8084/";
        String page = "/";
        final String input = "<script src=\"/applicationPath/controller\"></script>";
        final String expectedOutputRelative = "<script src=\"/controller\"></script>";
        final String expectedOutputAbsolute = "<script src=\"http://localhost:8084/controller\"></script>";

        // Relative test
        UrlRewriter tested = createUrlRewriter(visible, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);

        // Absolute test
        tested = createUrlRewriter(visible, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputRelative, result);
    }

    /**
     * Test for 0000238: ResourceFixUp does not support the protocol-relative urls.
     * <p>
     * protocol-relative urls should be considered as absolute urls.
     * 
     * @see "http://sourceforge.net/apps/mantisbt/webassembletool/view.php?id=238"
     * 
     */
    public void testBug238() {
        String base = "http://localhost:8084/applicationPath/";
        String visible = "http://localhost:8084/";
        String page = "/";
        final String input = "<script src=\"//domain.com/applicationPath/controller\"></script>";
        final String expectedOutputRelative = "<script src=\"//domain.com/applicationPath/controller\"></script>";
        final String expectedOutputAbsolute = "<script src=\"//domain.com/applicationPath/controller\"></script>";

        // Relative test
        UrlRewriter tested = createUrlRewriter(visible, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputAbsolute, result);

        // Absolute test
        tested = createUrlRewriter(visible, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(expectedOutputRelative, result);
    }

    /**
     * Test for 0000286: ResourceFixupRenderer StringIndexOutOfBoundsException
     * <p>
     * Index out of range if page parameter is empty.
     * 
     */
    public void testBug286() {
        String base = "https://myapplication";
        String page = "";
        final String input = "<html></html>";

        // Relative test
        UrlRewriter tested = createUrlRewriter(base, "absolute");
        String result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(input, result);

        // Absolute test
        tested = createUrlRewriter(base, "relative");
        result = tested.rewriteHtml(input, page, base).toString();
        assertEquals(input, result);
    }

    public void testCleanUp() {
        UrlRewriter urlRewriter = createUrlRewriter("", "absolute");
        assertEquals("path/to/page", urlRewriter.cleanUpPath("path/to/page"));
        assertEquals("path/page", urlRewriter.cleanUpPath("path/to/../page"));
        assertEquals("page", urlRewriter.cleanUpPath("path/to/../../page"));

        assertEquals("http://host/page", urlRewriter.cleanUpPath("http://host/path/to/../../page"));
        assertEquals("//host/page", urlRewriter.cleanUpPath("//host/path/to/../../page"));
        assertEquals("http://host/", urlRewriter.cleanUpPath("http://host/path/to/../../page/../"));
        assertEquals("http://", urlRewriter.cleanUpPath("http://host/path/to/../../../page/../"));

        // Test bad url
        assertEquals("http://host/path/to/../../../../page/../",
                urlRewriter.cleanUpPath("http://host/path/to/../../../../page/../"));

        assertEquals("page", urlRewriter.cleanUpPath("path/../to/../page"));

        // test empty url
        assertEquals("", urlRewriter.cleanUpPath(""));

        // Test url that can't be totally cleaned
        assertEquals("path/../../page", urlRewriter.cleanUpPath("path/../../page"));

        // Test url that can't be cleaned
        assertEquals("../../path/../to/../page", urlRewriter.cleanUpPath("../../path/../to/../page"));
        assertEquals("../page", urlRewriter.cleanUpPath("../page"));
        assertEquals("../", urlRewriter.cleanUpPath("../"));

    }

}
