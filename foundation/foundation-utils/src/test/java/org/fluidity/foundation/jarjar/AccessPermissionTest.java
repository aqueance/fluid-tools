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

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class AccessPermissionTest {

    @Test
    public void testExplicitURL() throws Exception {
        final String url = "file:/some/path/file.txt";
        final Permission permission = new AccessPermission(url);

        check(true, permission, permission);
        check(false, permission, new URL(url).openConnection().getPermission());
    }

    @Test
    public void testWrongURL() throws Exception {
        check(false, new AccessPermission("file:/some/path/file.txt"), new AccessPermission("http://host.com/path/file.txt"));
        check(false, new AccessPermission("file:/some/path/file1.txt"), new AccessPermission("file:/some/path/file2.txt"));
    }

    @Test
    public void testWildcards() throws Exception {
        check(true, new AccessPermission("file:/some/path/*"), new AccessPermission("file:/some/path/file.txt"));
        check(false, new AccessPermission("file:/some/path1/*"), new AccessPermission("file:/some/path2/file.txt"));
        check(true, new AccessPermission("file:/-"), new AccessPermission("file:/some/path/file.txt"));
        check(true, new AccessPermission("http://*"), new AccessPermission("http://some.host/path/file.txt"));
        check(false, new AccessPermission("file:/-"), new AccessPermission("http://some.host/path/file.txt"));
        check(false, new AccessPermission("http://*"), new AccessPermission("file:/some/path/file.txt"));
    }

    private void check(final boolean match, final Permission permission, final Permission check) {
        assert permission.implies(check) == match : String.format("%s -> %s", permission, check);
    }
}
