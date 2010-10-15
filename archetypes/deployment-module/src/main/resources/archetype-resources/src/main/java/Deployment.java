#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.fluidity.deployment.DeployedComponent;

final class Deployment implements DeployedComponent {

    public String key() {
        return "sample-deployment";
    }

    public String name() {
        return "Sample Deployment";
    }

    public void start() throws Exception {
        // get something started and return
    }

    public void stop() throws Exception {
        // get that something stopped and return
    }
}
