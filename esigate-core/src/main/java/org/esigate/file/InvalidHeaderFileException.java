package org.esigate.file;

/**
 * Exception to be thrown when a problem occurs with a resource.
 * 
 * @author Francois-Xavier Bonnet
 * 
 */
public class InvalidHeaderFileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidHeaderFileException(String string) {
	super(string);
    }

}
