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

package org.fluidity.features.remote;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.jar.Attributes;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Exceptions;

import static org.fluidity.foundation.Command.Job;

/**
 * Loads from an URL and launches a command line Java application. The URL can be any one supported by the Java runtime.
 * <p/>
 * Setting up proxies: <a
 * href="http://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html">http://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html</a>
 *
 * @author Tibor Varga
 */
public final class RemoteLauncher {

    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            throw usage("No arguments specified");
        } else {
            final URL url = new URL(args[0]);

            Archives.Nested.access(new Job<Exception>() {
                public void run() throws Exception {
                    final String[] arguments = new String[args.length - 1];
                    System.arraycopy(args, 1, arguments, 0, arguments.length);

                    final String main = Exceptions.wrap(String.format("%s is not an archive", url), Problem.class, new Command.Process<String, Throwable>() {
                        public String run() throws Throwable {
                            return Archives.attributes(true, url, Attributes.Name.MAIN_CLASS)[0];
                        }
                    });

                    if (main == null) {
                        throw usage("No main class specified in %s", url);
                    }

                    final ClassLoader loader = ClassLoaders.create(Collections.singleton(url), null, null);
                    final Method start = loader.loadClass(main).getMethod("main", String[].class);
                    start.invoke(null, (Object) arguments);
                }
            });
        }
    }

    private static class Problem extends RuntimeException {
        public Problem(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    private static RuntimeException usage(final String format, final Object... arguments) throws IOException {
        if (format != null) {
            System.out.printf(format, arguments);
            System.out.println();
            System.out.println();
        }

        System.out.printf("Usage: java -jar %s <URL> [arguments]%n", new File(Archives.root().getPath()).getName());
        System.exit(-1);

        return null;
    }
}
