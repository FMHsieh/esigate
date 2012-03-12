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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.esigate.api.BaseUrlRetrieveStrategy;
import org.esigate.authentication.RemoteUserAuthenticationHandler;
import org.esigate.cache.CacheStorage;
import org.esigate.cache.DefaultCacheStorage;
import org.esigate.cookie.SerializableBasicCookieStore;
import org.esigate.renderers.ResourceFixupRenderer;
import org.esigate.url.IpHashBaseUrlRetrieveStrategy;
import org.esigate.url.RoundRobinBaseUrlRetrieveStrategy;
import org.esigate.url.SingleBaseUrlRetrieveStrategy;
import org.esigate.url.StickySessionBaseUrlRetrieveStrategy;


/**
 * Driver configuration parameters
 * 
 * @author Francois-Xavier Bonnet
 * @author Nicolas Richeton
 */
public class DriverConfiguration {
	private static final String STICKYSESSION = "stickysession";
	private static final String IPHASH = "iphash";
	private static final String ROUNDROBIN = "roundrobin";
	
	private final String instanceName;
	private String uriEncoding = "ISO-8859-1";
	private boolean fixResources = false;
	private String visibleBaseURL = null;
	private int fixMode = ResourceFixupRenderer.RELATIVE;
	private int maxConnectionsPerHost = 20;
	private int connectTimeout = 1000;
	private int socketTimeout = 10000;
	private boolean useCache = true;
	private int cacheRefreshDelay = 0;
	private int cacheMaxFileSize = 0;
	private Class<? extends CacheStorage> cacheStorageClass;
	private final String localBase;
	private boolean putInCache = false;
	private String proxyHost = null;
	private int proxyPort = 0;
	private String proxyUser = null;
	private String proxyPassword = null;
	private boolean filterJsessionid = true;
	private String authenticationHandler = RemoteUserAuthenticationHandler.class.getName();
	private final Properties properties;
	private boolean preserveHost = false;
	private String cookieStore = SerializableBasicCookieStore.class.getName();
	private String filter = null;
	private final List<String> parsableContentTypes;
	private final Set<String> blackListedHeaders;
	private final BaseUrlRetrieveStrategy baseUrlRetrieveStrategy;
	private boolean isVisibleBaseURLEmpty = true;

	private static final String DEFAULT_PARSABLE_CONTENT_TYPES = "text/html, application/xhtml+xml";
	private static final String DEFAULT_BLACK_LISTED_HEADERS = "Content-Length,Content-Encoding,Transfer-Encoding,"
			+ "Set-Cookie,Cookie,Connection,Keep-Alive,Proxy-Authenticate,Proxy-Authorization,TE,Trailers,Upgrade";


	public DriverConfiguration(String instanceName, Properties props) {
		this.instanceName = instanceName;
		// Remote application settings
		baseUrlRetrieveStrategy = getBaseUrlRetrieveSession(props);
		
		uriEncoding = getPropertyValue(props, "uriEncoding", uriEncoding);
		maxConnectionsPerHost = getPropertyValue(props, "maxConnectionsPerHost", maxConnectionsPerHost);
		int timeout = getPropertyValue(props, "timeout", 1000);
		connectTimeout = getPropertyValue(props, "connectTimeout", timeout);
		socketTimeout = getPropertyValue(props, "socketTimeout", timeout * 10);

		// Cache settings
		cacheRefreshDelay = getPropertyValue(props, "cacheRefreshDelay", cacheRefreshDelay);
		cacheMaxFileSize = getPropertyValue(props, "cacheMaxFileSize", cacheMaxFileSize);
		if (props.getProperty("cacheStorageClassName") != null) {
			String cacheStorageClassName = props.getProperty("cacheStorageClassName");
			try {
				@SuppressWarnings("unchecked")
				Class<? extends CacheStorage> cacheStorageClass = (Class<? extends CacheStorage>) this
						.getClass().getClassLoader()
						.loadClass(cacheStorageClassName);
				if (cacheStorageClass != null) {
					this.cacheStorageClass = cacheStorageClass;
				}
			} catch (Exception e) {
				throw new RuntimeException("Cachestorage instance can not be loaded", e);
			}
		}
		if (null == this.cacheStorageClass) {
			this.cacheStorageClass = DefaultCacheStorage.class;
		}

		// Local file system settings
		localBase = getPropertyValue(props, "localBase", null);
		putInCache = getPropertyValue(props, "putInCache", putInCache);

		// proxy settings
		if (props.getProperty("proxyHost") != null
				&& props.getProperty("proxyPort") != null) {
			proxyHost = getPropertyValue(props, "proxyHost", null);
			proxyPort = getPropertyValue(props, "proxyPort", 0);
			if (props.getProperty("proxyUser") != null
					&& props.getProperty("proxyPassword") != null) {
				proxyUser = getPropertyValue(props, "proxyUser", null);
				proxyPassword = getPropertyValue(props, "proxyPassword", null);
			}
		}
		useCache = getPropertyValue(props, "useCache", useCache);
		filterJsessionid = getPropertyValue(props, "filterJsessionid", filterJsessionid);

		// Authentification handler
		authenticationHandler = getPropertyValue(props, "authenticationHandler", authenticationHandler);

		// Cookie Store
		cookieStore = getPropertyValue(props, "cookieStore", cookieStore);

		// Wat Filter
		filter = getPropertyValue(props, "filter", filter);

		preserveHost = getPropertyValue(props, "preserveHost", preserveHost);

		// Fix resources
		if (props.getProperty("fixResources") != null) {
			fixResources = getPropertyValue(props, "fixResources", fixResources);
			// Fix resources mode
			if (props.getProperty("fixMode") != null) {
				if ("absolute".equalsIgnoreCase(props.getProperty("fixMode"))) {
					this.fixMode = ResourceFixupRenderer.ABSOLUTE;
				}
			}
			// Visible base url
			visibleBaseURL = getPropertyValue(props, "visibleUrlBase", null);
			isVisibleBaseURLEmpty = StringUtils.isEmpty(visibleBaseURL);
		}

		// Parsable content types
		String strContentTypes = getPropertyValue(props, "parsableContentTypes", DEFAULT_PARSABLE_CONTENT_TYPES);
		StringTokenizer tokenizer = new StringTokenizer(strContentTypes, ",");
		String contentType;
		parsableContentTypes = new ArrayList<String>();
		while (tokenizer.hasMoreElements()) {
			contentType = tokenizer.nextToken();
			contentType = contentType.trim();
			parsableContentTypes.add(contentType);
		}

		// populate headers black list
		blackListedHeaders = new HashSet<String>();
		for (String header : DEFAULT_BLACK_LISTED_HEADERS.split(",")) {
			blackListedHeaders.add(header.toLowerCase());
		}
		String headers = getPropertyValue(props, "blackListedHeaders", null);
		if (headers != null) {
			for (String header : headers.split(",")) {
				blackListedHeaders.add(header.toLowerCase());
			}
		}

		properties = props;
	}

