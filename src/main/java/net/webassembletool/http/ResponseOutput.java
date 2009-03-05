package net.webassembletool.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.webassembletool.output.Output;
import net.webassembletool.output.OutputException;

/**
 * Output implementation that simply writes to an HttpServletResponse.
 * 
 * @author Fran�ois-Xavier Bonnet
 * 
 */
public class ResponseOutput extends Output {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private OutputStream outputStream;

    public ResponseOutput(HttpServletRequest request,
	    HttpServletResponse response) {
	this.request = request;
	this.response = response;
    }

    /** {@inheritDoc} */
    @Override
    public void open() {
		String ifModifiedSince = request.getHeader("If-Modified-Since");
		String ifNoneMatch = request.getHeader("If-None-Match");
		if ((ifModifiedSince != null && ifModifiedSince
			.equals(getHeader("Last-Modified")))
			|| (ifNoneMatch != null && ifNoneMatch
				.equals(getHeader("ETag")))) {
		    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		} else {
			response.setStatus(getStatusCode());
			response.setCharacterEncoding(getCharsetName());
			try {
				copyHeaders();
				outputStream = response.getOutputStream();
			} catch (IOException e) {
				throw new OutputException(e);
			}
		}
    }

    /** {@inheritDoc} */
    @Override
    public OutputStream getOutputStream() {
    	return outputStream;
    }
    
    /**
     * Copy all the headers to the response
     */
    private void copyHeaders() {
        for (Iterator<Map.Entry<Object, Object>> headersIterator = getHeaders()
                .entrySet().iterator(); headersIterator.hasNext();) {
            Map.Entry<Object, Object> entry = headersIterator.next();
             if (!"content-length".equalsIgnoreCase(entry.getKey().toString()))
            	    response.setHeader(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
		if (outputStream != null)
			try {
				outputStream.close();
			} catch (IOException e) {
				throw new OutputException(e);
			}
    }
}
