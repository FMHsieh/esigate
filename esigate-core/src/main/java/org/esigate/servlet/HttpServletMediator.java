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

package org.esigate.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.Principal;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.BasicRequestLine;
import org.esigate.api.ContainerRequestMediator;
import org.esigate.util.HttpRequestHelper;
import org.esigate.util.UriUtils;

public class HttpServletMediator implements ContainerRequestMediator {
	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final ServletContext servletContext;
	private final HttpEntityEnclosingRequest httpRequest;

	public HttpServletMediator(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws IOException {
		this.request = request;
		this.response = response;
		this.servletContext = servletContext;
		// create request line
		String uri = UriUtils.createURI(request.getScheme(), request.getServerName(), request.getServerPort(), request.getRequestURI(), request.getQueryString(), null).toString();
		ProtocolVersion protocolVersion = BasicLineParser.parseProtocolVersion(request.getProtocol(), null);
		BasicHttpEntityEnclosingRequest result = new BasicHttpEntityEnclosingRequest(new BasicRequestLine(request.getMethod(), uri, protocolVersion));
		// copy headers
		@SuppressWarnings("rawtypes")
		Enumeration names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			@SuppressWarnings("rawtypes")
			Enumeration values = request.getHeaders(name);
			while (values.hasMoreElements()) {
				String value = (String) values.nextElement();
				result.addHeader(name, value);
			}
		}
		// create entity
		ServletInputStream inputStream = request.getInputStream();
		if (inputStream != null) {
			// Copy entity-related headers
			String contentLengthHeader = request.getHeader(HttpHeaders.CONTENT_LENGTH);
			long contentLength = (contentLengthHeader != null) ? Long.parseLong(contentLengthHeader) : -1;
			InputStreamEntity entity = new InputStreamEntity(inputStream, contentLength);
			String contentTypeHeader = request.getContentType();
			if (contentTypeHeader != null)
				entity.setContentType(contentTypeHeader);
			String contentEncodingHeader = request.getCharacterEncoding();
			if (contentEncodingHeader != null)
				entity.setContentEncoding(contentEncodingHeader);
			result.setEntity(entity);
		}
		HttpRequestHelper.setMediator(result, this);
		this.httpRequest = result;
	}

	public Cookie[] getCookies() {
		javax.servlet.http.Cookie[] src = request.getCookies();
		Cookie result[] = null;
		if (src != null) {
			result = new Cookie[src.length];
			for (int i = 0; i < src.length; i++) {
				javax.servlet.http.Cookie c = src[i];
				BasicClientCookie dest = new BasicClientCookie(c.getName(), c.getValue());
				dest.setSecure(c.getSecure());
				dest.setDomain(c.getDomain());
				dest.setPath(c.getPath());
				dest.setComment(c.getComment());
				dest.setVersion(c.getVersion());
				result[i] = dest;
			}
		}
		return result;
	}

	public void addCookie(Cookie src) {
		response.addCookie(rewriteCookie(src));
	}
	
	static javax.servlet.http.Cookie rewriteCookie(Cookie src) {
		javax.servlet.http.Cookie servletCookie = new javax.servlet.http.Cookie(src.getName(), src.getValue());

		if (src.getDomain() != null)
			servletCookie.setDomain(src.getDomain());
		servletCookie.setPath(src.getPath());
		servletCookie.setSecure(src.isSecure());
		servletCookie.setComment(src.getComment());
		servletCookie.setVersion(src.getVersion());
		if (src.getExpiryDate() != null) {
			int maxAge = (int) ((src.getExpiryDate().getTime() - System.currentTimeMillis()) / 1000);
			// According to Cookie class specification, a negative value
			// would be considered as no value. That is not what we want!
			if (maxAge < 0) {
				maxAge = 0;
			}
			servletCookie.setMaxAge(maxAge);
		}
		return servletCookie;
	}
	

	public String getRemoteAddr() {
		return request.getRemoteAddr();
	}

	public String getRemoteUser() {
		return request.getRemoteUser();
	}

	public Principal getUserPrincipal() {
		return request.getUserPrincipal();
	}

	public void sendResponse(HttpResponse httpResponse) throws IOException {
		response.setStatus(httpResponse.getStatusLine().getStatusCode());
		for (Header header : httpResponse.getAllHeaders()) {
			String name = header.getName();
			String value = header.getValue();
			response.addHeader(name, value);
		}
		HttpEntity httpEntity = httpResponse.getEntity();
		if (httpEntity != null) {
			long contentLength = httpEntity.getContentLength();
			if (contentLength > -1 && contentLength < Integer.MAX_VALUE)
				response.setContentLength((int) contentLength);
			Header contentType = httpEntity.getContentType();
			if (contentType != null)
				response.setContentType(contentType.getValue());
			Header contentEncoding = httpEntity.getContentEncoding();
			if (contentEncoding != null)
				response.setHeader(contentEncoding.getName(), contentEncoding.getValue());
			httpEntity.writeTo(response.getOutputStream());
		}
	}

	public void setSessionAttribute(String key, Serializable value) {
		HttpSession session = request.getSession();
		session.setAttribute(key, value);
	}

	public Serializable getSessionAttribute(String key) {
		HttpSession session = request.getSession(false);
		if (session == null)
			return null;
		return (Serializable) session.getAttribute(key);
	}

	public InputStream getResourceAsStream(String path) {
		return servletContext.getResourceAsStream(path);
	}

	@Deprecated
	public String getSessionId() {
		HttpSession session = request.getSession(false);
		if (session != null)
			return session.getId();
		return null;
	}

	public HttpEntityEnclosingRequest getHttpRequest() {
		return httpRequest;
	}

}
