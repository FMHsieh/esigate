package net.webassembletool.resource;

/**
 * Exception to be thrown when a resource does not exist.
 * 
 * @author Fran�ois-Xavier Bonnet
 * 
 */
public class ResourceNotFoundException extends Exception {
    public ResourceNotFoundException(String relUrl) {
	super("Resource not found: " + relUrl);
    }
}
