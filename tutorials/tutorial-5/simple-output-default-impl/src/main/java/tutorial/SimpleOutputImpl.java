package tutorial;

import org.fluidity.composition.Component;

@Component
final class SimpleOutputImpl implements SimpleOutput {

    public void println(final String message) {
        System.out.println(message);
    }
}
