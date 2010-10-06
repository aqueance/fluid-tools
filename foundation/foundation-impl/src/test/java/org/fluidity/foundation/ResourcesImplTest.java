/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.foundation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ResourcesImplTest extends MockGroupAbstractTest {

    private final Resources delegate = addControl(Resources.class);

    private final Resources resources = new ResourcesImpl();

    @BeforeMethod
    public void setup() throws Exception {
        super.setup();
        Thread.currentThread().setContextClassLoader(new ClassLoader() {
            public Class loadClass(String name) {
                return delegate.loadClass(name);
            }

            public URL getResource(String name) {
                return delegate.locateResource(name);
            }

            protected Enumeration<URL> findResources(String name) {
                return Collections.enumeration(Arrays.asList(delegate.locateResources(name)));
            }

            public InputStream getResourceAsStream(String name) {
                return delegate.loadResource(name);
            }
        });
    }

    @AfterMethod
    public void cleanup() {
        Thread.currentThread().setContextClassLoader(null);
    }

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    @Test
    public void loadClass() throws Exception {
        final String className = getClass().getName();
        EasyMock.expect(delegate.loadClass(className)).andReturn(getClass());
        EasyMock.expect(delegate.loadClass(className)).andThrow(new RuntimeException());

        replay();
        assert getClass() == resources.loadClass(className);
        assert resources.loadClass(className) == null;
        verify();
    }

    @Test
    public void loadResource() throws Exception {
        final String resourceName = "resource";
        final InputStream stream = new ByteArrayInputStream(new byte[0]);

        EasyMock.expect(delegate.loadResource(resourceName)).andReturn(stream);
        EasyMock.expect(delegate.loadResource(resourceName)).andReturn(stream);

        replay();
        assert stream == resources.loadResource(resourceName);
        assert stream == resources.loadResource("/" + resourceName);
        verify();
    }

    @Test
    public void loadClassResource() throws Exception {
        final String className = getClass().getName();
        final String resourceName = className.replace('.', '/') + ".class";

        final InputStream stream = new ByteArrayInputStream(new byte[0]);

        EasyMock.expect(delegate.loadResource(resourceName)).andReturn(stream);

        replay();
        assert stream == resources.loadClassResource(className);
        verify();
    }

    @Test
    public void locateResource() throws Exception {
        final String resourceName = "whatever";
        final URL url = new URL("file:///");

        EasyMock.expect(delegate.locateResource(resourceName)).andReturn(url);

        replay();
        assert url.equals(resources.locateResource(resourceName));
        verify();
    }

    @Test
    public void locateResources() throws Exception {
        final String resourceName = "whatever";
        final URL url1 = new URL("file:///a");
        final URL url2 = new URL("file:///b");

        final URL[] urls = new URL[] { url1, url2 };
        EasyMock.expect(delegate.locateResources(resourceName)).andReturn(urls);

        replay();
        assert new ArrayList<URL>(Arrays.asList(urls))
                .equals(new ArrayList<URL>(Arrays.asList(resources.locateResources(resourceName))));
        verify();
    }
}