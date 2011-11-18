/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.deployment.plugin.spi.ServerBootstrap;

import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Bootstraps a Jetty web container and deploys the WAR file that contains this class and then all other supplied WAR files.
 *
 * @author Tibor Varga
 */
public final class JettyBootstrap implements ServerBootstrap {

    public void bootstrap(final int httpPort, final File bootApp, final List<File> managedApps, final File workDirectory, final String args[]) {
        final WebAppContext defaultContext = deployWar(bootApp, workDirectory, true);
        final List<WebAppContext> contextList = new ArrayList<WebAppContext>();

        for (final File app : managedApps) {
            contextList.add(deployWar(app, workDirectory, false));
        }

        JettyServer.start(httpPort, defaultContext, contextList);
    }

    private WebAppContext deployWar(final File warFile, final File workDirectory, final boolean root) {
        final WebAppContext context = new WebAppContext();
        final String archiveName = warFile.getName();
        final String contextPath = "/" + (root ? "" : artifactId(archiveName));

        context.setTempDirectory(new File(workDirectory, archiveName));
        context.setContextPath(contextPath);
        context.setParentLoaderPriority(true);

        context.setWar(warFile.getPath());

        System.out.printf("Context %s: %s%n", context.getContextPath(), context.getWar());

        return context;
    }

    private final Pattern archiveNamePattern = Pattern.compile("(.+?)(-\\d.*)?\\.war");

    private String artifactId(final String archiveName) {
        final Matcher matcher = archiveNamePattern.matcher(archiveName);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Could not parse archive name %s using pattern %s", archiveName, archiveNamePattern));
        }

        return matcher.group(1);
    }
}
