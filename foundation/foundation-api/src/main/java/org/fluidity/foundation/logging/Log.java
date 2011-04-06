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

package org.fluidity.foundation.logging;

/**
 * Log interface to use by Fluid Tools components. An instance is provided to your component via dependency injection. You need to specify the @{@link Marker}
 * for the log messages as an annotation of the dependency on this interface. For instance:
 * <pre>
 * &#64;Component
 * public class MyComponent {
 *
 *   private final Log log;
 *
 *   public MyComponent(final &#64;Marker(MyComponent.class) Log log) {
 *     this.log = log;
 *   }
 * }
 * </pre>
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
}
