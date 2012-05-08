package org.fluidity.deployment.osgi;

/**
 * @author Tibor Varga
 */
final class ServiceDescriptor extends Descriptor {

    public final String filter;
    public final Service annotation;

    public ServiceDescriptor(final Class<?> reference, final Service service) {
        super(service.api() == Object.class ? reference : service.api());

        final String filter = service.filter();
        this.filter = filter.isEmpty() ? null : filter;
        this.annotation = new ServiceImpl(type, filter);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(final Object o) {
        if (!super.equals(o)) {
            return false;
        }

        final ServiceDescriptor that = (ServiceDescriptor) o;
        return !(filter != null ? !filter.equals(that.filter) : that.filter != null);
    }

    @Override
    public String toString() {
        return filter == null || filter.isEmpty() ? super.toString() : String.format("%s '%s'", super.toString(), filter);
    }
}
