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

package org.fluidity.foundation.spi;

import org.fluidity.foundation.logging.Log;

/**
 * This is a service provider interface for Fluid Tools. For use in client code, see to the documentation of the {@link Log} class.
 * <p/>
 * Creates source-bound {@link Log} objects. Your implementation should hook the {@link Log} interface to the respective log sink of the logging framework of
 * your choice. The prevalent implementation of this interface will be looked for as a <a href="http://download.oracle.com/javase/1.5.0/docs/guide/jar/jar.html#Service
 * Provider">JAR Service Provider</a>.
 * <p/>
 * If you use Fluid Tools composition then all you need is annotate your implementation class with org.fluidity.composition.ServiceProvider and put it in the
 * class path for it to be found and used, provided that there is no other suitable implementation in the class path. If you use more of Fluid Tools than just
 * the composition framework then you should also annotate your implementation with org.fluidity.composition.Component for components of Fluid Tools to find
 * it.
 * <p/>
 * {@link Log} objects returned by this factory are thread safe as long as the underlying log implementation is.
 *
 * @author Tibor Varga
 */
public interface LogFactory {

    /**
     * Creates a log whose output is annotated by the given marker class.
     *
     * @param marker the class to annotate log messages with.
     *
     * @return a log sink.
     */
    Log createLog(Class<?> marker);
}
