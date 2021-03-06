#*
Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*##set( $symbol_pound = '#' )#*
*##set( $symbol_dollar = '$' )#*
*##set( $symbol_escape = '\' )#*
*#package ${package};

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
