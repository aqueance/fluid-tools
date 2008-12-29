package org.fluidity.composition.cli;

/**
 * The main class of the comman line application that parses parameters and keeps the application's main thread running until stopped. The implementation can
 * access the command line parameters by having a constructor with, among other dependencies, a <code>final String[] args</code> parameter.
 *
 * <p/>
 *
 * The application exits when the call to the {@link Runnable#run()} method returns, unless the developer has started but failed to stop non-deamon threads.
 *
 * TODO: extend RuntimeControl
 */
public interface MainLoop extends Runnable {

    /**
     * Stops the main thread, intended to stop the application itself.
     */
    void stop();
}
