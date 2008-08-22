package net.webassembletool.resource;

/**
 * Exception thrown when an unsupported HTTP method is called
 * 
 * @author Fran�ois-Xavier Bonnet
 */
public class HttpMethodNotSupportedException extends ResourceException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public HttpMethodNotSupportedException(String message) {
	super(message);
    }

}
