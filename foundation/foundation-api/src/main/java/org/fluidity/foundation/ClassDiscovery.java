/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation;

/**
 * Partially implements the <a href="https://github.com/aqueance/fluid-tools/wiki/User-Guide---Overview#service-providers">service provider</a> discovery
 * mechanism described in the <a href="http://download.oracle.com/javase/1.5.0/docs/guide/jar/jar.html#Service Provider">JAR File Specification</a>. The
 * implementation is partial because this component does not instantiate the service provider classes, it only finds them.
 * <p/>
 * The goal of this component is to find and return the list of <em>classes</em> that implement a given interface or extend a given class. To find <em>and
 * instantiate</em> those classes, use a dependency injected {@link org.fluidity.composition.ComponentGroup @ComponentGroup} annotated array parameter instead.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * public final class MyComponent {
 *
 *   MyComponent(final <span class="hl1">ClassDiscovery</span> discovery) {
 *     final {@linkplain ClassLoader} loader = getClass().getClassLoader();
 *     final Class&lt;<span class="hl2">MyProvider</span>>[] classes = discovery.<span class="hl1">findComponentClasses</span>(<span class="hl2">MyProvider</span>.class, loader, false);
 *     assert classes != null : <span class="hl2">MyProvider</span>.class;
 *     &hellip;
 *   }
 *
 *   {@linkplain org.fluidity.composition.ServiceProvider @ServiceProvider}
 *   public interface <span class="hl2">MyProvider</span> {
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface ClassDiscovery {

    /**
     * Finds all classes visible to the given class loader that implement or extend the given service provider interface. Use this method to find classes for
     * standard JAR service providers or those with a corresponding {@link org.fluidity.composition.ServiceProvider @ServiceProvider} annotation.
     *
     * @param api         the interface or class all discovered classes should implement or extend.
     * @param classLoader the class loader to use to find the classes.
     * @param strict      specifies whether to find classes directly visible to the given class loader (<code>true</code>) or indirectly via any of its parent
     *                    class loaders (<code>false</code>).
     * @param <T>         the type of the given service provider interface.
     *
     * @return a list of <code>Class</code> objects for the discovered classes.
     */
    <T> Class<? extends T>[] findComponentClasses(Class<T> api, ClassLoader classLoader, boolean strict);

    /**
     * Finds all classes visible to the given class loader that implement or extend the given service provider interface. This variant can be used when the
     * service provider list has been generated without a corresponding {@link org.fluidity.composition.ServiceProvider @ServiceProvider} annotation.
     *
     * @param <T>         the type of the given service provider interface.
     *
     * @param type        the type of the service provider.
     * @param api         the interface or class all discovered classes should implement or extend.
     * @param classLoader the class loader to use to find the classes.
     * @param inherit     specifies whether the discovered classes must be assignable to <code>api</code> (<code>true</code>) or not (<code>false</code>);
     *                    checked only when <code>type</code> is not {@link org.fluidity.foundation.ServiceProviders#TYPE}.
     * @param strict      specifies whether to find classes directly visible to the given class loader (<code>true</code>) or indirectly via any of its parent
     *                    class loaders (<code>false</code>).
     * @return a list of <code>Class</code> objects for the discovered classes.
     */
    <T> Class<? extends T>[] findComponentClasses(String type, Class<T> api, ClassLoader classLoader, boolean inherit, boolean strict);
}
