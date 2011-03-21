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
package net.webassembletool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.webassembletool.authentication.AuthenticationHandler;
import net.webassembletool.cookie.CustomCookieStore;
import net.webassembletool.extension.ExtensionFactory;
import net.webassembletool.filter.Filter;
import net.webassembletool.http.CachedHttpResourceFactory;
import net.webassembletool.http.HttpResourceFactory;
import net.webassembletool.http.ResponseOutput;
import net.webassembletool.output.Output;
import net.webassembletool.output.StringOutput;
import net.webassembletool.output.TextOnlyStringOutput;
import net.webassembletool.regexp.ReplaceRenderer;
import net.webassembletool.renderers.ResourceFixupRenderer;
import net.webassembletool.resource.Resource;
import net.webassembletool.resource.ResourceUtils;
import net.webassembletool.tags.BlockRenderer;
import net.webassembletool.tags.TemplateRenderer;
import net.webassembletool.util.Rfc2616;
import net.webassembletool.vars.VariablesResolver;
import net.webassembletool.xml.XpathRenderer;
import net.webassembletool.xml.XsltRenderer;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class used to retrieve data from a provider application using HTTP
 * requests. Data can be retrieved as binary streams or as String for text data.
 * To improve performance, the Driver uses a cache that can be configured
 * depending on the needs.
 * 
 * @author Francois-Xavier Bonnet
 * @contributor Nicolas Richeton
 */
public class Driver {
	private static final Logger LOG = LoggerFactory.getLogger(Driver.class);
	private final DriverConfiguration config;
	private final HttpClient httpClient;
	private final AuthenticationHandler authenticationHandler;
	private final Filter filter;
	private final ResourceFactory resourceFactory;
	private final ExtensionFactory extension;

