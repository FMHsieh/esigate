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

package org.esigate;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.http.HttpHeaders;
import org.apache.http.client.CookieStore;
import org.apache.http.protocol.HttpContext;
import org.esigate.api.HttpRequest;
import org.esigate.api.HttpResponse;
import org.esigate.authentication.AuthenticationHandler;
import org.esigate.cookie.CookieManager;
import org.esigate.extension.ExtensionFactory;
import org.esigate.filter.Filter;
import org.esigate.http.GenericHttpRequest;
import org.esigate.http.HttpClientHelper;
import org.esigate.http.HttpResponseUtils;
import org.esigate.http.RequestCookieStore;
import org.esigate.http.ResourceUtils;
import org.esigate.regexp.ReplaceRenderer;
import org.esigate.renderers.ResourceFixupRenderer;
import org.esigate.tags.BlockRenderer;
import org.esigate.tags.TemplateRenderer;
import org.esigate.vars.VariablesResolver;
import org.esigate.xml.XpathRenderer;
import org.esigate.xml.XsltRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class used to retrieve data from a provider application using HTTP
 * requests. Data can be retrieved as binary streams or as String for text data.
 * To improve performance, the Driver uses a cache that can be configured
 * depending on the needs.
 * 
 * @author Francois-Xavier Bonnet
 * @author Nicolas Richeton
 * @author Sylvain Sicard
 */
public class Driver {
	private static final Logger LOG = LoggerFactory.getLogger(Driver.class);
	private final DriverConfiguration config;
	private final AuthenticationHandler authenticationHandler;
	private final Filter filter;
	private final HttpClientHelper httpClientHelper;
	private final List<String> parsableContentTypes;
	private final CookieManager cookieManager;

	public Driver(String name, Properties props) {
		this(name, props, new HttpClientHelper());
		httpClientHelper.init(props);
	}

	public Driver(String name, Properties props, HttpClientHelper httpClientHelper) {
		config = new DriverConfiguration(name, props);
		this.httpClientHelper = httpClientHelper;
		// Authentication handler
		authenticationHandler = ExtensionFactory.getExtension(props, Parameters.AUTHENTICATION_HANDLER, AuthenticationHandler.class);
		// Filter
		Filter f = ExtensionFactory.getExtension(props, Parameters.FILTER, Filter.class);
		filter = (f != null) ? f : Filter.EMPTY;
		parsableContentTypes = new ArrayList<String>();
		String strContentTypes = Parameters.PARSABLE_CONTENT_TYPES.getValueString(props);
		StringTokenizer tokenizer = new StringTokenizer(strContentTypes, ",");
		String contentType;
		while (tokenizer.hasMoreElements()) {
			contentType = tokenizer.nextToken();
			contentType = contentType.trim();
			parsableContentTypes.add(contentType);
		}
		cookieManager = ExtensionFactory.getExtension(props, Parameters.COOKIE_MANAGER, CookieManager.class);
	}

	/**
	 * Get current user context in session or request. Context will be saved to
	 * session only if not empty.
	 * 
	 * @param httpRequest
	 *            http request
	 * 
	 * @return UserContext
	 */
	public final UserContext getUserContext(HttpRequest httpRequest) {
		return new UserContext(httpRequest, config.getInstanceName());
	}

	/**
	 * Retrieves a page from the provider application, evaluates XPath
	 * expression if exists, applies XSLT transformation and writes result to a
	 * Writer.
	 * 
	 * @param source
	 *            external page used for inclusion
	 * @param template
	 *            path to the XSLT template (may be <code>null</code>) will be
	 *            evaluated against current web application context
	 * @param out
	 *            Writer to write the block to
	 * @param request
	 *            original client request
	 * @param response 
	 * @param replaceRules
	 *            the replace rules to be applied on the block
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the block
	 */
	public final void renderXml(String source, String template, Appendable out, HttpRequest request, HttpResponse response, Map<String, String> replaceRules) throws IOException, HttpErrorPage {
		LOG.info("renderXml provider=" + config.getInstanceName() + " source=" + source + " template=" + template);
		render(source, null, out, request, response, new XsltRenderer(template, request), new ReplaceRenderer(replaceRules));
	}

