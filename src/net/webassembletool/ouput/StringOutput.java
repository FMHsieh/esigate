package net.webassembletool.ouput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Output implementation that writes to a String.<br />
 * StringOutput should be used only for text responses such as HTML and not for
 * binary data such as images.
 * 
 * @author Fran�ois-Xavier Bonnet
 * 
 */
public class StringOutput implements Output {
	private final static Log log = LogFactory.getLog(StringOutput.class);
	private String charset = "ISO-8859-1";
	private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	public void addHeader(String name, String value) {
		// Nothing to do
	}
	public void close() {
		// Nothing to do
	}
	public void open() {
		// Nothing to do
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	public void write(byte[] bytes, int off, int len) throws IOException {
		byteArrayOutputStream.write(bytes, off, len);
	}
	public String toString() {
		try {
			return byteArrayOutputStream.toString(charset);
		} catch (UnsupportedEncodingException e) {
			log.fatal("Encoding not supported", e);
			return null;
		}
	}
}
