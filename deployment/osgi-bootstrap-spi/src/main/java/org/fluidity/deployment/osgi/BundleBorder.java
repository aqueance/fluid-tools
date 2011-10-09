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
 * Enables cross-bundle class loading in an OSGi container. More specifically, this component allows:
 * <ul>
 * <li>importing a remote component into the calling bundle and allows it to find classes in this bundle: useful when receiving a reference to an OSGi service
 * to allow it to load classes from the receiving bundle;</li>
 * <li>exporting a local component for access by another to allow the local component load classes from the bundle of the other: useful when sending an OSGi
 * service component to another bundle to allow it to load classes from that other bundle.</li>
 * </ul>
 *
 * @author Tibor Varga
 */
public interface BundleBorder {

    /**
     * Wraps the given object from another bundle to allow it to find classes in this bundle.
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
     *
     * @param type   the Java interface of the local object.
     * @param remote the remote object, the bundle of which the local object must be able to find classes in.
     * @param local  the local object to be invoked by the client or any other object from its bundle.
     *
     * @return the wrapped local object.
     */
    <T> T exported(Class<T> type, Object remote, T local);
}
