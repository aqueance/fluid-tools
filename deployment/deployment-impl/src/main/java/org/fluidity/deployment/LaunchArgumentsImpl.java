/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