	/**
	 * Retrieves a page from the provider application, evaluates XPath
	 * expression if exists, applies XSLT transformation and writes result to a
	 * Writer.
	 * 
	 * @param source
	 *            external page used for inclusion
	 * @param xpath
	 *            XPath expression (may be <code>null</code>)
	 * @param out
	 *            Writer to write the block to
	 * @param request
	 *            original client request
	 * @param response 
	 * @param replaceRules
	 *            the replace rules to be applied on the block
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the block
	 */
	public final void renderXpath(String source, String xpath, Appendable out, HttpRequest request, HttpResponse response, Map<String, String> replaceRules) throws IOException, HttpErrorPage {
		LOG.info("renderXpath provider=" + config.getInstanceName() + " source=" + source + " xpath=" + xpath);
		render(source, null, out, request, response, new XpathRenderer(xpath), new ReplaceRenderer(replaceRules));
	}

	/**
	 * Retrieves a block from the provider application and writes it to a
	 * Writer. Block can be defined in the provider application using HTML
	 * comments.<br />
	 * eg: a block name "myblock" should be delimited with
	 * "&lt;!--$beginblock$myblock$--&gt;" and "&lt;!--$endblock$myblock$--&gt;
	 * 
	 * @param page
	 *            Page containing the block
	 * @param name
	 *            Name of the block
	 * @param writer
	 *            Writer to write the block to
	 * @param request
	 *            original client request
	 * @param response 
	 * @param replaceRules
	 *            the replace rules to be applied on the block
	 * @param parameters
	 *            Additional parameters
	 * @param copyOriginalRequestParameters
	 *            indicates whether the original request parameters should be
	 *            copied in the new request
	 * @return {@link ResourceContext}
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the block
	 */
	public final ResourceContext renderBlock(String page, String name, Appendable writer, HttpRequest request, HttpResponse response, Map<String, String> replaceRules, Map<String, String> parameters,
			boolean copyOriginalRequestParameters) throws IOException, HttpErrorPage {
		LOG.info("renderBlock provider=" + config.getInstanceName() + " page=" + page + " name=" + name);
		return render(page, parameters, writer, request, response, new BlockRenderer(name, page), new ReplaceRenderer(replaceRules));
	}

	/**
	 * Retrieves a template from the provider application and renders it to the
	 * writer replacing the parameters with the given map. If "name" param is
	 * null, the whole page will be used as the template.<br />
	 * eg: The template "mytemplate" can be delimited in the provider page by
	 * comments "&lt;!--$begintemplate$mytemplate$--&gt;" and
	 * "&lt;!--$endtemplate$mytemplate$--&gt;".<br />
	 * Inside the template, the parameters can be defined by comments.<br />
	 * eg: parameter named "myparam" should be delimited by comments
	 * "&lt;!--$beginparam$myparam$--&gt;" and "&lt;!--$endparam$myparam$--&gt;"
	 * 
	 * @param page
	 *            Address of the page containing the template
	 * @param name
	 *            Template name
	 * @param writer
	 *            Writer where to write the result
	 * @param request
	 *            originating request object
	 * @param response 
	 * @param params
	 *            Blocks to replace inside the template
	 * @param replaceRules
	 *            The replace rules to be applied on the block
	 * @param parameters
	 *            Parameters to be added to the request
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the template
	 */
	public final void renderTemplate(String page, String name, Appendable writer, HttpRequest request, HttpResponse response, Map<String, String> params, Map<String, String> replaceRules,
			Map<String, String> parameters) throws IOException, HttpErrorPage {
		LOG.info("renderTemplate provider=" + config.getInstanceName() + " page=" + page + " name=" + name);
		render(page, parameters, writer, request, response, new TemplateRenderer(name, params, page), new ReplaceRenderer(replaceRules));
	}

	public final void renderEsi(String page, Appendable writer, HttpRequest request, HttpResponse response) throws IOException, HttpErrorPage {
		render(page, null, writer, request, response);
	}

	public final void render(String page, Appendable writer, ResourceContext parent, Renderer... renderers) throws IOException, HttpErrorPage {
		render(page, parent.getParameters(), writer, parent.getOriginalRequest(), parent.getOriginalResponse(), renderers);
	}

