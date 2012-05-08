package org.fluidity.deployment.osgi;

import java.lang.annotation.Annotation;

import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
final class ServiceImpl implements Service {

    private Class<?> type;
    private final String filter;

    public ServiceImpl(final Class<?> type) {
        this(type, "");
    }

    public ServiceImpl(final Class<?> type, final String filter) {
        this.type = type;
        this.filter = filter;
    }

    public Class<? extends Annotation> annotationType() {
        return Service.class;
    }

    public Class<?> api() {
        return type;
    }

    public String filter() {
        return filter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || !(o instanceof Service)) {
            return false;
        }

        final Service that = (Service) o;
        return !(filter() != null ? !filter().equals(that.filter()) : that.filter() != null) && api().equals(that.api());

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();

        if (filter != null) {
            result = 31 * result + filter.hashCode();
        }

        return result;
    }

    @Override
    public String toString() {
        return Strings.printAnnotation(this);
    }
}
