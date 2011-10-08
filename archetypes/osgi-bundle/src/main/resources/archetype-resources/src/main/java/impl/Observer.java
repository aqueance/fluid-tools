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

import org.fluidity.deployment.osgi.BundleComponentContainer;

import ${package}.MessageSink;
import ${package}.MessageSource;

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

final class Observer implements BundleComponentContainer.Observer {

    private boolean sinkStarted;
    private boolean sourceStarted;

    private MessageSource source;

    public Class<?>[] types() {
        return new Class<?>[] { MessageSource.class, MessageSink.class };
    }

    public void started(final Class<?> type, final Object component) {
        assert !sinkStarted || !sourceStarted : type;

        sinkStarted |= type == MessageSink.class;

        if (type == MessageSource.class) {
            sourceStarted = true;
            source = (MessageSource) component;
        }

        if (sinkStarted && sourceStarted) {
            source.sendText("service observer: ready");
        }
    }

    public void stopping(final Class<?> type, final Object component) {
        assert sinkStarted || sourceStarted : type;

        if (sinkStarted && sourceStarted) {
            source.sendText("service observer: not ready");
            source = null;
        }

        if (type == MessageSource.class) sourceStarted = false;
        if (type == MessageSink.class) sinkStarted = false;
    }
}
