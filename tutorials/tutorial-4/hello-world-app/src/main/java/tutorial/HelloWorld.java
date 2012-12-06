package tutorial;

import org.fluidity.composition.Component;
import org.fluidity.deployment.cli.Application;

@Component
final class HelloWorld implements Application {

    private final SimpleOutput output;

    HelloWorld(final SimpleOutput output) {
        this.output = output;
    }

    public void run(final String[] arguments) {
        output.println("Hello World!");
    }
}
