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
*#package ${package};

import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Source;
import org.fluidity.composition.Component;

@Component
final class EchoText implements ComponentApi.MessageSink {
    private final Log log;

    public EchoText(final @Source(EchoText.class) Log log) {
        this.log = log;
    }

    public boolean receiveText(String text) {
        log.info(text);
        return true;
    }
}
