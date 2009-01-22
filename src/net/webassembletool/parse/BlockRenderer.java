package net.webassembletool.parse;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.webassembletool.RetrieveException;
import net.webassembletool.StringUtils;
import net.webassembletool.ouput.StringOutput;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Block renderer.
 * <p>
 * Extracts data between <code>&lt;!--$beginblock$myblock$--&gt;</code> and
 * <code>&lt;!--$endblock$myblock$--&gt;</code> separators
 * 
 * @author Stanislav Bernatskyi
 */
public class BlockRenderer implements Renderer {
    private final static Log LOG = LogFactory.getLog(BlockRenderer.class);

    private final String page;
    private final String name;
    private final Writer out;

    public BlockRenderer(String name, Writer out, String page) {
	this.name = name;
	this.out = out;
	this.page = page;
    }

    /** {@inheritDoc} */
    public void render(StringOutput src, Map<String, String> replaceRules)
	    throws IOException, RetrieveException {
	if (src.getStatusCode() != HttpServletResponse.SC_OK) {
	    throw new RetrieveException(src.getStatusCode(), src
		    .getStatusMessage(), src.toString());
	}

	String content = src.toString();
	if (content == null)
	    return;
	Tag openTag = Tag.find("beginblock$" + name, content);
	Tag closeTag = Tag.find("endblock$" + name, content);
	if (openTag == null || closeTag == null) {
	    LOG.warn("Block not found: page=" + page + " block=" + name);
	} else {
	    LOG.debug("Serving block: page=" + page + " block=" + name);
	    out.append(StringUtils.replace(content.substring(openTag
		    .getEndIndex(), closeTag.getBeginIndex()), replaceRules));
	}
    }

}