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

package org.fluidity.deployment.osgi;

/**
 * Enables controlled cross-bundle class loading in an OSGi container. More specifically, this component allows:
 * <ul>
 * <li>import: allowing access by a remote component to classes in the calling bundle: useful when receiving a reference to an OSGi service that you want to be
 * able to load classes from the calling bundle;</li>
 * <li>export: allowing a local component to access classes in the bundle that loaded another component: useful when sending to another bundle an OSGi service
 * component that you want to be able to load classes from that other bundle.</li>
 * </ul>
 * <h3>Usage</h3>
 * Fluid Tool uses this component internally to inject an OSGi service as a dependency of a {@linkplain BundleComponentContainer.Managed managed component}.
 * Manual use of this component would only be necessary if you don't use such managed components.
 * <pre>
 * final <span class="hl1">BundleBoundary</span> boundary = &hellip;
 * final {@linkplain org.osgi.framework.BundleContext} context = &hellip;
 * final {@linkplain org.osgi.framework.ServiceReference}&lt;<span class="hl2">RemoteComponent</span>> reference = context.getServiceReference(<span class="hl2">RemoteComponent</span>.class);
 * final <span class="hl2">RemoteComponent</span> service = boundary.<span class="hl1">imported</span>(<span class="hl2">RemoteComponent</span>.class, context.getService(reference));
 * </pre>
 *
 * @author Tibor Varga
 */
public interface BundleBoundary {

    /**
     * Wraps the given object from another bundle to allow it to find classes in the calling bundle.
     * <p/>
     * The given object must implement the given interface and the given interface must be represented at run time by the same class in both bundles; i.e., it
     * must be exported by one bundle and imported by all others.
     *
     * @param type   the Java interface of the remote object.
     * @param remote the object from another bundle.
     *
     * @return the wrapped service.
     */
    <T> T imported(Class<T> type, T remote);

    /**
     * Wraps the given <code>local</code> object to allow it to load classes from the bundle of the provided <code>remote</code> object. If the
     * <code>remote</code> object is not known, the receiver of the <code>local</code> object must use the {@link #imported(Class, Object)} method instead.
     * <p/>
     * The <code>local</code> object must implement the given interface and the given interface must be represented at run time by the same class in both
     * bundles; i.e., it must be exported by one bundle and imported by all others.
     *
     * @param type   the Java interface of the local object.
     * @param remote the remote object, the bundle of which the local object must be able to find classes in.
     * @param local  the local object to be invoked by the client or any other object from its bundle.
     *
     * @return the wrapped local object.
     */
    <T> T exported(Class<T> type, Object remote, T local);

    /**
     * Invokes the given command with a context class loader that allows the <code>remote</code> object to find classes from the
     * bundle of the <code>local</code> object.
     *
     * @param remote  the remote object.
     * @param local   the local object.
     * @param command the command to allow with the tunneling class loader.
     * @param <T>     the return type of the command.
     * @param <E>     the exception type throws by the command.
     *
     * @return whatever the command returns.
     */
    <T, E extends Throwable> T invoke(Object remote, Object local, Command<T, E> command) throws E;

    /**
     * A command to be invoked by {@link BundleBoundary#invoke(Object, Object, BundleBoundary.Command) BundleBoundary.invoke()}.
     *
     * @param <T> the return type of the {@link #run()} method.
     * @param <E> the exception type thrown by the {@link #run()} method.
     */
    interface Command<T, E extends Throwable> {

        /**
         * Executes in the context of a tunneling class loader as described at {@link BundleBoundary#invoke(Object,
         * Object, BundleBoundary.Command) BundleBoundary.invoke()}.
         *
         * @return whatever the caller of {@link BundleBoundary#invoke(Object, Object, BundleBoundary.Command) BundleBoundary.invoke()} wants
         *         returned.
         *
         * @throws E whatever the caller of {@link BundleBoundary#invoke(Object, Object, BundleBoundary.Command) BundleBoundary.invoke()}
         *           accepts to be thrown.
         */
        T run() throws E;
    }
}
