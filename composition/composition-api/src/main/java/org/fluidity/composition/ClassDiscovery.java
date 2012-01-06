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

package org.fluidity.composition;

/**
 * Partially implements the <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Service_Providers">service provider</a> discovery mechanism described
 * in the <a href="http://download.oracle.com/javase/1.5.0/docs/guide/jar/jar.html#Service Provider">JAR File Specification</a>. The implementation is partial
 * because this component does not instantiate the service provider classes, it only finds them.
 * <p/>
 * The goal of this component is to find and return the list of <em>classes</em> that implement a given interface or extend a given class. To find and
 * <em>instantiate</em> those classes, use a dependency injected {@link ComponentGroup} annotated array parameter instead.
 *
 * @author Tibor Varga
 */
public interface ClassDiscovery {

    /**
     * Finds all classes visible to the given class loader that have been registered according to the service provider specification.
     *
     * @param api         the interface or class all discovered classes should implement or extend.
     * @param classLoader the class loader to use to find the classes.
     * @param strict      specifies whether to find classes directly visible to the given class loader (<code>true</code>) or indirectly via any of its parent
     *                    class loaders (<code>false</code>).
     *
     * @return a list of <code>Class</code> objects for the discovered classes.
     */
    <T> Class<T>[] findComponentClasses(Class<T> api, ClassLoader classLoader, boolean strict);
}
