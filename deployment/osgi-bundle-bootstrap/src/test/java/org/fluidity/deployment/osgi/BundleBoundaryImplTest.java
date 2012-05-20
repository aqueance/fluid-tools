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

import java.lang.reflect.Constructor;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class BundleBoundaryImplTest {

    @Test
    public void testClassLoading() throws Exception {
        final IsolatedClassLoader bundle1 = new IsolatedClassLoader("service", BundleBoundaryImplTest.class, BundleBoundary.class, ServiceProvider.class);
        final IsolatedClassLoader bundle2 = new IsolatedClassLoader("client", BundleBoundaryImplTest.class, BundleBoundary.class, ServiceProvider.class);

        final Class<?> providerClass = bundle1.loadClass(ServiceProviderImpl.class.getName());
        final Class<?> consumerClass = bundle2.loadClass(ServiceConsumerImpl.class.getName());

        assert providerClass.getClassLoader() == bundle1;
        assert consumerClass.getClassLoader() == bundle2;

        final ServiceProvider provider = (ServiceProvider) providerClass.newInstance();

        final Constructor<?> constructor = consumerClass.getDeclaredConstructor(ServiceProvider.class);

        final ServiceConsumer consumer = (ServiceConsumer) constructor.newInstance(customs(bundle2).imported(ServiceProvider.class, provider));

        checkClassLoader(bundle2, consumer);
        checkClassLoader(bundle2, (ServiceConsumer) constructor.newInstance(customs(bundle1).exported(ServiceProvider.class, consumer, provider)));
        checkClassLoader(bundle2, (ServiceConsumer) constructor.newInstance(customs(bundle1).exported(ServiceProvider.class, consumerClass, provider)));
    }

    private void checkClassLoader(final IsolatedClassLoader expected, final ServiceConsumer consumer) throws Exception {
        final String name = consumer.call();
        assert expected.name().equals(name) : name;
    }

    private BundleBoundary customs(final ClassLoader loader) throws Exception {
        final Constructor<?> borders = loader.loadClass(BundleBoundaryImpl.class.getName()).getDeclaredConstructor();
        borders.setAccessible(true);
        return (BundleBoundary) borders.newInstance();
    }

}
