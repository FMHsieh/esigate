package net.webassembletool.ouput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.webassembletool.resource.MemoryResource;

import org.apache.commons.httpclient.Header;

/**
 * Output implementation that stores the file and headers to a MemoryResource.
 * 
 * @author Fran�ois-Xavier Bonnet
 * @see MemoryResource
 * 
 */
public class MemoryOutput implements Output {
    private ArrayList<Header> headers = new ArrayList<Header>();
    private byte[] byteArray;
    private String charset;
    private ByteArrayOutputStream byteArrayOutputStream;
    private int maxSize = 0;
    private boolean tooBig = false;

    public MemoryOutput(int maxSize) {
	this.maxSize = maxSize;
    }

    public void addHeader(String name, String value) {
	headers.add(new Header(name, value));
    }

    public void close() {
	byteArray = byteArrayOutputStream.toByteArray();
	try {
	    byteArrayOutputStream.close();
	} catch (IOException e) {
	    // Should not happen
	}
	byteArrayOutputStream = null;
    }

    public void open() {
	byteArrayOutputStream = new ByteArrayOutputStream();
    }

    public void setCharset(String charset) {
	this.charset = charset;
    }

    public void write(byte[] bytes, int off, int len) throws IOException {
	if (!tooBig) {
	    byteArrayOutputStream.write(bytes, off, len);
	    tooBig = (maxSize > 0 && byteArrayOutputStream.size() > maxSize);
	}
    }

    public MemoryResource toResource() {
	if (tooBig)
	    return null;
	Header[] headersArray = new Header[headers.size()];
	for (int i = 0; i < headersArray.length; i++) {
	    headersArray[i] = headers.get(i);
	}
	return new MemoryResource(byteArray, charset, headersArray);
    }
}