	private BaseUrlRetrieveStrategy getBaseUrlRetrieveSession(Properties props) {
		BaseUrlRetrieveStrategy urlStrategy = null;
		String baseURLs = getPropertyValue(props, "remoteUrlBase", null);
		try {
			if (!StringUtils.isEmpty(baseURLs)) {
				String[] urls = StringUtils.split(baseURLs, ",");
				if(1 == urls.length){
					String baseURL = StringUtils.trimToEmpty(urls[0]);
					new URL(baseURL);
					urlStrategy = new SingleBaseUrlRetrieveStrategy(baseURL);
				}else if(urls.length > 0){
					String[] urlArr = new String[urls.length];
					for(int i = 0; i < urls.length; i++){
						String baseURL = StringUtils.trimToEmpty(urls[i]);
						new URL(baseURL);
						urlArr[i] = baseURL;
					}
					String strategy = StringUtils.trimToEmpty(getPropertyValue(props, "remoteUrlBaseStrategy", ROUNDROBIN));
					if(ROUNDROBIN.equalsIgnoreCase(strategy)){
						urlStrategy = new RoundRobinBaseUrlRetrieveStrategy(urlArr);
					}else if(IPHASH.equalsIgnoreCase(strategy)){
						urlStrategy = new IpHashBaseUrlRetrieveStrategy(urlArr);
					}else if(STICKYSESSION.equalsIgnoreCase(strategy)){
						urlStrategy = new StickySessionBaseUrlRetrieveStrategy(urlArr);
					}else{
						throw new ConfigurationException("No such BaseUrlRetrieveStrategy '"+ strategy+"'");
					}
					
				}
			}
		} catch (MalformedURLException e) {
			throw new ConfigurationException(e);
		}
		return urlStrategy;
	}

	private static int getPropertyValue(Properties props, String name, int defaultValue) {
		String value = props.getProperty(name);
		return value != null ? Integer.parseInt(value) : defaultValue;
	}
	private static boolean getPropertyValue(Properties props, String name, boolean defaultValue) {
		String value = props.getProperty(name);
		return value != null ? Boolean.parseBoolean(value) : defaultValue;
	}
	private static String getPropertyValue(Properties props, String name, String defaultValue) {
		return props.getProperty(name, defaultValue);
	}

	public boolean isBlackListed(String headerName) {
		if (headerName == null || headerName.length() == 0) {
			return true;
		}
		return blackListedHeaders.contains(headerName.toLowerCase());
	}

	public String getFilter() {
		return filter;
	}

	public int getFixMode() {
		return fixMode;
	}

	public boolean isFixResources() {
		return fixResources;
	}

	public String getVisibleBaseURL(String currentBaseUrl) {
		return isVisibleBaseURLEmpty ? currentBaseUrl : visibleBaseURL;
	}

	public boolean isPreserveHost() {
		return preserveHost;
	}

	public String getInstanceName() {
		return instanceName;
	}

	

	public int getMaxConnectionsPerHost() {
		return maxConnectionsPerHost;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public boolean isUseCache() {
		return useCache;
	}

	public int getCacheRefreshDelay() {
		return cacheRefreshDelay;
	}

	public int getCacheMaxFileSize() {
		return cacheMaxFileSize;
	}

	public String getLocalBase() {
		return localBase;
	}

	public boolean isPutInCache() {
		return putInCache;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}
	
	public String getProxyUser(){
		return proxyUser;
	}
	
	public String getProxyPassword(){
		return proxyPassword;
	}

	public String getUriEncoding() {
		return uriEncoding;
	}

	public boolean isFilterJsessionid() {
		return filterJsessionid;
	}

	public String getAuthenticationHandler() {
		return authenticationHandler;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setCookieStore(String cookieStore) {
		this.cookieStore = cookieStore;
	}

	public String getCookieStore() {
		return cookieStore;
	}

	public Class<? extends CacheStorage> getCacheStorageClass() {
		return cacheStorageClass;
	}

	/**
	 * List of parsable content types. Default is text/html,
	 * application/xhtml+xml
	 * 
	 * @return List of parsable content types.
	 */
	public Collection<String> getParsableContentTypes() {
		return parsableContentTypes;
	}

	public BaseUrlRetrieveStrategy getBaseUrlRetrieveStrategy() {
		return baseUrlRetrieveStrategy;
	}

}
