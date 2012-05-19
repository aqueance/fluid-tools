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

package org.fluidity.deployment.plugin.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.deployment.plugin.spi.ServerBootstrap;
import org.fluidity.foundation.jarjar.Handler;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Bootstraps a Jetty web container and deploys the WAR file that contains this class and then all other supplied WAR files.
 *
 * @author Tibor Varga
 */
public final class JettyBootstrap implements ServerBootstrap {

    public void bootstrap(final int httpPort, final File bootApp, final List<File> managedApps, final String args[]) throws IOException {
        final WebAppContext defaultContext = deployWar(bootApp, true);
        final List<WebAppContext> contextList = new ArrayList<WebAppContext>();

        for (final File app : managedApps) {
            contextList.add(deployWar(app, false));
        }

        JettyServer.start(httpPort, defaultContext, contextList);
    }

    private WebAppContext deployWar(final File warFile, final boolean root) throws IOException {
        final WebAppContext context = new WebAppContext();
        context.setExtractWAR(false);
        context.setCopyWebInf(false);
        context.setClassLoader(new InlineWebAppClassLoader(context));

        final String archiveName = warFile.getName();
        final String contextPath = "/" + (root ? "" : artifactId(archiveName));

        context.setContextPath(contextPath);
        context.setParentLoaderPriority(true);

        context.setWar(warFile.getPath());

        System.out.printf("Context %s: %s%n", context.getContextPath(), context.getWar());

        return context;
    }

    private final Pattern archiveNamePattern = Pattern.compile("(.+?)-\\d.*\\.war");

    private String artifactId(final String archiveName) {
        final Matcher matcher = archiveNamePattern.matcher(archiveName);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Could not parse archive name %s using pattern %s", archiveName, archiveNamePattern));
        }

        return matcher.group(1);
    }

    private static class InlineWebAppClassLoader extends WebAppClassLoader {

        public InlineWebAppClassLoader(final WebAppContext context) throws IOException {
            super(context);
        }

        @Override
        public void addClassPath(final String classPath) throws IOException {
            if (classPath != null) {
                for (final String path : classPath.split("[,;]")) {
                    final Resource resource = getContext().newResource(path.trim());

                    if (resource.toString().endsWith("/")) {
                        addURL(resource.getURL());
                    } else {
                        addURL(Handler.formatURL(resource.getURL()));
                    }
                }
            }
        }
    }
}
