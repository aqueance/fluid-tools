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
*#
package ${package};

import org.fluidity.composition.Component;
import org.fluidity.deployment.DeployedComponent;

/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Component
public final class MyDeployment implements DeployedComponent {

    private final ComponentApi sink;

    public MyDeployment(final ComponentApi sink) {
        this.sink = sink;
    }

    public String id() {
        return "my-deployment";
    }

    public String name() {
        return "My Deployment";
    }

    public void start(final Context observer) throws Exception {
        sink.sendText("--- Deployed component started");
    }

    public void stop() throws Exception {
        sink.sendText("--- Deployed component stopped");
    }
}
