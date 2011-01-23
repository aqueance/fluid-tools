/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Bootstraps a Jetty web container and deploys the .war file that contains this class and then all other .war files supplied.
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

        JettyServer.start(httpPort, args, defaultContext, contextList);
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

    private final Pattern archiveNamePattern = Pattern.compile("(.+?)-\\d.*\\.war");

    private String artifactId(final String archiveName) {
        final Matcher matcher = archiveNamePattern.matcher(archiveName);

        if (!matcher.matches()) {
            throw new RuntimeException(String.format("Could not parse archive name %s using pattern %s", archiveName, archiveNamePattern));
        }

        return matcher.group(1);
    }
}
