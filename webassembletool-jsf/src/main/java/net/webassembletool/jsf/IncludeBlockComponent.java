package net.webassembletool.jsf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.webassembletool.DriverFactory;
import net.webassembletool.HttpErrorPage;
import net.webassembletool.taglib.ReplaceableTag;

public class IncludeBlockComponent extends UIComponentBase implements
		ReplaceableTag {
	private Boolean displayErrorPage;
	private String name;
	private String page;
	private String provider;
	private Map<String, String> replaceRules = new HashMap<String, String>();

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		@SuppressWarnings("rawtypes")
		Iterator it = getChildren().iterator();
		while (it.hasNext()) {
			UIComponent child = (UIComponent) it.next();
			UIComponentUtils.renderChild(child);
			if (child instanceof ReplaceComponent) {
				ReplaceComponent rc = (ReplaceComponent) child;
				replaceRules.put(rc.getExpression(), rc.getValue());
			}
		}
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		HttpServletRequest request = (HttpServletRequest) context
				.getExternalContext().getRequest();
		HttpServletResponse response = (HttpServletResponse) context
				.getExternalContext().getResponse();
		try {
			DriverFactory.getInstance(getProvider()).renderBlock(getPage(),
					getName(), writer, request, response, replaceRules, null,
					false);
		} catch (HttpErrorPage re) {
			if (isDisplayErrorPage()) {
				writer.write(re.getMessage());
			}
		}
	}

	@Override
	public String getFamily() {
		return IncludeBlockComponent.class.getPackage().toString();
	}

	public String getName() {
		return UIComponentUtils.getParam(this, "name", name);
	}

	public String getPage() {
		return UIComponentUtils.getParam(this, "page", page);
	}

	public String getProvider() {
		return UIComponentUtils.getParam(this, "provider", provider);
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	public Map<String, String> getReplaceRules() {
		return replaceRules;
	}

	public boolean isDisplayErrorPage() {
		return UIComponentUtils.getParam(this, "displayErrorPage",
				displayErrorPage);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreState(FacesContext context, Object state) {
		Object[] values = (Object[]) state;
		super.restoreState(context, values[0]);
		page = (String) values[1];
		name = (String) values[2];
		provider = (String) values[3];
		displayErrorPage = (Boolean) values[4];
		replaceRules = (HashMap<String, String>) values[5];
	}

	@Override
	public Object saveState(FacesContext context) {
		Object[] values = new Object[6];
		values[0] = super.saveState(context);
		values[1] = page;
		values[2] = name;
		values[3] = provider;
		values[4] = displayErrorPage;
		values[5] = replaceRules;
		return values;
	}

	public void setDisplayErrorPage(boolean displayErrorPage) {
		this.displayErrorPage = displayErrorPage;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

}
