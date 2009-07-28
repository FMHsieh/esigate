package net.webassembletool.output;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.NullOutputStream;

import net.webassembletool.resource.ResourceUtils;

/**
 * TextOnlyStringOutput is a variant of string output which actually checks
 * whether content is of type text before buffering it. If no header indicates
 * whether this input is text the output is directly forwarded to binaryOutput
 * specified in construction time. For details on how text content is detected
 * look at {@link ResourceUtils#isTextContentType(String)}. The
 * {@link #hasTextBuffer()} method can be used to check whether the content
 * has been buffered. Notice that {@link #hasTextBuffer()} throws
 * IllegalStateException see its javadoc for details. Notice the nothing is done
 * in the fallback binary output until forwarding has been decided in open
 * method That is you can safley pass an output object that writes to http
 * resonse for example.
 * 
 * @author Omar BENHAMID
 */
public class TextOnlyStringOutput extends Output {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private ByteArrayOutputStream byteArrayOutputStream;
    private OutputStream outputStream;
    
    public TextOnlyStringOutput(HttpServletRequest request, HttpServletResponse response){
    	this.request = request;
    	this.response = response;
    }

    /**
     * Check whether this output has buffered text content or has forwarded it
     * to its fallback binary output considering it binary.
     * 
     * @return true if text content has been (or is beeing) buffered and false
     *         if it has been (is beeing) forwarded.
     * @throws IllegalStateException it this have not yet been decided. This
     *             happens when output is not yet opened and cann still receive
     *             more headers.
     */
    public boolean hasTextBuffer() throws IllegalStateException {
        return byteArrayOutputStream !=null;
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
			// FIXME in order to avoid NullpointerException when sending a
			// not_modified response
			outputStream = new NullOutputStream();
		} else {
			response.setStatus(getStatusCode());
			try {
				copyHeaders();
				if (ResourceUtils.isTextContentType(getHeader("Content-Type"))) {
					byteArrayOutputStream = new ByteArrayOutputStream();
				} else {
					outputStream = response.getOutputStream();
				}
			} catch (IOException e) {
				throw new OutputException(e);
			}
		}
	}

    /**
     * Copy all the headers to the response
     */
	private void copyHeaders() {
		for (Iterator<Map.Entry<Object, Object>> headersIterator = getHeaders()
				.entrySet().iterator(); headersIterator.hasNext();) {
			Map.Entry<Object, Object> entry = headersIterator.next();
			if (!"content-length".equalsIgnoreCase((String) (entry.getKey())))
				response.setHeader(entry.getKey().toString(), entry.getValue()
						.toString());
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

	/**
	 * @see net.webassembletool.output.StringOutput#getOutputStream()
	 */
	@Override
	public OutputStream getOutputStream() {
		if (byteArrayOutputStream != null)
			return byteArrayOutputStream;
		else
			return outputStream;
	}

	@Override
	public String toString() {
		if(byteArrayOutputStream == null) return "<Unparsed binary data: Content-Type=" + getHeader("Content-Type") + " >";
		String charsetName = getCharsetName();
		if (charsetName ==null) 
			charsetName = "ISO-8859-1";
		try {
			return byteArrayOutputStream.toString(charsetName);
		} catch (UnsupportedEncodingException e) {
			throw new OutputException(e);
		}
	}

}
