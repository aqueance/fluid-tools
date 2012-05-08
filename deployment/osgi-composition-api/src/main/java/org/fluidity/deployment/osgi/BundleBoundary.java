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

package org.fluidity.deployment.osgi;

/**
 * Enables controlled cross-bundle class loading in an OSGi container. More specifically, this component allows:
 * <ul>
 * <li>importing a remote component into the calling bundle, allowing <em>it</em> to find classes in this bundle: useful when receiving a reference to an OSGi
 * service that you want to be able to load classes from this bundle;</li>
 * <li>exporting a local component for access by another component, allowing the local component to load classes from the bundle of the other: useful when
 * sending to another bundle an OSGi service component that you want to be able to load classes from that other bundle.</li>
 * </ul>
 * <h3>Usage</h3>
 * Fluid Tool uses this component internally to inject an OSGi service as a dependency of a {@linkplain BundleComponentContainer.Managed managed component}.
 * Manual use of this component would only be necessary if you don't use such managed components.
 * <pre>
 * final <span class="hl1">BundleBoundary</span> customs = ...;
 * final {@linkplain org.osgi.framework.BundleContext} context = ...;
 * final {@linkplain org.osgi.framework.ServiceReference} reference = context.getServiceReferences(<span class="hl2">RemoteComponent</span>.class.getName());
 * final <span class="hl2">RemoteComponent</span> component = (<span class="hl2">RemoteComponent</span>) customs.<span class="hl1">import</span>(<span class="hl2">RemoteComponent</span>.class, context.getService(reference));
 *
 * </pre>
 *
 * @author Tibor Varga
 */
public interface BundleBoundary {

    /**
     * Wraps the given object from another bundle to allow it to find classes in this bundle.
     * <p/>
     * The given object must implement the given interface and the given interface must be represented at run time by the same class in both bundles; i.e., it
     * must be exported by one bundle and imported by the others.
     *
     * @param type   the Java interface of the remote object.
     * @param remote the object from another bundle.
     *
     * @return the wrapped service.
     */
    <T> T imported(Class<T> type, T remote);

    /**
     * Wraps the given local object to allow it to load classes from the bundle of the provided remote object. If the remote object is not known, the receiver
     * of our local object must use the {@link #imported(Class, Object)} method instead.
     * <p/>
     * The given object must implement the given interface and the given interface must be represented at run time by the same class in both bundles; i.e., it
     * must be exported by one bundle and imported by the others.
     *
     * @param type   the Java interface of the local object.
     * @param remote the remote object, the bundle of which the local object must be able to find classes in.
     * @param local  the local object to be invoked by the client or any other object from its bundle.
     *
     * @return the wrapped local object.
     */
    <T> T exported(Class<T> type, Object remote, T local);
}
