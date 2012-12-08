package org.fluidity.deployment.maven;

import org.apache.maven.plugin.logging.Log;

/**
 * Encapsulates logging from Maven plugins.
 *
 * @author Tibor Varga
 */
public class Logger {

    /**
     * Creates a new logger that wraps the given Maven <code>log</code> with the given verbosity setting.
     *
     * @param log     the Maven log to wrap.
     * @param verbose tells if {@link #detail(String, Object...)} logs at <code>INFO</code> level (<code>true</code>) or <code>DEBUG</code> level
     *                (<code>false</code>).
     *
     * @return a new {@link Logger} instance; never <code>null</code>.
     */
    public static Logger initialize(final Log log, final boolean verbose) {
        return new Logger(log, verbose);
    }

    private final Log log;
    private final boolean debug;
    private final boolean verbose;

    private Logger(final Log log, final boolean verbose) {
        this.log = log;
        this.debug = log.isDebugEnabled();
        this.verbose = verbose;
    }

    /**
     * Tells if {@link #detail(String, Object...)} would output anything.
     *
     * @return <code>true</code> if {@link #detail(String, Object...)} would output anything; <code>false</code> otherwise.
     */
    public boolean active() {
        return debug || verbose;
    }

    /**
     * Outputs some detail either at <code>INFO</code> level, <code>DEBUG</code> level, or at no level at all, depending on whether INFO logging is enabled by
     * the <code>verbose</code> parameter of {@link #initialize(Log, boolean)} else on whether debug logging is {@linkplain Log#isDebugEnabled() enabled}.
     *
     * @param format    the format string of the message to feed to {@link String#format(String, Object...)}.
     * @param arguments the arguments to the <code>format</code>.
     */
    public void detail(final String format, final Object... arguments) {
        if (verbose) {
            log.info(String.format(format, arguments));
        } else if (debug) {
            log.debug(String.format(format, arguments));
        }
    }

    /**
     * Emits a message at <code>INFO</code> level.
     *
     * @param format    the format string of the message to feed to {@link String#format(String, Object...)}.
     * @param arguments the arguments to the <code>format</code>.
     */
    public void info(final String format, final Object... arguments) {
        log.info(String.format(format, arguments));
    }

    /**
     * Emits a message at <code>WARN</code> level.
     *
     * @param format    the format string of the message to feed to {@link String#format(String, Object...)}.
     * @param arguments the arguments to the <code>format</code>.
     */
    public void warn(final String format, final Object... arguments) {
        log.warn(String.format(format, arguments));
    }
}