	/**
	 * @param page
	 *            Address of the page containing the template
	 * @param parameters
	 *            parameters to be added to the request
	 * @param writer
	 *            Writer where to write the result
	 * @param request
	 *            originating request object
	 * @param renderers
	 *            the renderers to use to transform the output
	 * @return {@link ResourceContext}
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the template
	 */
	private final ResourceContext render(String page, Map<String, String> parameters, Appendable writer, HttpRequest request, HttpResponse response, Renderer... renderers) throws IOException,
			HttpErrorPage {
		if (LOG.isInfoEnabled()) {
			List<String> rendererNames = new ArrayList<String>(renderers.length);
			for (Renderer renderer : renderers) {
				rendererNames.add(renderer.getClass().getName());
			}
			LOG.info("render provider={} page= {} renderers={}", new Object[] { config.getInstanceName(), page, rendererNames });
		}
		String resultingpage = VariablesResolver.replaceAllVariables(page, request);
		ResourceContext resourceContext = new ResourceContext(this, resultingpage, parameters, request, response);
		String currentValue = getResourceAsString(resourceContext);

		// Fix resources
		if (config.isFixResources()) {
			ResourceFixupRenderer fixup = new ResourceFixupRenderer(resourceContext.getBaseURL(), config.getVisibleBaseURL(resourceContext.getBaseURL()), page, config.getFixMode());
			StringWriter stringWriter = new StringWriter();
			fixup.render(resourceContext, currentValue, stringWriter);
			currentValue = stringWriter.toString();
		}

		// Process all renderers
		for (Renderer renderer : renderers) {
			StringWriter stringWriter = new StringWriter();
			renderer.render(resourceContext, currentValue, stringWriter);
			currentValue = stringWriter.toString();
		}
		writer.append(currentValue);
		return resourceContext;
	}

	/**
	 * Retrieves a resource from the provider application and transforms it
	 * using the Renderer passed as a parameter.
	 * 
	 * @param relUrl
	 *            the relative URL to the resource
	 * @param request
	 *            the request
	 * @param response
	 *            the response
	 * @param renderers
	 *            the renderers to use to transform the output
	 * @throws IOException
	 *             If an IOException occurs while writing to the response
	 * @throws HttpErrorPage
	 *             If the page contains incorrect tags
	 */
	public void proxy(String relUrl, HttpRequest request, HttpResponse response, Renderer... renderers) throws IOException, HttpErrorPage {
		LOG.info("proxy provider={} relUrl={}", config.getInstanceName(), relUrl);

		ResourceContext resourceContext = new ResourceContext(this, relUrl, null, request, response);
		request.setCharacterEncoding(config.getUriEncoding());
		if (!authenticationHandler.beforeProxy(resourceContext)) {
			return;
		}
		HttpRequest originalRequest = resourceContext.getOriginalRequest();
		String url = ResourceUtils.getHttpUrlWithQueryString(resourceContext, true);
		GenericHttpRequest httpRequest = httpClientHelper.createHttpRequest(originalRequest, url, true);
		org.apache.http.HttpResponse httpResponse = execute(httpRequest, resourceContext);
		if (!isTextContentType(httpResponse)) {
			LOG.debug("'" + relUrl + "' is binary on no transformation to apply: was forwarded without modification.");
			httpClientHelper.render(httpResponse, response, originalRequest, httpRequest);
		} else {
			LOG.debug("'" + relUrl + "' is text : will apply renderers.");
			String currentValue = HttpResponseUtils.toString(httpResponse);

			List<Renderer> listOfRenderers = new ArrayList<Renderer>(renderers.length + 1);
			if (config.isFixResources()) {
				ResourceFixupRenderer fixup = new ResourceFixupRenderer(resourceContext.getBaseURL(), config.getVisibleBaseURL(resourceContext.getBaseURL()), relUrl, config.getFixMode());
				listOfRenderers.add(fixup);
			}
			listOfRenderers.addAll(Arrays.asList(renderers));

			for (Renderer renderer : listOfRenderers) {
				StringWriter stringWriter = new StringWriter();
				renderer.render(resourceContext, currentValue, stringWriter);
				currentValue = stringWriter.toString();
			}
			// Write the result to the OutpuStream using default charset
			// ISO-8859-1 if not defined
			String charsetName = HttpResponseUtils.getContentCharset(httpResponse);
			if (charsetName == null) {
				charsetName = "ISO-8859-1";
			}
			httpClientHelper.render(currentValue, httpResponse, response, originalRequest, httpRequest);
		}
	}

