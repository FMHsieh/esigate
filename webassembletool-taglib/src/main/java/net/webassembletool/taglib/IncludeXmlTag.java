package net.webassembletool.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import net.webassembletool.HttpErrorPage;

/**
 * Retrieves an XML fragment from the provider application and inserts it into
 * the page. May optionally evaluate XPath expressions and apply XSLT templates
 * on retrieved fragment before insert.
 * 
 * @author Stanislav Bernatskyi
 * 
 */
public class IncludeXmlTag extends TagSupport {
    private static final long serialVersionUID = 1L;
    private String source;
    private String xpath;
    private String template;
    private String provider;

	@Override
	public int doStartTag() throws JspException {
		try {
			if (xpath != null && template == null) {
				DriverUtils.renderXpath(provider, source, xpath, pageContext);
			} else if (template != null && xpath == null) {
				DriverUtils.renderXml(provider, source, template, pageContext);
			} else {
				throw new JspException(
						"One and only one of the attributes \"xpath\" or \"template\" must be defined");
			}
			return EVAL_BODY_INCLUDE;
		} catch (HttpErrorPage e) {
			throw new JspException(e);
		}
	}

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSource() {
        return source;
    }

    public String getXpath() {
        return xpath;
    }

    public String getTemplate() {
        return template;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

}
