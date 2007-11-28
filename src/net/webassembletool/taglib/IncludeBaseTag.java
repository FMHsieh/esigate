package net.webassembletool.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import net.webassembletool.Driver;

/**
 * Generates an HTML "base" tag pointing to a page inside the provider
 * application.<br />
 * This tag is an alternative to using the ProxyServlet.
 * 
 * @author Fran�ois-Xavier Bonnet
 * 
 */
public class IncludeBaseTag extends TagSupport {
	private String page;
	private String provider;
	public int doStartTag() throws JspException {
		JspWriter out = pageContext.getOut();
		try {
			out.write("<base href=\"" + Driver.getInstance(provider).getBaseURL() + page + "\" />");
		} catch (IOException e) {
			throw new JspException(e);
		}
		return EVAL_BODY_INCLUDE;
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
