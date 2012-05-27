package org.esigate.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collection;

public interface HttpRequest {
	/**
	 * Returns the value of a request parameter as a <code>String</code>, or <code>null</code> if the parameter does not exist. Request parameters are extra information sent with the request. For HTTP
	 * servlets, parameters are contained in the query string or posted form data.
	 * 
	 * <p>
	 * If you use this method with a multivalued parameter, the value returned is equal to the first value in the array returned by <code>getParameterValues</code>.
	 * 
	 * <p>
	 * If the parameter data was sent in the request body, such as occurs with an HTTP POST request, then reading the body directly via {@link #getInputStream} can interfere with the execution of this
	 * method.
	 * 
	 * @param name
	 *            a <code>String</code> specifying the name of the parameter
	 * 
	 * @return a <code>String</code> representing the single value of the parameter
	 * 
	 */
	public String getParameter(String name);

	/**
	 * Returns the value of the specified request header as a <code>String</code>. If the request did not include a header of the specified name, this method returns <code>null</code>. If there are
	 * multiple headers with the same name, this method returns the first head in the request. The header name is case insensitive. You can use this method with any request header.
	 * 
	 * @param name
	 *            a <code>String</code> specifying the header name
	 * 
	 * @return a <code>String</code> containing the value of the requested header, or <code>null</code> if the request does not have a header of that name
	 */
	public String getHeader(String name);

	/**
	 * Returns an enumeration of all the header names this request contains. If the request has no headers, this method returns an empty enumeration.
	 * 
	 * <p>
	 * Some servlet containers do not allow servlets to access headers using this method, in which case this method returns <code>null</code>
	 * 
	 * @return an enumeration of all the header names sent with this request; if the request has no headers, an empty enumeration; if the servlet container does not allow servlets to use this method,
	 *         <code>null</code>
	 */
	public Collection<String> getHeaderNames();

	/**
	 * Returns an array containing all of the <code>Cookie</code> objects the client sent with this request. This method returns <code>null</code> if no cookies were sent.
	 * 
	 * @return an array of all the <code>Cookies</code> included with this request, or <code>null</code> if the request has no cookies
	 */
	public Cookie[] getCookies();

	/**
	 * Returns the name of the HTTP method with which this request was made, for example, GET, POST, or PUT. Same as the value of the CGI variable REQUEST_METHOD.
	 * 
	 * @return a <code>String</code> specifying the name of the method with which this request was made
	 */
	public String getMethod();

	/**
	 * Returns the Internet Protocol (IP) address of the client or last proxy that sent the request. For HTTP servlets, same as the value of the CGI variable <code>REMOTE_ADDR</code>.
	 * 
	 * @return a <code>String</code> containing the IP address of the client that sent the request
	 */
	public String getRemoteAddr();

	/**
	 * Retrieves the body of the request as binary data using a {@link InputStream}. This method may be called to read the body, not both.
	 * 
	 * @return a {@link InputStream} object containing the body of the request
	 * 
	 * @exception IOException
	 *                if an input or output exception occurred
	 */
	public InputStream getInputStream() throws IOException;

	/**
	 * Returns the MIME type of the body of the request, or <code>null</code> if the type is not known. For HTTP servlets, same as the value of the CGI variable CONTENT_TYPE.
	 * 
	 * @return a <code>String</code> containing the name of the MIME type of the request, or null if the type is not known
	 */
	public String getContentType();

	/**
	 * Returns a boolean indicating whether this request was made using a secure channel, such as HTTPS.
	 * 
	 * @return a boolean indicating if the request was made using a secure channel
	 */
	public boolean isSecure();

	/**
	 * Returns the name of the character encoding used in the body of this request. This method returns <code>null</code> if the request does not specify a character encoding
	 * 
	 * @return a <code>String</code> containing the name of the chararacter encoding, or <code>null</code> if the request does not specify a character encoding
	 */
	public String getCharacterEncoding();

	public void setCharacterEncoding(String env) throws UnsupportedEncodingException;

