package net.webassembletool;

/**
 * Exception thrown when an HTML document contains WAT tags with invalid
 * arguments
 * 
 * @author Fran�ois-Xavier Bonnet
 */
public class AggregationSyntaxException extends RenderingException {
    private static final long serialVersionUID = 1L;

    /** @param string Error message */
    public AggregationSyntaxException(String string) {
        super(string);
    }

}
