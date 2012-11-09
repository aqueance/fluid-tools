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

package org.fluidity.foundation;

import java.net.URL;
import java.util.Arrays;

import org.fluidity.foundation.jarjar.Handler;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ArchivesTest {

    private final URL container = getClass().getClassLoader().getResource("level0.jar");

    @Test
    public void testNestedURLComponents() throws Exception {
        final URL root = Archives.containing(Object.class);
        assert root != null;
        assert Archives.containing(root) == null : Archives.containing(root);

        final URL level1 = Handler.formatURL(container, "level1-2.jar");
        final URL level2 = Handler.formatURL(container, "level1-2.jar", "level2.jar");
        final URL level3 = Handler.formatURL(container, "level1-2.jar", "level2.jar", "level3.jar");

        assert level2.equals(Archives.containing(level3)) : Archives.containing(level3);
        assert level1.equals(Archives.containing(level2)) : Archives.containing(level2);
        assert container.equals(Archives.containing(level1)) : Archives.containing(level1);

        verify(Archives.resourcePath(level2, level1), "level2.jar");
        verify(Archives.resourcePath(level3, level1), "level2.jar", "level3.jar");
        verify(Archives.resourcePath(level3, level2), "level3.jar");
        verify(Archives.resourcePath(level3, container), "level1-2.jar", "level2.jar", "level3.jar");
    }

    private void verify(final String[] path, final String... resources) throws Exception {
        assert path != null;
        assert path.length == resources.length : Arrays.toString(path);

        for (int i = 0, length = resources.length; i < length; i++) {
            final String expected = resources[i];
            final String actual = path[i];

            assert expected.equals(actual) : String.format("Expected '%s', got '%s'", expected, actual);
        }
    }

    @Test
    public void testJarURLComponents() throws Exception {
        final String file = Archives.FILE.concat(":/tmp/archive.jar");
        final String resource = "resource.txt";
        final URL url = new URL(String.format("%s:%s%s%s", Archives.PROTOCOL, file, Archives.DELIMITER, resource));

        final URL base = Archives.containing(url);
        assert file.equals(base.toExternalForm()) : base;

        final String name = Archives.resourcePath(url, base)[0];
        assert resource.equals(name) : name;
    }
}
