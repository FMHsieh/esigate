package org.esigate.resource;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.esigate.output.Output;


public class NullResource extends Resource {

	@Override
	public int getStatusCode() {
		return 404;
	}

	@Override
	public void release() {
		// Nothing to do
	}

	@Override
	public void render(Output output) throws IOException {
		try {
			output.setStatus(404, "Not found");
			output.setCharsetName("ISO-8859-1");
			output.open();
		} finally {
			output.close();
		}
	}

	@Override
	public Collection<String> getHeaderNames() {
		return Collections.emptySet();
	}

	@Override
	public String getHeader(String name) {
		return null;
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return Collections.emptySet();
	}

	@Override
	public String getStatusMessage() {
		return "Not found";
	}

}
