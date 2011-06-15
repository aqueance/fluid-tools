/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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
class ServiceSpecification {
    public final Class<?> reference;
    public final Class<?> api;
    public final String filter;

    private final int hashCode;

    public ServiceSpecification(final Class<?> reference, final Service service) {
        this(reference, service.api() == Object.class ? reference : service.api(), service.filter());
    }

    ServiceSpecification(final Class<?> api, final String filter) {
        this(api, api, filter);
    }

    ServiceSpecification(final Class<?> api) {
        this(api, api, null);
    }

    ServiceSpecification(final Class<?> reference, final Class<?> api, final String filter) {
        this.reference = reference;
        this.api = api;
        this.filter = filter;
        this.hashCode = calculateHash();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ServiceSpecification that = (ServiceSpecification) o;
        return api.equals(that.api) && reference.equals(that.reference) && !(filter != null ? !filter.equals(that.filter) : that.filter != null);

    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int calculateHash() {
        int result = reference.hashCode();
        result = 31 * result + api.hashCode();
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return filter == null || filter.isEmpty() ? api.toString() : String.format("%s '%s'", api, filter);
    }
}
