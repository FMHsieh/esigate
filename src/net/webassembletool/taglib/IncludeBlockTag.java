package net.webassembletool.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import net.webassembletool.Driver;

/**
 * Retrieves an HTML fragment from the provider application and inserts it into
 * the page.
 * 
 * @author Fran�ois-Xavier Bonnet
 * 
 */
public class IncludeBlockTag extends BodyTagSupport {
    private String name;
    private String page;
    private String provider;

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    @Override
    public int doEndTag() throws JspException {
	try {
	    Driver.getInstance(provider).renderBlock(page, name, pageContext);
	} catch (IOException e) {
	    throw new JspException(e);
	}
	return EVAL_PAGE;
    }

    public String getPage() {
	return page;
    }

    public void setPage(String page) {
	this.page = page;
    }

    String getProvider() {
	return provider;
    }

    void setProvider(String provider) {
	this.provider = provider;
    }
}
