package tutorial;

import org.fluidity.composition.Component;
import org.fluidity.foundation.Log;

@Component
final class SimpleLoggingImpl implements SimpleOutput {

    private final Log<?> log;

    SimpleLoggingImpl(final Log<SimpleLoggingImpl> log) {
        this.log = log;
    }

    public void println(final String message) {
        log.info(message);
    }
}
