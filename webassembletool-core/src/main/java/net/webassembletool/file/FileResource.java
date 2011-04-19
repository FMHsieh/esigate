package net.webassembletool.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import net.webassembletool.output.Output;
import net.webassembletool.resource.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Resource implementation pointing to a file on the local FileSystem.
 * 
 * @author Francois-Xavier Bonnet
 * 
 */
public class FileResource extends Resource {
	private File file;
	private final HeadersFile headersFile;

	public FileResource(File dataFile, File headersFile) throws IOException {
		this.file = dataFile;
		if (file.exists() && headersFile.exists()) {
			this.headersFile = FileUtils.loadHeaders(headersFile);
		} else {
			this.headersFile = new HeadersFile(404, "Not found");
		}
	}

	@Override
	public void release() {
		file = null;
	}

	@Override
	public void render(Output output) throws IOException {
		output.setStatus(headersFile.getStatusCode(), headersFile.getStatusMessage());
		for (Entry<String, List<String>> header : headersFile.getHeadersMap().entrySet()) {
			List<String> values = header.getValue();
			for (String value : values) {
				output.addHeader(header.getKey(), value);
			}
		}

		if (file != null) {
			InputStream inputStream = new FileInputStream(file);
			try {
				output.open();
				OutputStream out = output.getOutputStream();
				IOUtils.copy(inputStream, out);
			} finally {
				inputStream.close();
				output.close();
			}
		}
	}

	/**
	 * @see net.webassembletool.resource.Resource#getStatusCode()
	 */
	@Override
	public int getStatusCode() {
		return headersFile.getStatusCode();
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headersFile.getHeadersMap().keySet();
	}

	@Override
	public String getHeader(String name) {
		if (StringUtils.isEmpty(name)) {
			return null;
		}
		return headersFile.getHeader(name);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return headersFile.getHeaders(name);
	}

	@Override
	public String getStatusMessage() {
		return headersFile.getStatusMessage();
	}
}
