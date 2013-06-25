/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.esigate.taglib;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.esigate.Driver;
import org.esigate.DriverFactory;
import org.esigate.HttpErrorPage;
import org.esigate.api.BaseUrlRetrieveStrategy;
import org.esigate.regexp.ReplaceRenderer;
import org.esigate.servlet.HttpServletMediator;
import org.esigate.tags.BlockRenderer;
import org.esigate.tags.TemplateRenderer;
import org.esigate.xml.XpathRenderer;
import org.esigate.xml.XsltRenderer;

/**
 * Utility class used by all tags to access to the Driver
 * 
 * @author Francois-Xavier Bonnet
 * @author Sylvain Sicard
 */
public class DriverUtils {

	public final static String getBaseUrl(String provider, HttpRequest request) {
		BaseUrlRetrieveStrategy baseUrlRetrieveStrategy = DriverFactory.getInstance(provider).getConfiguration().getBaseUrlRetrieveStrategy();
		return baseUrlRetrieveStrategy.getBaseURL(request);
	}

	public final static void renderBlock(String provider, String page, String name, PageContext pageContext, Map<String, String> replaceRules, Map<String, String> parameters,
			boolean copyOriginalRequestParameters) throws JspException, HttpErrorPage {
		try {
			Driver driver = DriverFactory.getInstance(provider);
			HttpEntityEnclosingRequest httpRequest = new HttpServletMediator((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(),
					pageContext.getServletContext()).getHttpRequest();
			driver.render(page, parameters, pageContext.getOut(), httpRequest, new BlockRenderer(name, page), new ReplaceRenderer(replaceRules));
		} catch (IOException e) {
			throw new JspException(e);
		}
	}

	public final static void renderTemplate(String provider, String page, String name, PageContext pageContext, Map<String, String> params, Map<String, String> replaceRules,
			Map<String, String> parameters) throws JspException, HttpErrorPage {
		try {
			Driver driver = DriverFactory.getInstance(provider);
			HttpEntityEnclosingRequest httpRequest = new HttpServletMediator((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(),
					pageContext.getServletContext()).getHttpRequest();
			driver.render(page, parameters, pageContext.getOut(), httpRequest, new TemplateRenderer(name, params, page), new ReplaceRenderer(replaceRules));
		} catch (IOException e) {
			throw new JspException(e);
		}
	}

	public final static void renderXml(String provider, String source, String template, PageContext pageContext, Map<String, String> replaceRules) throws JspException, HttpErrorPage {
		try {
			Driver driver = DriverFactory.getInstance(provider);
			HttpEntityEnclosingRequest httpRequest = new HttpServletMediator((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(),
					pageContext.getServletContext()).getHttpRequest();
			driver.render(source, null, pageContext.getOut(), httpRequest, new XsltRenderer(template, httpRequest), new ReplaceRenderer(replaceRules));

		} catch (IOException e) {
			throw new JspException(e);
		}
	}

	public final static void renderXpath(String provider, String source, String xpath, PageContext pageContext, Map<String, String> replaceRules) throws JspException, HttpErrorPage {
		try {
			Driver driver = DriverFactory.getInstance(provider);
			HttpEntityEnclosingRequest httpRequest = new HttpServletMediator((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(),
					pageContext.getServletContext()).getHttpRequest();
			driver.render(source, null, pageContext.getOut(), httpRequest, new XpathRenderer(xpath), new ReplaceRenderer(replaceRules));
		} catch (IOException e) {
			throw new JspException(e);
		}
	}

	private DriverUtils() {

	}
}
