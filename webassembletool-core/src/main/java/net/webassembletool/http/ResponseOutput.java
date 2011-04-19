package net.webassembletool.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import net.webassembletool.output.Output;
import net.webassembletool.output.OutputException;

/**
 * Output implementation that simply writes to an HttpServletResponse.
 * 
 * @author Francois-Xavier Bonnet
 * 
 */
public class ResponseOutput extends Output {
	private final HttpServletResponse response;
	private OutputStream outputStream;

	public ResponseOutput(HttpServletResponse response) {
		this.response = response;
	}

	/** {@inheritDoc} */
	@Override
	public void open() {
		response.setStatus(getStatusCode());
		try {
			copyHeaders();
			outputStream = response.getOutputStream();
		} catch (IOException e) {
			throw new OutputException(e);
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
		for (Entry<String, List<String>> entry : getHeaders().entrySet()) {
			List<String> values = entry.getValue();
			for (String value : values) {
				response.addHeader(entry.getKey(), value);
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				throw new OutputException(e);
			}
		}
	}
}