	/**
	 * This method returns the content of an url as a StringOutput. The result
	 * is cached into the request scope in order not to send several requests if
	 * you need several blocks in the same page to build the final page.
	 * 
	 * @param context
	 *            the target resource
	 * @return the content of the url
	 * @throws HttpErrorPage
	 * @throws IOException
	 */
	protected String getResourceAsString(ResourceContext context) throws HttpErrorPage, IOException {
		String result;
		String url = ResourceUtils.getHttpUrlWithQueryString(context, false);
		org.esigate.api.HttpRequest request = context.getOriginalRequest();
		boolean cacheable = "GET".equalsIgnoreCase(context.getOriginalRequest().getMethod());
		if (cacheable) {
			result = (String) request.getAttribute(url);
			if (result != null)
				return result;
		}
		HttpRequest originalRequest = context.getOriginalRequest();
		GenericHttpRequest httpRequest = httpClientHelper.createHttpRequest(originalRequest, url, false);
		org.apache.http.HttpResponse httpResponse = execute(httpRequest, context);
		result = HttpResponseUtils.toString(httpResponse);
		if (cacheable) {
			request.setAttribute(url, result);
		}
		return result;
	}

	/**
	 * This method returns the content of an url as a String. The result is
	 * cached into the request scope in order not to send several requests if
	 * you need several blocks in the same page to build the final page.
	 * 
	 * @param page
	 *            Address of the page containing the template
	 * @param ctx
	 *            target resource
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the template
	 * @return the content of the url
	 * @throws HttpErrorPage
	 * @throws IOException
	 */
	public String getResourceAsString(String page, ResourceContext ctx) throws HttpErrorPage, IOException {
		String actualPage = VariablesResolver.replaceAllVariables(page, ctx.getOriginalRequest());
		ResourceContext resourceContext = new ResourceContext(this, actualPage, null, ctx.getOriginalRequest(), ctx.getOriginalResponse());
		String currentValue = getResourceAsString(resourceContext);
		return currentValue;

	}

	/**
	 * Get current driver configuration.
	 * <p>
	 * This method is not intended to get a WRITE access to the configuration.
	 * <p>
	 * This may be supported in future versions (testing is needed). For the
	 * time being, changing configuration settings after getting access through
	 * this method is <b>UNSUPPORTED</b> and <b>SHOULD NOT</b> be used.
	 * 
	 * @return current configuration
	 */
	public DriverConfiguration getConfiguration() {
		return config;
	}

	/**
	 * Check whether the given content-type value corresponds to "parsable"
	 * text.
	 * 
	 * @param httpResponse
	 *            the response to analyze depending on its content-type
	 * @return true if this represents text or false if not
	 */
	private boolean isTextContentType(org.apache.http.HttpResponse httpResponse) {
		String contentType = HttpResponseUtils.getFirstHeader(HttpHeaders.CONTENT_TYPE, httpResponse);
		return isTextContentType(contentType);
	}

	protected boolean isTextContentType(String contentType) {
		if (contentType != null) {
			String lowerContentType = contentType.toLowerCase();
			for (String textContentType : parsableContentTypes) {
				if (lowerContentType.startsWith(textContentType)) {
					return true;
				}
			}
		}
		return false;
	}

	private org.apache.http.HttpResponse executeSingleRequest(GenericHttpRequest httpRequest, HttpContext httpContext, ResourceContext resourceContext) {
		org.apache.http.HttpResponse result;
		filter.preRequest(httpRequest, httpContext, resourceContext);
		authenticationHandler.preRequest(httpRequest, resourceContext);
		long start = System.currentTimeMillis();
		result = httpClientHelper.execute(httpRequest, httpContext);
		long end = System.currentTimeMillis();
		filter.postRequest(httpRequest, result, httpContext, resourceContext);
		if (LOG.isDebugEnabled()) {
			LOG.debug(httpRequest.getRequestLine().toString() + " -> " + result.getStatusLine().toString() + " (" + (end - start) + " ms)");
		}
		return result;
	}

	private org.apache.http.HttpResponse execute(GenericHttpRequest httpRequest, ResourceContext resourceContext) throws HttpErrorPage, IOException {
		CookieStore cookieStore = new RequestCookieStore(cookieManager, resourceContext);
		HttpContext httpContext = httpClientHelper.createHttpContext(cookieStore);
		org.apache.http.HttpResponse httpResponse = executeSingleRequest(httpRequest, httpContext, resourceContext);
		while (authenticationHandler.needsNewRequest(httpResponse, resourceContext)) {
			HttpResponseUtils.release(httpResponse);
			httpResponse = executeSingleRequest(httpRequest, httpContext, resourceContext);
		}
		if (HttpResponseUtils.isError(httpResponse)) {
			throw new HttpErrorPage(httpResponse);
		}
		return httpResponse;
	}

}
