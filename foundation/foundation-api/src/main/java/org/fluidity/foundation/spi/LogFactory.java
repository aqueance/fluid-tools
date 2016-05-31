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

package org.fluidity.foundation.spi;

import org.fluidity.foundation.Log;

/**
 * Adapts an external logging framework to the Fluid Tools dependency injection system. This interface is used only by implementations of such an adaptor. For
 * logging in your code, see to the documentation of the {@link Log} class.
 * <p>
 * The log factory creates source-bound {@link Log} objects. Your implementation should adapt the respective log sink of the logging framework of your choice
 * to the {@link Log} interface. The prevalent implementation of this interface will be looked for as a
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Service_Provider">JAR Service Provider</a>.
 * <p>
 * You will need to use Fluid Tools to implement and build your adaptor, and annotate the implementation with both
 * {@link org.fluidity.composition.ServiceProvider @ServiceProvider} and {@link org.fluidity.composition.Component @Component}. The Java archive containing the
 * implementation must be in the class path for it to be found and used, and it must be the <i>only</i> archive in the class path that contains a {@code
 * LogFactory} implementation.
 * <p>
 * See {@link LogAdapter} for a recommended way to adapt any logging framework to Fluid Tools.
 * <p>
 * {@link Log} objects returned by this factory are thread safe as long as the underlying log implementation is.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface LogFactory {

    /**
     * Returns the class of the instantiated {@link Log} implementation.
     *
     * @return a class object; never <code>null</code>.
     */
    Class<?> type();

    /**
     * Creates a log whose output is annotated by the given class.
     *
     * @param source the class to annotate log messages with.
     * @param <T>    the type of the given <code>source</code> class to which the returned {@link Log} instance belongs.
     *
     * @return a log sink.
     */
    <T> Log<T> createLog(Class<T> source);
}
