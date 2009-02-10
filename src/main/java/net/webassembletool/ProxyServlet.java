package net.webassembletool;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used to proxy requests from a remote application.
 * 
 * @author Fran�ois-Xavier Bonnet
 * 
 */
public class ProxyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String provider;

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String relUrl = request.getServletPath();
        if (request.getPathInfo() != null)
            relUrl += request.getPathInfo();
        boolean propagateJsessionId = response.encodeURL("/").contains(
                "jsessionid");
        DriverFactory.getInstance(provider).proxy(relUrl, request, response,
                null, propagateJsessionId);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        provider = config.getInitParameter("provider");
    }
}
