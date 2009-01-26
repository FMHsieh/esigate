package net.webassembletool.parse;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;
import net.webassembletool.AggregationSyntaxException;
import net.webassembletool.RenderingException;
import net.webassembletool.parse.AggregateRendererRegionParser.Result;

public class AggregateRendererRegionParserTest extends TestCase {

    public void testParse1() throws AggregationSyntaxException {
        AggregateRendererRegionParser tested = new AggregateRendererRegionParser() {
            @Override
            protected Result find(String content, int pos) {
                assertEquals("content", content);
                if (pos == 0) {
                    return new Result(new MockRegion(), -1);
                } else if (pos == -1) {
                    return null;
                } else {
                    fail("unexpected position: " + pos);
                    return null;
                }
            }
        };

        List<IRegion> actual = tested.parse("content");
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertTrue(actual.get(0) instanceof MockRegion);
    }

    public void testParse() throws IOException, RenderingException {
        AggregateRendererRegionParser tested = new AggregateRendererRegionParser();
        String content = "content<!--$includeblock$token1$token2--> some text <!--$endincludeblock-->"
                + "content<!--$includetemplate$token1$token2--> some text <!--$endincludetemplate-->"
                + "content<!--$includeblock$token1$token2-->content"
                + "<!--$includeblock$token1$token2--> some text <!--$endincludeblock-->content"
                + "<esi:include src='$PROVIDER({something})/page' />content";
        List<IRegion> actual = tested.parse(content);
        assertNotNull(actual);
        assertEquals(11, actual.size());
        int i = 0;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof UnmodifiableRegion);
        checkOutput("content", actual.get(i));
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof IncludeBlockRegion);
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof UnmodifiableRegion);
        checkOutput("content", actual.get(i));
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof IncludeTemplateRegion);
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof UnmodifiableRegion);
        checkOutput("content", actual.get(i));
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof IncludeBlockRegion);
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof UnmodifiableRegion);
        checkOutput("content", actual.get(i));
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof IncludeBlockRegion);
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof UnmodifiableRegion);
        checkOutput("content", actual.get(i));
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof IncludeBlockRegion);
        i++;
        assertNotNull(actual.get(i));
        assertTrue(actual.get(i) instanceof UnmodifiableRegion);
        checkOutput("content", actual.get(i));
        i++;
        assertEquals(11, i);
    }

    private void checkOutput(String expected, IRegion region)
            throws IOException, RenderingException {
        StringWriter out = new StringWriter();
        region.process(out, null);
        assertEquals(expected, out.toString());
    }

    public void testFind() throws IOException, RenderingException {
        AggregateRendererRegionParser tested = new AggregateRendererRegionParser();
        String content = "content";

        Result actual = tested.find(content, Integer.MAX_VALUE);
        assertNull(actual);

        actual = tested.find(content, 0);
        assertNotNull(actual);
        assertEquals(content.length(), actual.getPos());
        assertTrue(actual.getRegion() instanceof UnmodifiableRegion);
        assertNull("should be one result found", tested.find(content, actual
                .getPos()));
        checkOutput("content", actual.getRegion());

        content = "content<!--$includeblock$token1$token2-->";
        actual = tested.find(content, 0);
        assertNotNull(actual);
        assertEquals("content".length(), actual.getPos());
        assertTrue(actual.getRegion() instanceof UnmodifiableRegion);
        checkOutput("content", actual.getRegion());
        actual = tested.find(content, actual.getPos());
        assertNotNull("should be two results", actual);
        assertEquals(content.length(), actual.getPos());
        assertTrue(actual.getRegion() instanceof IncludeBlockRegion);

        content = "content<esi:include src='$PROVIDER({something})/page' />";
        actual = tested.find(content, 0);
        assertNotNull(actual);
        assertEquals("content".length(), actual.getPos());
        assertTrue(actual.getRegion() instanceof UnmodifiableRegion);
        checkOutput("content", actual.getRegion());
        actual = tested.find(content, actual.getPos());
        assertNotNull("should be two results", actual);
        assertEquals(content.length(), actual.getPos());
        assertTrue(actual.getRegion() instanceof IncludeBlockRegion);

        try {
            tested.find("<!--$includesome-->", 0);
            fail("should fail with AggregationSyntaxException");
        } catch (AggregationSyntaxException e) {
            assertNotNull(e.getMessage());
            assertEquals("Invalid syntax: <includesome>", e.getMessage());
        }

        try {
            tested.find("<!--$includesome$token1$token2-->", 0);
            fail("should fail with AggregationSyntaxException");
        } catch (AggregationSyntaxException e) {
            assertNotNull(e.getMessage());
            assertEquals("Unknown tag: <includesome,token1,token2>", e
                    .getMessage());
        }

        content = "<!--$includeblock$token1$token2-->ignored<!--$endincludeblock-->";
        actual = tested.find(content, 0);
        assertNotNull(actual);
        assertEquals(content.length(), actual.getPos());
        assertTrue(actual.getRegion() instanceof IncludeBlockRegion);
        actual = tested.find(content, actual.getPos());
        assertNull("should be one result found", actual);

        content = "<!--$includetemplate$token1$token2-->ignored<!--$endincludetemplate-->";
        actual = tested.find(content, 0);
        assertNotNull(actual);
        assertEquals(content.length(), actual.getPos());
        assertTrue(actual.getRegion() instanceof IncludeTemplateRegion);
        actual = tested.find(content, actual.getPos());
        assertNull("should be one result found", actual);
    }

    private static class MockRegion implements IRegion {
        MockRegion() {
            // local
        }

        public void process(Writer out, HttpServletRequest request) {
            throw new IllegalStateException("unexpected call");
        }

    }
}
