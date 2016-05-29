/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.deployment.osgi.isolated;

import java.util.Properties;

import org.fluidity.composition.Component;
import org.fluidity.deployment.osgi.BundleComponents;
import org.fluidity.deployment.osgi.impl.ServiceProvider;
import org.fluidity.foundation.ClassLoaders;

/**
 * @author Tibor Varga
 */
@Component(automatic = false)
public final class ServiceProviderImpl implements ServiceProvider, BundleComponents.Registration {

    public Properties properties() {
        return null;
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }

    public String callback(String className) throws Exception {
        return ((Callback) ClassLoaders.findClassLoader(getClass(), true).loadClass(className).newInstance()).invoke();
    }
}
