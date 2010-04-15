package net.webassembletool.authentication;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import net.webassembletool.HttpErrorPage;
import net.webassembletool.ResourceContext;
import net.webassembletool.UserContext;
import net.webassembletool.http.HttpClientRequest;
import net.webassembletool.http.HttpClientResponse;

/**
 * AuthenticationHandler implementation that retrieves the user passed by the
 * servlet container or set manually into the RequestContext and transmits it as
 * a HTTP header X_REMOTE_USER in all requests
 * 
 * @author Francois-Xavier Bonnet
 * 
 */
public class RemoteUserAuthenticationHandler implements AuthenticationHandler {

	public boolean needsNewRequest(HttpClientResponse response,
			ResourceContext requestContext) {
		return false;
	}

	public void preRequest(HttpClientRequest request,
			ResourceContext requestContext) {
		UserContext userContext = requestContext.getUserContext();
		String remoteUser = null;
		if (userContext != null && userContext.getUser() != null) {
			remoteUser = userContext.getUser();
		} else if (requestContext.getOriginalRequest().getRemoteUser() != null) {
			remoteUser = requestContext.getOriginalRequest().getRemoteUser();
		}
		if (remoteUser != null) {
			request.addHeader("X_REMOTE_USER", remoteUser);
		}
	}

	public void init(Properties properties) {
		// Nothing to do
	}

	public boolean beforeProxy(ResourceContext requestContext) {
		return true;
	}

	public void render(String src, Writer out) throws IOException,
			HttpErrorPage {
		// Just copy src to out
		out.write(src);
	}
}
