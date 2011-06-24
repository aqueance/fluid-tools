#*
Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)

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

import java.util.Properties;

import org.fluidity.composition.Component;
import org.fluidity.deployment.osgi.Whiteboard;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Source;

import ${package}.MessageSink;

/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Component(automatic = false)
final class MessageSinkService implements MessageSink, Whiteboard.Registration {

    private final Log log;
    private Properties properties = new Properties();

    public MessageSinkService(final @Source(MessageSinkService.class) Log log) {
        this.log = log;
        this.properties.setProperty("default", String.valueOf(true));
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }

    public Class<?>[] types() {
        return new Class<?>[] { MessageSink.class };
    }

    public Properties properties() {
        return properties;
    }

    public boolean receiveText(final String text) {
        log.info(text);
        return true;
    }
}
