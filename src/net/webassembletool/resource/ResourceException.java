/**
 * 
 */
package net.webassembletool.resource;

/**
 * Exception to be thrown when a problem occurs with a resource.
 * 
 * @author Fran�ois-Xavier Bonnet
 * 
 */
public class ResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceException(String string) {
	super(string);
    }

}
