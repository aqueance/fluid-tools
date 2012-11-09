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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;

import org.fluidity.foundation.ClassLoaders;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class URLClassLoaderTest {

    private final String container = "classpath/samples.jar";
    private final URL root = getClass().getClassLoader().getResource(container);

    @Test
    public void testEmbeddedClassPath() throws Exception {
        assert new java.net.URLClassLoader(new URL[] { root }).loadClass("org.fluidity.samples.Root").newInstance() != null;
        assert ClassLoaders.create(Collections.singleton(root), null).loadClass("org.fluidity.samples.Root").newInstance() != null;
    }

    @Test
    public void testFileNames() throws Exception {
        final URL url = ClassLoaders.findClassResource(getClass());
        final File target = new File(URLDecoder.decode(url.getPath(), "UTF-8")).getParentFile();

        File[] files = null;

        try {
            files = temporary(target, "check space", "check%20url+encoding", "check:colon", "check!bang");

            create(files);
            verify(files);
        } finally {
            delete(files);
        }
    }

    private File[] temporary(final File target, final String... names) {
        final File tmp = new File(System.getProperty("java.io.tmpdir"));

        final File[] files = new File[names.length << 1];

        for (int i = 0, limit = names.length; i < limit; i++) {
            final String name = names[i];
            try {
                files[i] = File.createTempFile(name, ".txt", tmp);
                files[i + names.length] = File.createTempFile(name, ".txt", target);
            } catch (final IOException e) {
                assert false : String.format("%s: %s", name, e.getMessage());
            }
        }

        return files;
    }

    private void create(final File... files) throws IOException {
        for (final File file : files) {
            assert file.exists() || file.createNewFile() : file;
        }
    }

    private void verify(final File... files) throws IOException {
        for (final File file : files) {
            assert file.exists() : file;
            assert URLClassLoader.file(file.getAbsolutePath()).exists() : file;

            final String path = file.getParent();
            if (URLDecoder.decode(path, "UTF-8").equals(path)) {
                assert URLClassLoader.file(String.format("%s%s%s", path, File.separator, encode(file.getName()))).exists() : file;
                assert URLClassLoader.file(encode(file.getPath())).exists() : file;
            }
        }
    }

    private void delete(final File... files) {
        if (files != null) {
            for (final File file : files) {
                if (file.exists() && !file.delete()) {
                    file.deleteOnExit();
                }
            }
        }
    }

    private String encode(final String text) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, "UTF-8");
    }
}
