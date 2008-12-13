#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.fluidity.composition.Component;

@Component
final class ComponentImpl implements ComponentApi {

    private final Dependency dependency;

    public ComponentImpl(final Dependency dependency) {
        this.dependency = dependency;
    }

    public boolean sendText(final String text) {
        return dependency.receiveText(text);
    }
}
