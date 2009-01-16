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
package org.fluidity.foundation.logging;

import org.fluidity.composition.Component;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.ApplicationInfo;
import org.fluidity.foundation.Logging;

/**
 * Turns an instance of {@link org.fluidity.foundation.logging.StandardOutLogging} into a component.
 */
@Component(fallback = true)
final class DefaultLogging implements Logging {

    private final Logging delegate;

    public DefaultLogging(final @Optional ApplicationInfo appInfo) {
        this.delegate = new StandardOutLogging(appInfo == null ? null : appInfo.name());
    }

    public boolean isTraceEnabled(final Class source) {
        return delegate.isTraceEnabled(source);
    }

    public void trace(final Class source, final String message) {
        delegate.trace(source, message);
    }

    public void trace(final Class source, final String message, final Throwable exception) {
        delegate.trace(source, message, exception);
    }

    public void debug(final Class source, final String message) {
        delegate.debug(source, message);
    }

    public void debug(final Class source, final String message, final Throwable exception) {
        delegate.debug(source, message, exception);
    }

    public void info(final Class source, final String message) {
        delegate.info(source, message);
    }

    public void warning(final Class source, final String message) {
        delegate.warning(source, message);
    }

    public void warning(final Class source, final String message, final Throwable exception) {
        delegate.warning(source, message, exception);
    }

    public void error(final Class source, final String message) {
        delegate.error(source, message);
    }

    public void error(final Class source, final String message, final Throwable exception) {
        delegate.error(source, message, exception);
    }

    public void fatal(final Class source, final String message) {
        delegate.fatal(source, message);
    }

    public void fatal(final Class source, final String message, final Throwable exception) {
        delegate.fatal(source, message, exception);
    }

    public void timer(final Class source, final String message, final long beginStamp) {
        delegate.timer(source, message, beginStamp);
    }
}
