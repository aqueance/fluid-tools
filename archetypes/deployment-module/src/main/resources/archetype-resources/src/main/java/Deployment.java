#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.fluidity.composition.Component;
import org.fluidity.composition.DeployedComponent;

@Component
final class Deployment implements DeployedComponent {

    public String key() {
        return null;
    }

    public String name() {
        return null;
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }
}
