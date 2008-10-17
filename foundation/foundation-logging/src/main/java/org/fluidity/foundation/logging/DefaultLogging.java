package org.fluidity.foundation.logging;

import org.fluidity.composition.Component;
import org.fluidity.foundation.ApplicationInfo;
import org.fluidity.foundation.Logging;

/**
 * Turns an instance of {@link org.fluidity.foundation.logging.StandardOutLogging} into a component.
 */
@Component(fallback = true)
final class DefaultLogging implements Logging {

    private final Logging delegate;

    public DefaultLogging(final ApplicationInfo appInfo) {
        this.delegate = new StandardOutLogging(appInfo.applicationName());
    }

    public DefaultLogging() {
        this.delegate = new StandardOutLogging(null);
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
