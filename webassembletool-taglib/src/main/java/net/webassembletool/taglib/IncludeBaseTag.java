package net.webassembletool.taglib;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import net.webassembletool.vars.VariablesResolver;

/**
 * Generates an HTML "base" tag pointing to a page inside the provider
 * application.<br /> This tag is an alternative to using the ProxyServlet.
 * 
 * @author Francois-Xavier Bonnet
 * 
 */
public class IncludeBaseTag extends TagSupport {
    private static final long serialVersionUID = 1L;
    private String page;
    private String provider;

    @Override
    public int doStartTag() throws JspException {
	JspWriter out = pageContext.getOut();
	try {

            String strpage = VariablesResolver.replaceAllVariables(page);
	    out.write("<base href=\"" + DriverUtils.getBaseUrl(provider) + strpage
		    + "\" />");
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

    public String getProvider() {
	return provider;
    }

    public void setProvider(String provider) {
	this.provider = provider;
    }
}
