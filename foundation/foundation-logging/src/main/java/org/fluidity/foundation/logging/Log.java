package org.fluidity.foundation.logging;

import org.fluidity.composition.ComponentContainerAccess;
import org.fluidity.foundation.Logging;

/**
 * Static access to a logging instance.
 */
public final class Log {

    private static Logging log;

    static {
        Logging log = null;

        try {
            log = new ComponentContainerAccess().getComponent(Logging.class);
        } catch (final AssertionError e) {
            // empty
        } catch (final NoClassDefFoundError e) {
            // empty
        } catch (final Exception e) {
            // empty
        }

        Log.log = log == null ? new DefaultLogging() : log;
    }

    public static boolean isTraceEnabled(final Class source) {
        assert log != null : Logging.class;
        return log.isTraceEnabled(source);
    }

    public static void trace(final Class source, final String message) {
        assert log != null : Logging.class;
        log.trace(source, message);
    }

    public static void trace(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.trace(source, message, exception);
    }

    public static void debug(final Class source, final String message) {
        assert log != null : Logging.class;
        log.debug(source, message);
    }

    public static void debug(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.debug(source, message, exception);
    }

    public static void info(final Class source, final String message) {
        assert log != null : Logging.class;
        log.info(source, message);
    }

    public static void warning(final Class source, final String message) {
        assert log != null : Logging.class;
        log.warning(source, message);
    }

    public static void warning(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.warning(source, message, exception);
    }

    public static void error(final Class source, final String message) {
        assert log != null : Logging.class;
        log.error(source, message);
    }

    public static void error(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.error(source, message, exception);
    }

    public static void fatal(final Class source, final String message) {
        assert log != null : Logging.class;
        log.fatal(source, message);
    }

    public static void fatal(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.fatal(source, message, exception);
    }

    public static void timer(final Class source, final String message, final long beginStamp) {
        assert log != null : Logging.class;
        log.timer(source, message, beginStamp);
    }
}
