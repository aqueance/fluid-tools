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

package org.fluidity.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.spi.EmptyPackageBindings;

/**
 * @author Tibor Varga
 */
@Component(automatic = false)
final class LaunchArgumentsImpl implements LaunchArguments {

    private final List<String> arguments = new ArrayList<String>();

    public LaunchArgumentsImpl(final String[] arguments) {
        this.arguments.addAll(Arrays.asList(arguments));
    }

    public String[] arguments() {
        return arguments.toArray(new String[arguments.size()]);
    }

    /**
     * Binds the launch arguments implementation if the launch argument string array has been made available via a call to
     * <code>ContainerBoundary.setBindingProperty(LaunchArguments.ARGUMENTS_KEY, ...)</code>.
     *
     * @author Tibor Varga
     */
    public static class Bindings extends EmptyPackageBindings {

        private final String[] arguments;

        public Bindings(final Map<Object, String[]> properties) {
            arguments = properties.get(LaunchArguments.ARGUMENTS_KEY);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void bindComponents(final ComponentContainer.Registry registry) {
            if (arguments != null) {
                registry.makeChildContainer(LaunchArgumentsImpl.class).getRegistry().bindInstance(arguments);
            }
        }
    }
}
