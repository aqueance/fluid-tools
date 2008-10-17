/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package org.fluidity.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;

import org.fluidity.composition.Component;

/**
 * Uses the class loader hierarchy to load resources. 
 *
 * @author Tibor Varga
 * @version $Revision$
 */
@Component
final class ResourcesImpl implements Resources {

    public String resourceName(String name) {
        return ClassLoaderUtils.absoluteResourceName(name);
    }

    public URL locateResource(String name) {
        return classLoader().getResource(absoluteName(name));
    }

    public URL[] locateResources(String name) {
        Collection<URL> answer = new LinkedHashSet<URL>();

        try {
            for (Enumeration resources = classLoader().getResources(absoluteName(name)); resources.hasMoreElements();) {
                answer.add((URL) resources.nextElement());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return answer.toArray(new URL[answer.size()]);
    }

    public InputStream loadResource(String name) {
        return classLoader().getResourceAsStream(absoluteName(name));
    }

    public InputStream loadClassResource(String className) {
        return classLoader().getResourceAsStream(className.replace('.', '/') + ".class");
    }

    public Class loadClass(String className) {
        try {
            return classLoader().loadClass(className);
        } catch (Exception e) {
            return null;
        }
    }

    private ClassLoader classLoader() {
        return ClassLoaderUtils.findClassLoader(getClass());
    }

    private String absoluteName(String name) {
        return ClassLoaderUtils.absoluteResourceName(name);
    }
}
