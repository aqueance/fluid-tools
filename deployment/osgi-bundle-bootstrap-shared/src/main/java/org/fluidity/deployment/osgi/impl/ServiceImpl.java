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

package org.fluidity.deployment.osgi.impl;

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.fluidity.deployment.osgi.Service;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public final class ServiceImpl implements Service {

    private Class<?> type;
    private final String filter;

    ServiceImpl(final Class<?> type) {
        this(type, "");
    }

    ServiceImpl(final Class<?> type, final String filter) {
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
        return Objects.hash(type, filter);
    }

    @Override
    public String toString() {
        return Strings.describeAnnotation(false, this);
    }
}
