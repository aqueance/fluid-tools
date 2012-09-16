/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.deployment.osgi;

/**
 * @author Tibor Varga
 */
final class ServiceDescriptor extends Descriptor {

    public final String filter;
    public final Service annotation;

    ServiceDescriptor(final Class<?> reference, final Service service) {
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
