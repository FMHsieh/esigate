package net.webassembletool.taglib;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import net.webassembletool.Driver;

/**
 * Retrieves an HTML fragment from the provider application and inserts it into
 * the page. Extends AbstractReplaceableTag, so a ReplaceTag can be used inside
 * this tag.
 * 
 * @author Fran�ois-Xavier Bonnet
 * 
 */
public class IncludeBlockTag extends BodyTagSupport implements IReplaceableTag,
	IParameterTag {
    private static final long serialVersionUID = 1L;
    private String name;
    private String page;
    private String provider;
    private Map<String, String> replaceRules = new HashMap<String, String>();
    private Map<String, String> parameters = new HashMap<String, String>();

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    @Override
    public int doEndTag() throws JspException {
	try {
	    Driver.getInstance(provider).renderBlock(page, name, pageContext,
		    replaceRules, parameters);
	} catch (IOException e) {
	    throw new JspException(e);
	}
	name = null;
	page = null;
	provider = null;
	replaceRules = new HashMap<String, String>();
	parameters = new HashMap<String, String>();

	return EVAL_PAGE;
    }

    public String getPage() {
	return page;
    }

    public void setPage(String page) {
	this.page = page;
    }

    public String getProvider() {
	return provider;
    }

    public void setProvider(String provider) {
	this.provider = provider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.webassembletool.taglib.IReplaceableTag#getReplaceRules()
     */
    public Map<String, String> getReplaceRules() {
	return replaceRules;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.webassembletool.taglib.IParameterTag#getParameters()
     */
    public Map<String, String> getParameters() {
	return parameters;
    }

}
