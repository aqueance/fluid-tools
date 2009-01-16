package org.fluidity.deployment.cli;

import org.fluidity.deployment.RuntimeControl;

/**
 * The main class of the comman line application that parses parameters and keeps the application's main thread running until stopped. The implementation can
 * access the command line parameters by having a constructor with, among other dependencies, a <code>final String[] args</code> parameter.
 *
 * <p/>
 *
 * The application exits when the call to the {@link Runnable#run()} method returns, unless the developer has started but failed to stop non-deamon threads.
 */
public interface MainLoop extends Runnable, RuntimeControl {

}
