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

package org.fluidity.foundation.jarjar;

import java.security.BasicPermission;
import java.security.Permission;

import org.fluidity.foundation.Strings;

/**
 * Wraps another permission so that the access control implementation honors the full nested URL in security policy files. The {@link URLClassLoader} replaces
 * instances with their delegates because these permission, probably due to being defined by application code, remain unresolved and thus cannot be granted to
 * a code base in security policy files.
 *
 * @author Tibor Varga
 */
final class AccessPermission extends BasicPermission {

    private final Permission delegate;

    AccessPermission(final Permission delegate) {
        super(delegate.getName(), delegate.getActions());
        this.delegate = delegate;
    }

    /**
     * Returns the wrapped permission.
     *
     * @return the wrapped permission.
     */
    Permission delegate() {
        return delegate;
    }

    @Override
    public boolean implies(final Permission permission) {
        return permission.getClass() == getClass() && delegate.implies(((AccessPermission) permission).delegate);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", Strings.formatClass(false, true, getClass()), delegate.toString());
    }
}
