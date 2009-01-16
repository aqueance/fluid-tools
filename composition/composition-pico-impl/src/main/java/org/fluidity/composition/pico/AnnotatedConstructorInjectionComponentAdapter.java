/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.composition.pico;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.Optional;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.Parameter;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;
import org.picocontainer.PicoIntrospectionException;
import org.picocontainer.PicoVisitor;
import org.picocontainer.defaults.AssignabilityRegistrationException;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapter;
import org.picocontainer.defaults.NotConcreteRegistrationException;
import org.picocontainer.defaults.TooManySatisfiableConstructorsException;
import org.picocontainer.defaults.UnsatisfiableDependenciesException;

/**
 * Code mostly copied from <code>ConstructorInjectionComponentAdapter</code>, only addition is that recognition of
 *
 * @{@link org.fluidity.composition.Optional} constructor parameter annotation.
 */
public class AnnotatedConstructorInjectionComponentAdapter extends ConstructorInjectionComponentAdapter {

    private transient List<Constructor> sortedMatchingConstructors;

    public AnnotatedConstructorInjectionComponentAdapter(final Object componentKey, final Class componentImplementation)
            throws AssignabilityRegistrationException, NotConcreteRegistrationException {
        super(componentKey, componentImplementation, null, true);
    }

    private List<Constructor> getSortedMatchingConstructors() {
        List<Constructor> matchingConstructors = new ArrayList<Constructor>();
        Constructor[] allConstructors = getConstructors();

        // filter out all constructors that will definately not match
        for (Constructor constructor : allConstructors) {
            if ((parameters == null || constructor.getParameterTypes().length == parameters.length) && (
                    allowNonPublicClasses || (constructor.getModifiers() & Modifier.PUBLIC) != 0)) {
                matchingConstructors.add(constructor);
            }
        }

        // optimize list of constructors moving the longest at the beginning
        if (parameters == null) {
            Collections.sort(matchingConstructors, new Comparator<Constructor>() {
                public int compare(Constructor arg0, Constructor arg1) {
                    return arg1.getParameterTypes().length - arg0.getParameterTypes().length;
                }
            });
        }

        return matchingConstructors;
    }

    private Constructor[] getConstructors() {
        return AccessController.doPrivileged(new PrivilegedAction<Constructor[]>() {
            public Constructor[] run() {
                return getComponentImplementation().getDeclaredConstructors();
            }
        });
    }

    @Override
    protected Constructor getGreediestSatisfiableConstructor(final PicoContainer container)
            throws PicoIntrospectionException, AssignabilityRegistrationException, NotConcreteRegistrationException {
        final Set<Constructor> conflicts = new HashSet<Constructor>();
        final Set<List<Class>> unsatisfiableDependencyTypes = new HashSet<List<Class>>();

        if (sortedMatchingConstructors == null) {
            sortedMatchingConstructors = getSortedMatchingConstructors();
        }

        Constructor greediestConstructor = null;
        int lastSatisfiableConstructorSize = -1;
        Class unsatisfiedDependencyType = null;

        for (final Constructor constructor : sortedMatchingConstructors) {
            final Class[] parameterTypes = constructor.getParameterTypes();

            final Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
            final Parameter[] currentParameters = parameters != null
                    ? parameters
                    : createDefaultParameters(parameterTypes, parameterAnnotations, container);

            boolean failedDependency = false;

            // remember: all constructors with less arguments than the given parameters are filtered out already
            for (int i = 0; i < currentParameters.length; i++) {

                // check wether this constructor is statisfiable
                if (currentParameters[i].isResolvable(container, this, parameterTypes[i])) {
                    continue;
                }

                unsatisfiableDependencyTypes.add(Arrays.asList(parameterTypes));
                unsatisfiedDependencyType = parameterTypes[i];
                failedDependency = true;
                break;
            }

            if (greediestConstructor != null && parameterTypes.length != lastSatisfiableConstructorSize) {
                if (conflicts.isEmpty()) {

                    // we found our match [i.e. greedy and satisfied]
                    return greediestConstructor;
                } else {

                    // fits although not greedy
                    conflicts.add(constructor);
                }
            } else if (!failedDependency && lastSatisfiableConstructorSize == parameterTypes.length) {
                // satisfied and same size as previous one?
                conflicts.add(constructor);
                conflicts.add(greediestConstructor);
            } else if (!failedDependency) {
                greediestConstructor = constructor;
                lastSatisfiableConstructorSize = parameterTypes.length;
            }
        }

        if (!conflicts.isEmpty()) {
            throw new TooManySatisfiableConstructorsException(getComponentImplementation(), conflicts);
        } else if (greediestConstructor == null && !unsatisfiableDependencyTypes.isEmpty()) {
            throw new UnsatisfiableDependenciesException(this,
                    unsatisfiedDependencyType,
                    unsatisfiableDependencyTypes,
                    container);
        } else if (greediestConstructor == null) {

            // be nice to the user, show all constructors that were filtered out
            throw new PicoInitializationException(
                    "Either do the specified parameters not match any of the following constructors: "
                            + new HashSet<Constructor>(Arrays.asList(getConstructors())).toString()
                            + " or the constructors were not accessible for '" + getComponentImplementation() + "'");
        }

        return greediestConstructor;
    }

    private Parameter[] createDefaultParameters(final Class[] parameterTypes,
                                                final Annotation[][] parameterAnnotations,
                                                final PicoContainer container) {
        final Parameter[] parameters = super.createDefaultParameters(parameterTypes);

        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];

            if (!parameter.isResolvable(container, this, parameterTypes[i]) && isOptional(parameterAnnotations[i])) {
                parameters[i] = new OptionalParameter(parameter);
            }
        }

        return parameters;
    }

    private boolean isOptional(final Annotation[] annotations) {
        for (final Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Optional.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This is a constructor parameter that was specified as optional and that could not be resolved. This implementation resolves to <code>null</code>,
     * effectively allowing the corresponding constructor parameter to receive a <code>null</code> value.
     */
    private static class OptionalParameter implements Parameter {

        private final Parameter parameter;

        public OptionalParameter(final Parameter parameter) {
            this.parameter = parameter;
        }

        public Object resolveInstance(final PicoContainer container,
                                      final ComponentAdapter adapter,
                                      final Class expectedType) {
            return null;
        }

        public boolean isResolvable(final PicoContainer container,
                                    final ComponentAdapter adapter,
                                    final Class expectedType) {
            return true;
        }

        public void verify(final PicoContainer container, final ComponentAdapter adapter, final Class expectedType) {
            // empty
        }

        public void accept(final PicoVisitor visitor) {
            parameter.accept(visitor);
        }
    }
}
