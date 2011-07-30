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
 * Partially implements the Service Provider discovery mechanism described in the <a href="http://download.oracle.com/javase/1.5.0/docs/guide/jar/jar.html#Service
 * Provider">JAR File Specification</a>.
 * <p/>
 * The implementation is partial because this component does not instantiate the discovered classes, it merely discovers them.
 * <p/>
 * This is useful not so much for client components as for those providing core composition functionality such as component container bootstrap. Client
 * components normally need to use a {@link ComponentGroup} annotated array parameter instead.
 *
 * @author Tibor Varga
 */
public interface ClassDiscovery {

    /**
     * Finds all classes in the class path that have been registered according to the standard service discovery specification.
     *
     * @param api         the interface all discovered classes should implement.
     * @param classLoader the class loader to use to find components.
     * @param strict      specifies whether the component may be loaded by only the given class loader (<code>true</code>) or any of its parent class loaders
     *                    (<code>false</code>).
     *
     * @return a list of <code>Class</code> objects for the discovered classes.
     */
    <T> Class<T>[] findComponentClasses(Class<T> api, ClassLoader classLoader, boolean strict);
}
