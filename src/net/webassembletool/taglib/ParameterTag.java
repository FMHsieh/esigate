package net.webassembletool.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Only used inside a tag that implements IParameterTag. This tag add a request
 * parameter to the parent tag, which will be applied on render.
 * 
 * <hr>
 * 
 * @author Cedric Brandes, 27 juin 08
 */
public class ParameterTag extends BodyTagSupport {
    private static final long serialVersionUID = 1L;
    private String parameter;

    public String getParameter() {
	return parameter;
    }

    /**
     * Sets the parameter to add to the request.
     * 
     * @param expression
     */
    public void setParameter(String expression) {
	this.parameter = expression;
    }

    /**
     * Add a new request parameter to the parent tag. The request parameter is
     * the parameter parameter, the value of the parameter is the content of the
     * body
     * 
     * @see javax.servlet.jsp.tagext.BodyTagSupport#doAfterBody()
     */
    public int doAfterBody() throws JspException {
	IParameterTag parameterTag = (IParameterTag) getParent();
	String value = getBodyContent().getString();
	parameterTag.getParameters().put(parameter, value);
	return SKIP_BODY;
    }
}