	/**
	 * Returns the login of the user making this request, if the user has been authenticated, or <code>null</code> if the user has not been authenticated. Whether the user name is sent with each
	 * subsequent request depends on the browser and type of authentication. Same as the value of the CGI variable REMOTE_USER.
	 * 
	 * @return a <code>String</code> specifying the login of the user making this request, or <code>null</code> if the user login is not known
	 */
	public String getRemoteUser();

	/**
	 * Returns the value of the named attribute as an <code>Object</code>, or <code>null</code> if no attribute of the given name exists.
	 * 
	 * <p>
	 * Attributes can be set two ways. The servlet container may set attributes to make available custom information about a request. Attributes can also be set programatically using
	 * {@link #setAttribute}. This allows information to be embedded into a request before a <code>RequestDispatcher</code> call.
	 * 
	 * <p>
	 * Attribute names should follow the same conventions as package names. This specification reserves names matching <code>java.*</code>, <code>javax.*</code>, and <code>sun.*</code>.
	 * 
	 * @param name
	 *            a <code>String</code> specifying the name of the attribute
	 * 
	 * @return an <code>Object</code> containing the value of the attribute, or <code>null</code> if the attribute does not exist
	 */
	public Object getAttribute(String name);

	/**
	 * Stores an attribute in this request. Attributes are reset between requests. This method is most often used in conjunction with <code>RequestDispatcher</code>.
	 * 
	 * <p>
	 * Attribute names should follow the same conventions as package names. Names beginning with <code>java.*</code>, <code>javax.*</code>, and <code>com.sun.*</code>, are reserved for use by Sun
	 * Microsystems.
	 * 
	 * @param name
	 *            a <code>String</code> specifying the name of the attribute
	 * 
	 * @param o
	 *            the <code>Object</code> to be stored
	 */
	public void setAttribute(String name, Object o);

	/**
	 * Returns a <code>java.security.Principal</code> object containing the name of the current authenticated user. If the user has not been authenticated, the method returns <code>null</code>.
	 * 
	 * @return a <code>java.security.Principal</code> containing the name of the user making this request; <code>null</code> if the user has not been authenticated
	 */
	public java.security.Principal getUserPrincipal();

	/**
	 * Returns the current <code>HttpSession</code> associated with this request or, if there is no current session and <code>create</code> is true, returns a new session.
	 * 
	 * <p>
	 * If <code>create</code> is <code>false</code> and the request has no valid <code>HttpSession</code>, this method returns <code>null</code>.
	 * 
	 * <p>
	 * To make sure the session is properly maintained, you must call this method before the response is committed. If the container is using cookies to maintain session integrity and is asked to
	 * create a new session when the response is committed, an IllegalStateException is thrown.
	 * 
	 * @param create
	 *            <code>true</code> to create a new session for this request if necessary; <code>false</code> to return <code>null</code> if there's no current session
	 * 
	 * @return the <code>HttpSession</code> associated with this request or <code>null</code> if <code>create</code> is <code>false</code> and the request has no valid session
	 * 
	 */
	public HttpSession getSession(boolean create);

	/**
	 * Returns time to live for resources in milliseconds
	 * 
	 * @return <code>Long</code> ttl value or <code>null</code>
	 * 
	 */
	public Long getResourceTtl();

	/**
	 * Returns indicator for storing resources in cache
	 * 
	 * @return <code>Boolean</code> indicator for storing resources in cache
	 * 
	 */
	public Boolean isNoStoreResource();

	/**
	 * Returns maximum time for fetching resources in milliseconds
	 * 
	 * @return <code>Integer</code> maxWait value
	 * 
	 */
	public Integer getFetchMaxWait();

	/**
	 * Store time to live for resources in milliseconds
	 * 
	 * @param ttl
	 *            the <code>Long</code> time to live value or <code>null</code>
	 * 
	 */
	public void setResourceTtl(Long ttl);

	/**
	 * Store indicator for storing resources in cache
	 * 
	 * @param noStore
	 *            the <code>boolean</code> indicator for storing resources in cache
	 * 
	 */
	public void setNoStoreResource(boolean noStore);

	/**
	 * Store maximum time for fetching resources in milliseconds
	 * 
	 * @param maxWait
	 *            <code>Integer</code> value
	 * 
	 */
	public void setFetchMaxWait(Integer maxWait);

	public URI getUri();
}
