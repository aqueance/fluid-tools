#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

/**
 * A sample component interface.
 */
public interface ComponentApi {

    /**
     * Sends the given text to a dependency.
     *
     * @param text the input text to reverse.
     *
     * @return <code>true</code> if the dependency accepted the text, <code>false</code> otherwise.
     */
    boolean sendText(final String text);

    /**
     * A dependency that accepts or rejects text.
     */
    interface Dependency {

        /**
         * Sends a text to this dependency.
         *
         * @param text the text to accept or reject.
         *
         * @return <code>true</code> if the text was accepted, <code>false</code> otherwise.
         */
        boolean receiveText(final String text);
    }
}
