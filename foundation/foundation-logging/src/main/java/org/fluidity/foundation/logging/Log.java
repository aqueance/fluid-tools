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
        } catch (AssertionError e) {
            // empty
        } catch (NoClassDefFoundError e) {
            // empty
        } catch (Exception e) {
            // empty
        }

        Log.log = log == null ? new DefaultLogging() : log;
    }

    public static boolean isTraceEnabled(Class source) {
        assert log != null : Logging.class;
        return log.isTraceEnabled(source);
    }

    public static void trace(Class source, String message) {
        assert log != null : Logging.class;
        log.trace(source, message);
    }

    public static void trace(Class source, String message, Throwable exception) {
        assert log != null : Logging.class;
        log.trace(source, message, exception);
    }

    public static void debug(Class source, String message) {
        assert log != null : Logging.class;
        log.debug(source, message);
    }

    public static void debug(Class source, String message, Throwable exception) {
        assert log != null : Logging.class;
        log.debug(source, message, exception);
    }

    public static void info(Class source, String message) {
        assert log != null : Logging.class;
        log.info(source, message);
    }

    public static void warning(Class source, String message) {
        assert log != null : Logging.class;
        log.warning(source, message);
    }

    public static void warning(Class source, String message, Throwable exception) {
        assert log != null : Logging.class;
        log.warning(source, message, exception);
    }

    public static void error(Class source, String message) {
        assert log != null : Logging.class;
        log.error(source, message);
    }

    public static void error(Class source, String message, Throwable exception) {
        assert log != null : Logging.class;
        log.error(source, message, exception);
    }

    public static void fatal(Class source, String message) {
        assert log != null : Logging.class;
        log.fatal(source, message);
    }

    public static void fatal(Class source, String message, Throwable exception) {
        assert log != null : Logging.class;
        log.fatal(source, message, exception);
    }

    public static void timer(Class source, String message, long beginStamp) {
        assert log != null : Logging.class;
        log.timer(source, message, beginStamp);
    }
}
