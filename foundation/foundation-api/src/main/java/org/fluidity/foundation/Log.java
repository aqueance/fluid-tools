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

package org.fluidity.foundation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Log interface to use by Fluid Tools components. An instance is provided to your component via dependency injection. You need to specify the @{@link Source}
 * for the log messages as an annotation of the dependency on this interface. For instance:
 * <pre>
 * &#64;Component
 * public class MyComponent {
 *
 *   private final Log log;
 *
 *   public MyComponent(final &#64;Log.Source(MyComponent.class) Log log) {
 *     this.log = log;
 *   }
 * }
 * </pre>
 *
 * Message formatting conforms to the Java print format specification.
 *
 * @author Tibor Varga
 */
public interface Log {

    boolean isTraceEnabled();

    boolean isDebugEnabled();

    boolean isInfoEnabled();

    void trace(String message, Object... args);

    void debug(String message, Object... args);

    void info(String message, Object... args);

    void warning(String message, Object... args);

    void error(String message, Object... args);

    void trace(Throwable exception, String message, Object... args);

    void debug(Throwable exception, String message, Object... args);

    void info(Throwable exception, String message, Object... args);

    void warning(Throwable exception, String message, Object... args);

    void error(Throwable exception, String message, Object... args);

    /**
     * Context annotation for {@link Log} components. See that for details.
     *
     * @author Tibor Varga
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.PARAMETER, ElementType.FIELD })
    @interface Source {

        /**
         * The class to pass to the {@link org.fluidity.foundation.spi.LogFactory} implementation for the {@link Log} instance injected to the object with this
         * annotation.
         *
         * @return a {@link Class} object.
         */
        Class<?> value();
    }
}
