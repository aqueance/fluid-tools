/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Security;

/**
 * Launches a main class from a JAR file using a class loader that can load classes from JAR files nested inside the main JAR. Nested JAR files must be located
 * in the path denoted by the manifest attribute name returned by {@link org.fluidity.foundation.Archives.Nested#attribute(String)
 * Archives.Nested.attribute(null)}. The main class to be loaded is defined by the manifest attribute named in {@link #START_CLASS}. The
 * <code>Main-Class</code> manifest attribute has to point to this class, obviously.
 * <p>
 * Without arguments, this launcher will try to load the {@link #START_CLASS} from the archive the launcher itself was loaded from. This can be
 * overridden with the {@link #URL_PARAM} parameter, which specifies the URL to load as the application.
 * <p>
 * The above manifest attributes are set by the appropriate {@link org.fluidity.deployment.plugin.spi.JarManifest} processor when used by the
 * <code>org.fluidity.maven:standalone-jar-maven-plugin</code> Maven plugin.
 *
 * @author Tibor Varga
 */
@SuppressWarnings({ "JavadocReference", "WeakerAccess" })
public final class Launcher {

    /**
     * The JAR manifest attribute that specifies the application's main class, one with a <code>public static void main(final String[] arguments) throws
     * Exception</code> method.
     */
    public static final String START_CLASS = "Start-Class";

    private static final String MAIN_CLASS = Attributes.Name.MAIN_CLASS.toString();

    /**
     * The optional command line parameter prefix to trigger loading and launching an application from a given URL. The syntax is:
     * <pre>
     * $ java -jar some-launcher.jar -url:http://host.com:port/path/application.jar
     * </pre>
     */
    public static final String URL_PARAM = "-url:";

    private Launcher() { }

    /**
     * The command line entry point.
     *
     * @param arguments the command line arguments.
     *
     * @throws Exception when anything goes wrong.
     */
    public static void main(final String[] arguments) throws Exception {
        try {
            Exceptions.wrap(() -> {
                if (Security.CONTROLLED) {
                    AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                        start(arguments);
                        return null;
                    });
                } else {
                    start(arguments);
                }

                return null;
            });
        } catch (final Exceptions.Wrapper wrapper) {
            throw wrapper.rethrow(Exception.class);
        }
    }

    private static void start(final String[] args) throws Exception {
        Archives.Cache.access(() -> {
            final Class<?> me = Launcher.class;
            final URL root = Archives.containing(me);

            final URL url;
            final String[] arguments;

            if (Archives.attributes(true, root, START_CLASS)[0] == null && args.length > 0 && args[0].startsWith(URL_PARAM)) {
                final String parameter = args[0].substring(URL_PARAM.length());

                try {
                    url = new URL(parameter);
                } catch (final MalformedURLException e) {
                    throw new IllegalArgumentException(String.format("Invalid URL: '%s'", parameter), e);
                }

                arguments = new String[args.length - 1];
                System.arraycopy(args, 1, arguments, 0, arguments.length);
            } else {
                url = root;
                arguments = args;
            }

            final String[] attributes = Archives.attributes(true, url, MAIN_CLASS, START_CLASS);
            final String main = attributes[attributes[1] == null ? 0 : 1];

            if (main == null || main.equals(me.getName())) {
                throw new IllegalStateException(String.format("%s main class defined in the %s manifest (attribute %s)",
                                                              main == null ? "No" : "Wrong",
                                                              url,
                                                              main == null ? MAIN_CLASS : START_CLASS));
            } else {
                final List<URL> urls = new ArrayList<>();

                urls.add(url);
                urls.addAll(Archives.Nested.dependencies(true, url, null));

                final ClassLoader parent = ClassLoaders.findClassLoader(me, true);
                final ClassLoader loader = ClassLoaders.create(urls, parent, null);

                ClassLoaders.context(loader, _loader -> _loader.loadClass(main).getMethod("main", String[].class).invoke(null, (Object) arguments));
            }

            return null;
        });
    }
}