	public Driver(String name, Properties props) {
		LOG.debug("Initializing instance: " + name);
		config = new DriverConfiguration(name, props);
		// Remote application settings
		if (config.getBaseURL() != null) {
			// Create and initialize scheme registry
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			try {
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, null, null);
				SSLSocketFactory sslSocketFactory = new SSLSocketFactory(
						sslContext);
				sslSocketFactory
						.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
				Scheme https = new Scheme("https", sslSocketFactory, 443);
				schemeRegistry.register(https);
			} catch (NoSuchAlgorithmException e) {
				throw new ConfigurationException(e);
			} catch (KeyManagementException e) {
				throw new ConfigurationException(e);
			}
			schemeRegistry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			// Create an HttpClient with the ThreadSafeClientConnManager.
			// This connection manager must be used if more than one thread will
			// be using the HttpClient.
			HttpParams httpParams = new BasicHttpParams();
			httpParams.setIntParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS,
					config.getMaxConnectionsPerHost());
			httpParams.setLongParameter(ConnManagerPNames.TIMEOUT,
					config.getTimeout());
			httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
					config.getTimeout());
			httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT,
					config.getTimeout());
			httpParams.setParameter(
					ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE,
					new ConnPerRouteBean(config.getMaxConnectionsPerHost()));
			httpParams.setBooleanParameter(
					ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
			ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager(
					httpParams, schemeRegistry);
			httpClient = new DefaultHttpClient(connectionManager, httpParams);
		} else {
			httpClient = null;
		}
		// Proxy settings
		if (config.getProxyHost() != null) {
			HttpHost proxy = new HttpHost(config.getProxyHost(),
					config.getProxyPort(), "http");
			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
					proxy);
		}
		// Cache
		if (config.isUseCache()) {
			resourceFactory = new CachedHttpResourceFactory(
					getHttpResourceFactory(), config);
		} else {
			resourceFactory = getHttpResourceFactory();
		}

		extension = new ExtensionFactory(config);

		// Authentication handler
		authenticationHandler = extension
				.getExtension(AuthenticationHandler.class);
		// Filter
		Filter f = extension.getExtension(Filter.class);
		filter = (f != null) ? f : Filter.EMPTY;
	}

	protected ResourceFactory getHttpResourceFactory() {
		return new HttpResourceFactory(httpClient);
	}

	public AuthenticationHandler getAuthenticationHandler() {
		return authenticationHandler;
	}

	/**
	 * Get current user context in session or null if no context exists.
	 * 
	 * @param request
	 *            http request
	 * @param create
	 *            if true and no current user context, it will be created and
	 *            added to the session.
	 * @return UserContext or null
	 */
	public final UserContext getUserContext(HttpServletRequest request,
			boolean create) {
		UserContext context = null;
		HttpSession session = request.getSession(create);
		if (session != null) {
			String key = getContextKey();
			context = (UserContext) session.getAttribute(key);
			if (context == null && create) {
				context = createNewUserContext();
				setUserContext(context, request);
			}
		}
		return context;
	}

	/**
	 * Return a new and initialized user context.
	 * 
	 * @return UserContext
	 */
	public UserContext createNewUserContext() {
		UserContext context = new UserContext(
				extension.getExtension(CustomCookieStore.class));
		return context;
	}

	/**
	 * Add user context to session.
	 * 
	 * @param context
	 *            UserContext to save.
	 * @param request
	 *            http request.
	 */
	public final void setUserContext(UserContext context,
			HttpServletRequest request) {
		HttpSession session = request.getSession();
		session.setAttribute(getContextKey(), context);
	}

	/**
	 * Returns the base URL used to retrieve contents from the provider
	 * application.
	 * 
	 * @return the base URL as a String
	 */
	public final String getBaseURL() {
		return config.getBaseURL();
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
	 * @param originalRequest
	 *            original client request
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the block
	 */
	public final void renderXml(String source, String template, Appendable out,
			HttpServletRequest originalRequest,
			HttpServletResponse originalResponse) throws IOException,
			HttpErrorPage {
		render(source, null, out, originalRequest, originalResponse,
				new XsltRenderer(template, originalRequest.getSession()
						.getServletContext()));
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
	 * @param originalRequest
	 *            original client request
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the block
	 */
	public final void renderXpath(String source, String xpath, Appendable out,
			HttpServletRequest originalRequest,
			HttpServletResponse originalResponse) throws IOException,
			HttpErrorPage {
		render(source, null, out, originalRequest, originalResponse,
				new XpathRenderer(xpath));
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
	 * @param originalRequest
	 *            original client request
	 * @param replaceRules
	 *            the replace rules to be applied on the block
	 * @param parameters
	 *            Additional parameters
	 * @param copyOriginalRequestParameters
	 *            indicates whether the original request parameters should be
	 *            copied in the new request
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the block
	 */
	public final void renderBlock(String page, String name, Appendable writer,
			HttpServletRequest originalRequest,
			HttpServletResponse originalResponse,
			Map<String, String> replaceRules, Map<String, String> parameters,
			boolean copyOriginalRequestParameters) throws IOException,
			HttpErrorPage {
		render(page, parameters, writer, originalRequest, originalResponse,
				new BlockRenderer(name, page),
				new ReplaceRenderer(replaceRules));
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
	 * @param originalRequest
	 *            originating request object
	 * @param params
	 *            Blocks to replace inside the template
	 * @param replaceRules
	 *            The replace rules to be applied on the block
	 * @param parameters
	 *            Parameters to be added to the request
	 * @param propagateJsessionId
	 *            indicates whether <code>jsessionid</code> should be propagated
	 *            or just removed from generated output
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the template
	 */
	public final void renderTemplate(String page, String name,
			Appendable writer, HttpServletRequest originalRequest,
			HttpServletResponse originalResponse, Map<String, String> params,
			Map<String, String> replaceRules, Map<String, String> parameters,
			boolean propagateJsessionId) throws IOException, HttpErrorPage {
		render(page, parameters, writer, originalRequest, originalResponse,
				new TemplateRenderer(name, params, page), new ReplaceRenderer(
						replaceRules));
	}

	/**
	 * @param page
	 *            Address of the page containing the template
	 * @param parameters
	 *            parameters to be added to the request
	 * @param writer
	 *            Writer where to write the result
	 * @param originalRequest
	 *            originating request object
	 * @param renderers
	 *            the renderers to use to transform the output
	 * @throws IOException
	 *             If an IOException occurs while writing to the writer
	 * @throws HttpErrorPage
	 *             If an Exception occurs while retrieving the template
	 */
	public final void render(String page, Map<String, String> parameters,
			Appendable writer, HttpServletRequest originalRequest,
			HttpServletResponse response, Renderer... renderers)
			throws IOException, HttpErrorPage {
		String resultingpage = VariablesResolver.replaceAllVariables(page);
		ResourceContext resourceContext = new ResourceContext(this,
				resultingpage, parameters, originalRequest, response);
		resourceContext.setPreserveHost(config.isPreserveHost());
		StringOutput stringOutput = getResourceAsString(resourceContext);
		String currentValue = stringOutput.toString();

		// Fix resources
		if (config.isFixResources()) {
			ResourceFixupRenderer fixup = new ResourceFixupRenderer(
					config.getBaseURL(), config.getVisibleBaseURL(), page,
					config.getFixMode());
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
	public final void proxy(String relUrl, HttpServletRequest request,
			HttpServletResponse response, Renderer... renderers)
			throws IOException, HttpErrorPage {
		ResourceContext resourceContext = new ResourceContext(this, relUrl,
				null, request, response);
		request.setCharacterEncoding(config.getUriEncoding());
		resourceContext.setProxy(true);
		resourceContext.setPreserveHost(config.isPreserveHost());
		if (!authenticationHandler.beforeProxy(resourceContext)) {
			return;
		}

		if (renderers.length == 0 && !config.isFixResources()) {
			// As we don't have any transformation to apply, we don't even
			// have to retrieve the resource if it is already in browser's
			// cache. So we can use conditional a request like
			// "if-modified-since"
			resourceContext.setNeededForTransformation(false);
			renderResource(resourceContext, new ResponseOutput(response));
		} else {
			// Directly stream out non text data
			TextOnlyStringOutput textOutput = new TextOnlyStringOutput(
					response, this.config.getParsableContentTypes());
			renderResource(resourceContext, textOutput);
			// If data was binary, no text buffer is available and no rendering
			// is needed.
			if (!textOutput.hasTextBuffer()) {
				LOG.debug("'" + relUrl
						+ "' is binary : was forwarded without modification.");
				return;
			}
			LOG.debug("'" + relUrl + "' is text : will apply renderers.");
			String currentValue = textOutput.toString();

			List<Renderer> listOfRenderers = new ArrayList<Renderer>(
					renderers.length + 1);
			if (config.isFixResources()) {
				ResourceFixupRenderer fixup = new ResourceFixupRenderer(
						config.getBaseURL(), config.getVisibleBaseURL(),
						relUrl, config.getFixMode());
				listOfRenderers.add(fixup);
			}
			listOfRenderers.addAll(Arrays.asList(renderers));

			for (Renderer renderer : listOfRenderers) {
				StringWriter stringWriter = new StringWriter();
				renderer.render(resourceContext, currentValue, stringWriter);
				currentValue = stringWriter.toString();
			}
			// Write the result to the OutpuStream
			String charsetName = textOutput.getCharsetName();
			if (charsetName == null) {
				// No charset was specified in the response, we assume this is
				// ISO-8859-1
				charsetName = "ISO-8859-1";
				// We do not use the Writer because the container may add some
				// unwanted headers like default content-type that may not be
				// consistent with the original response
				response.getOutputStream().write(
						currentValue.getBytes(charsetName));
			} else {
				// Even if Content-type header has been set, some containers
				// like
				// Jetty need the charsetName to be set, if not it will take
				// default value ISO-8859-1
				response.setCharacterEncoding(charsetName);
				response.getWriter().write(currentValue);
			}
		}
	}

	private final void renderResource(ResourceContext resourceContext,
			Output output) throws HttpErrorPage {
		Resource resource = null;
		try {
			resource = this.resourceFactory.getResource(resourceContext);
			try {
				Rfc2616.renderResource(config, resource, output);
			} catch (IOException e) {
				StringWriter out = new StringWriter();
				e.printStackTrace(new PrintWriter(out));
				HttpErrorPage httpErrorPage = new HttpErrorPage(
						HttpServletResponse.SC_BAD_GATEWAY, e.getMessage(),
						out.toString());
				httpErrorPage.initCause(e);
				throw httpErrorPage;
			}
		} finally {
			if (null != resource) {
				resource.release();
			}
		}

	}

	/**
	 * This method returns the content of an url as a StringOutput. The result
	 * is cached into the request scope in order not to send several requests if
	 * you need several blocks in the same page to build the final page.
	 * 
	 * @param target
	 *            the target resource
	 * @return the content of the url
	 * @throws HttpErrorPage
	 */
	protected StringOutput getResourceAsString(ResourceContext target)
			throws HttpErrorPage {
		StringOutput result = null;
		String url = ResourceUtils.getHttpUrlWithQueryString(target);
		HttpServletRequest request = target.getOriginalRequest();
		boolean cacheable = Rfc2616.isCacheable(target);
		if (cacheable) {
			result = (StringOutput) request.getAttribute(url);
		}
		if (result == null) {
			result = new StringOutput();
			renderResource(target, result);
			if (cacheable) {
				request.setAttribute(url, result);
			}
		}
		return result;
	}

	private final String getContextKey() {
		return UserContext.class.getName() + "#" + config.getInstanceName();
	}

	/**
	 * Returns {@linkplain Filter} instance configured for this driver. Never
	 * returns <code>null</code>.
	 */
	public Filter getFilter() {
		return filter;
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
}
