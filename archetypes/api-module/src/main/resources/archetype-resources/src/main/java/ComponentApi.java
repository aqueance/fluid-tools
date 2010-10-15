#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

/**
 * A sample component interface.
 */
public interface ComponentApi {

    /**
     * Sends the given text to a message sink.
     *
     * @param text the text to send.
     *
     * @return <code>true</code> if the dependency accepted the text, <code>false</code> otherwise.
     */
    boolean sendText(String text);

    /**
     * A sink that accepts or rejects text.
     */
    interface MessageSink {

        /**
         * Sends a text to this sink.
         *
         * @param text the text to accept or reject.
         *
         * @return <code>true</code> if the text was accepted, <code>false</code> otherwise.
         */
        boolean receiveText(String text);
    }
}
