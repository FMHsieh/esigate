package net.webassembletool.taglib;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import net.webassembletool.Context;
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
	public int doStartTag() throws JspException {
		Context.retrieveFromSession((HttpServletRequest) pageContext.getRequest());
		JspWriter out = pageContext.getOut();
		try {
			out.write("<base href=\"" + Driver.getInstance().getBaseURL() + page + "\" />");
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
}
