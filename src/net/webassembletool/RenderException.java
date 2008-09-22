package net.webassembletool;

/**
 * Exception thrown when an error occured retrieving a resource
 * 
 * @author Fran�ois-Xavier Bonnet
 */
public class RenderException extends Exception {
    private static final long serialVersionUID = 1L;
    private final int statusCode;
    private final String statusMessage;
    private final String errorPageContent;

    public RenderException(int statusCode, String statusMessage,
	    String errorPageContent) {
	this.statusCode = statusCode;
	this.statusMessage = statusMessage;
	this.errorPageContent = errorPageContent;
    }

    public int getStatusCode() {
	return statusCode;
    }

    public String getStatusMessage() {
	return statusMessage;
    }

    public String getErrorPageContent() {
	return errorPageContent;
    }
}
