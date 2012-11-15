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

import java.net.URL;
import java.security.Permission;

import org.fluidity.foundation.Command;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Strings;

/**
 * Represents a permission to access {@linkplain org.fluidity.foundation.Archives.Nested nested} archives. The {@link #getName()} of this permission is an URL
 * to the archive to access nested archives in. This permission then delegates to the appropriate permissions for that URL, like {@link java.io.FilePermission}
 * or {@link java.net.SocketPermission}. Wildcards supported by these are supported by adding the necessary protocol to the name of this permission, like
 * <code>file:/path/*</code>, <code>file:/path/-</code>, <code>http://*.host.com</code>, etc.
 *
 * @author Tibor Varga
 */
public final class AccessPermission extends Permission {

    private final Deferred.Reference<Permission> delegate = Deferred.reference(new Deferred.Factory<Permission>() {
        public Permission create() {
            final String spec = getName();

            return Exceptions.wrap(new Command.Process<Permission, Exception>() {
                public Permission run() throws Exception {
                    return new URL(spec).openConnection().getPermission();
                }
            });
        }
    });

    public AccessPermission(final String name) {
        super(name);
    }

    private Permission delegate() {
        return delegate.get();
    }

    @Override
    public boolean implies(final Permission permission) {
        return permission instanceof AccessPermission && delegate().implies(((AccessPermission) permission).delegate());
    }

    @Override
    public String getActions() {
        return "";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AccessPermission that = (AccessPermission) o;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s %s", Strings.formatClass(false, true, getClass()), getName());
    }
}
