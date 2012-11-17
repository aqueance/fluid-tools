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
*#package ${package}.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.fluidity.composition.Component;
import org.fluidity.deployment.osgi.BundleComponents;

import ${package}.MessageSink;
import ${package}.MessageSource;

@Component(automatic = false)
final class MessageSourceImpl implements MessageSource, BundleComponents.Registration, BundleComponents.Registration.Listener<MessageSink> {

    private final List<MessageSink> sinks = new ArrayList<MessageSink>();

    public void sendText(final String text) {
        for (final MessageSink sink : sinks) {
            sink.receiveText(text);
        }
    }

    public Class<?>[] types() {
        return new Class<?>[] { MessageSource.class };
    }

    public Properties properties() {
        return null;
    }

    public Class<MessageSink> clientType() {
        return MessageSink.class;
    }

    public void clientAdded(final MessageSink sink, final Properties properties) {
        sinks.add(sink);
    }

    public void clientRemoved(final MessageSink sink) {
        sinks.remove(sink);
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }
}
