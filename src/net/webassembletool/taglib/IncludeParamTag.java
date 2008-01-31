package net.webassembletool.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Replaces a parameter in a template by the content of its body. This tag can
 * only be used inside an IncludeTemplate tag.
 * 
 * @author Fran�ois-Xavier Bonnet
 * 
 */
public class IncludeParamTag extends BodyTagSupport {
    private String name;

    public int doEndTag() throws JspException {
	if (getBodyContent() != null) {
	    String bodyString = getBodyContent().getString();
	    IncludeTemplateTag templateTag = (IncludeTemplateTag) getParent();
	    templateTag.getParams().put(name, bodyString);
	}
	return EVAL_PAGE;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }
}
