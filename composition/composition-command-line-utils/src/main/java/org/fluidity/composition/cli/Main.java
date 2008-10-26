package org.fluidity.composition.cli;

/**
 * The main class of the comman line application that parses parameters and keeps the application's main thread running.
 */
public interface Main extends Runnable {

    /**
     * This method is invoked with the command line parameters just before the {@link org.fluidity.composition.DeploymentBootstrap} component loads all {@link
     * org.fluidity.composition.DeployedComponent} and {@link org.fluidity.composition.DeploymentObserver} objects.
     *
     * @param args the command line arguments.
     */
    void initialize(final String[] args);

    /**
     * Invoked after the {@link org.fluidity.composition.DeploymentBootstrap} component has been invoked. The application exits when this call returns.
     */
    void run();
}
