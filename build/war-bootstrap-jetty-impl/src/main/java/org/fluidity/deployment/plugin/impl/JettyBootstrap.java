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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.deployment.plugin.spi.ServerBootstrap;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Exceptions;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Descriptor;
import org.eclipse.jetty.webapp.DescriptorProcessor;
import org.eclipse.jetty.webapp.DiscoveredAnnotation;
import org.eclipse.jetty.webapp.FragmentDescriptor;
import org.eclipse.jetty.webapp.MetaData;
import org.eclipse.jetty.webapp.Ordering;
import org.eclipse.jetty.webapp.Origin;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebDescriptor;

/**
 * Bootstraps a Jetty web container and deploys the WAR file that contains this class and then all other supplied WAR files.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public final class JettyBootstrap implements ServerBootstrap {

    public void bootstrap(final int httpPort, final boolean extract, final File bootApp, final List<File> managedApps, final String args[]) throws IOException {
        final WebAppContext defaultContext = deployWar(bootApp, true, extract);
        final List<WebAppContext> contextList = new ArrayList<WebAppContext>();

        for (final File app : managedApps) {
            contextList.add(deployWar(app, false, extract));
        }

        JettyServer.start(httpPort, defaultContext, contextList);
    }

    private WebAppContext deployWar(final File warFile, final boolean root, final boolean extract) throws IOException {
        final WebAppContext context = new NestedWebAppContext();

        context.setThrowUnavailableOnStartupException(true);    // stops Jetty from swallowing exceptions on deployment
        context.setExtractWAR(extract);
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
            throw new IllegalArgumentException(String.format("Could not match archive name '%s' with pattern '%s'", archiveName, archiveNamePattern));
        }

        return matcher.group(1);
    }

    /**
     * Converts resource URLs to nested URLs.
     *
     * @author Tibor Varga
     */
    private static class InlineWebAppClassLoader extends WebAppClassLoader {

        InlineWebAppClassLoader(final WebAppContext context) throws IOException {
            super(context);
        }

        @Override
        public void addClassPath(final String classPath) throws IOException {
            if (classPath != null) {
                for (final String path : classPath.split("[,;]")) {
                    final Resource resource = getContext().newResource(path.trim());
                    addURL(resource.toString().endsWith("/") ? resource.getURL() : Archives.Nested.nestedURL(resource.getURL()));
                }
            }
        }
    }

    /**
     * Replaces resource URLs with nested URLs.
     *
     * @author Tibor Vara
     */
    private static class NestedMetaData extends MetaData {

        private final MetaData delegate;

        public NestedMetaData(final MetaData delegate) {
            assert delegate != null;
            this.delegate = delegate;
        }

        private Resource replace(final Resource resource) throws IOException {
            return Resource.newResource(Archives.Nested.nestedURL(resource.getURL()));
        }

        private Resource nested(final Resource resource) {
            return Exceptions.wrap(new Command.Process<Resource, IOException>() {
                public Resource run() throws IOException {
                    return replace(resource);
                }
            });
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public void setDefaults(final Resource resource) throws Exception {
            delegate.setDefaults(replace(resource));
        }

        @Override
        public void setWebXml(final Resource resource) throws Exception {
            delegate.setWebXml(replace(resource));
        }

        @Override
        public void addOverride(final Resource resource) throws Exception {
            delegate.addOverride(replace(resource));
        }

        @Override
        public void addFragment(final Resource jarResource, final Resource xmlResource) throws Exception {
            delegate.addFragment(replace(jarResource), replace(xmlResource));
        }

        @Override
        public void addDiscoveredAnnotations(final List<DiscoveredAnnotation> annotations) {
            delegate.addDiscoveredAnnotations(annotations);
        }

        @Override
        public void addDiscoveredAnnotations(final Resource resource, final List<DiscoveredAnnotation> annotations) {
            delegate.addDiscoveredAnnotations(nested(resource), annotations);
        }

        @Override
        public void addDescriptorProcessor(final DescriptorProcessor p) {
            delegate.addDescriptorProcessor(p);
        }

        @Override
        public void orderFragments() {
            delegate.orderFragments();
        }

        @Override
        public void resolve(final WebAppContext context) throws Exception {
            delegate.resolve(context);
        }

        @Override
        public boolean isDistributable() {
            return delegate.isDistributable();
        }

        @Override
        public WebDescriptor getWebXml() {
            return delegate.getWebXml();
        }

        @Override
        public List<WebDescriptor> getOverrideWebs() {
            return delegate.getOverrideWebs();
        }

        @Override
        public WebDescriptor getWebDefault() {
            return delegate.getWebDefault();
        }

        @Override
        public List<FragmentDescriptor> getFragments() {
            return delegate.getFragments();
        }

        @Override
        public List<Resource> getOrderedWebInfJars() {
            return delegate.getOrderedWebInfJars();
        }

        @Override
        public List<FragmentDescriptor> getOrderedFragments() {
            return delegate.getOrderedFragments();
        }

        @Override
        public Ordering getOrdering() {
            return delegate.getOrdering();
        }

        @Override
        public void setOrdering(final Ordering o) {
            delegate.setOrdering(o);
        }

        @Override
        public FragmentDescriptor getFragment(final Resource jar) {
            return delegate.getFragment(nested(jar));
        }

        @Override
        public FragmentDescriptor getFragment(final String name) {
            return delegate.getFragment(name);
        }

        @Override
        public Resource getJarForFragment(final String name) {
            return delegate.getJarForFragment(name);
        }

        @Override
        public Map<String, FragmentDescriptor> getNamedFragments() {
            return delegate.getNamedFragments();
        }

        @Override
        public Origin getOrigin(final String name) {
            return delegate.getOrigin(name);
        }

        @Override
        public Descriptor getOriginDescriptor(final String name) {
            return delegate.getOriginDescriptor(name);
        }

        @Override
        public void setOrigin(final String name, final Descriptor descriptor) {
            delegate.setOrigin(name, descriptor);
        }

        @Override
        public void setOrigin(final String name) {
            delegate.setOrigin(name);
        }

        @Override
        public boolean isMetaDataComplete() {
            return delegate.isMetaDataComplete();
        }

        @Override
        public void addWebInfJar(final Resource resource) {
            delegate.addWebInfJar(nested(resource));
        }

        @Override
        public List<Resource> getWebInfJars() {
            return delegate.getWebInfJars();
        }

        @Override
        public List<Resource> getOrderedContainerJars() {
            return delegate.getOrderedContainerJars();
        }

        @Override
        public void addContainerJar(final Resource jar) {
            delegate.addContainerJar(nested(jar));
        }

        @Override
        public boolean isAllowDuplicateFragmentNames() {
            return delegate.isAllowDuplicateFragmentNames();
        }

        @Override
        public void setAllowDuplicateFragmentNames(final boolean flag) {
            delegate.setAllowDuplicateFragmentNames(flag);
        }
    }

    /**
     * Replaces the metadata to enable replacement of resource URLs with nested archive URLs.
     *
     * @author Tibor Varga
     */
    private static class NestedWebAppContext extends WebAppContext {

        private Deferred.Reference<NestedMetaData> metaData = Deferred.reference(new Deferred.Factory<NestedMetaData>() {
            public NestedMetaData create() {
                return new NestedMetaData(NestedWebAppContext.super.getMetaData());
            }
        });

        @Override
        public MetaData getMetaData() {
            return metaData.get();
        }

        @Override
        protected void doStop() throws Exception {
            super.doStop();
            metaData.invalidate();
        }
    }
}
