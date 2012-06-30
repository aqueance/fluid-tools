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
 * Log interface to use by dependency injected components. An instance is provided to your component via dependency injection. You must use a parameterized
 * reference as the dependency on this interface to specify the class that will be the name of the injected logger.
 * <p/>
 * Message formatting conforms to the Java print format specification.
 * <p/>
 * The injected instance will be backed by one of the popular logging frameworks; which one is used depends on the {@link
 * org.fluidity.foundation.spi.LogFactory} found in the class path. If no specific logging framework is configured, Fluid Tools provides a default factory that
 * forwards all log messages to the standard output stream.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * final class <span class="hl3">MyComponent</span> {
 *
 *   private final <span class="hl1">Log</span> log;
 *
 *   public <span class="hl3">MyComponent</span>(final <span class="hl1">Log</span>&lt;<span class="hl3">MyComponent</span>> log) {
 *     this.log = log;
 *   }
 *
 *   public void someMethod() {
 *     log.info("so it has come to this");
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings({ "UnusedDeclaration", "JavadocReference" })
public interface Log<T> {

    /**
     * Tells if TRACE level messages will be emitted or not.
     *
     * @return <code>true</code> if TRACE level messages will be emitted, <code>false</code> if not.
     */
    boolean isTraceEnabled();

    /**
     * Tells if DEBUG level messages will be emitted or not.
     *
     * @return <code>true</code> if DEBUG level messages will be emitted, <code>false</code> if not.
     */
    boolean isDebugEnabled();

    /**
     * Tells if INFO level messages will be emitted or not.
     *
     * @return <code>true</code> if INFO level messages will be emitted, <code>false</code> if not.
     */
    boolean isInfoEnabled();

    /**
     * Tells if WARNING level messages will be emitted or not.
     *
     * @return <code>true</code> if WARNING level messages will be emitted, <code>false</code> if not.
     */
    boolean isWarningEnabled();

    /**
     * Emits a TRACE level message, provided that TRACE level message emission is {@link #isTraceEnabled() permitted}.
     *
     * @param format the format parameter of a {@link String#format(String, Object...)} call.
     * @param args   the args parameter of a {@link String#format(String, Object...)} call.
     */
    void trace(String format, Object... args);

    /**
     * Emits a DEBUG level message, provided that DEBUG level message emission is {@link #isDebugEnabled() permitted}.
     *
     * @param format the format parameter of a {@link String#format(String, Object...)} call.
     * @param args   the args parameter of a {@link String#format(String, Object...)} call.
     */
    void debug(String format, Object... args);

    /**
     * Emits an INFO level message, provided that INFO level message emission is {@link #isInfoEnabled() permitted}.
     *
     * @param format the format parameter of a {@link String#format(String, Object...)} call.
     * @param args   the args parameter of a {@link String#format(String, Object...)} call.
     */
    void info(String format, Object... args);

    /**
     * Emits a WARNING level message, provided that WARNING level message emission is {@link #isWarningEnabled() permitted}.
     *
     * @param format the format parameter of a {@link String#format(String, Object...)} call.
     * @param args   the args parameter of a {@link String#format(String, Object...)} call.
     */
    void warning(String format, Object... args);

    /**
     * Emits an ERROR level message.
     *
     * @param format the format parameter of a {@link String#format(String, Object...)} call.
     * @param args   the args parameter of a {@link String#format(String, Object...)} call.
     */
    void error(String format, Object... args);

    /**
     * Emits a TRACE level message and an exception stack trace, provided that TRACE level message emission is {@link #isTraceEnabled() permitted}.
     *
     * @param exception the exception to log the stack trace of.
     * @param format    the format parameter of a {@link String#format(String, Object...)} call.
     * @param args      the args parameter of a {@link String#format(String, Object...)} call.
     */
    void trace(Throwable exception, String format, Object... args);

    /**
     * Emits a DEBUG level message and an exception stack trace, provided that DEBUG level message emission is {@link #isDebugEnabled() permitted}.
     *
     * @param exception the exception to log the stack trace of.
     * @param format    the format parameter of a {@link String#format(String, Object...)} call.
     * @param args      the args parameter of a {@link String#format(String, Object...)} call.
     */
    void debug(Throwable exception, String format, Object... args);

    /**
     * Emits an INFO level message and an exception stack trace, provided that INFO level message emission is {@link #isInfoEnabled() permitted}.
     *
     * @param exception the exception to log the stack trace of.
     * @param format    the format parameter of a {@link String#format(String, Object...)} call.
     * @param args      the args parameter of a {@link String#format(String, Object...)} call.
     */
    void info(Throwable exception, String format, Object... args);

    /**
     * Emits a WARNING level message and an exception stack trace, provided that WARNING level message emission is {@link #isWarningEnabled() permitted}.
     *
     * @param exception the exception to log the stack trace of.
     * @param format    the format parameter of a {@link String#format(String, Object...)} call.
     * @param args      the args parameter of a {@link String#format(String, Object...)} call.
     */
    void warning(Throwable exception, String format, Object... args);

    /**
     * Emits an ERROR level message and an exception stack trace.
     *
     * @param exception the exception to log the stack trace of.
     * @param format    the format parameter of a {@link String#format(String, Object...)} call.
     * @param args      the args parameter of a {@link String#format(String, Object...)} call.
     */
    void error(Throwable exception, String format, Object... args);
}
