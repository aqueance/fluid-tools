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

package org.fluidity.foundation;

/**
 * Periodic updates facility. Components wishing to periodically update some data should call {@link #register(long, Snapshot)} upon initialization and then
 * use {@link Snapshot#get()} on the returned snapshot every time they wish to access the periodically updated data.
 * <p/>
 * This component is designed not to keep a hard reference to any object that registers for updates. Instead, the component using this one keeps a hard
 * reference to this component.
 * <p/>
 * The granularity of the updates can be configured by implementing a {@link org.fluidity.foundation.spi.PropertyProvider} component that returns a valid
 * number for the {@link #PERIOD_PROPERTY} key. The default period granularity is 1 second.
 *
 * @author Tibor Varga
 */
public interface Updates {

    /**
     * The configuration property that specifies the update granularity in milliseconds.
     */
    String PERIOD_PROPERTY = "org.fluidity.foundation.update.period.ms";

    /**
     * Registers an object to periodically update data with.
     * <p/>
     * The data update is implemented by the supplied <code>loader</code>, which will be invoked at most once every <code>period</code> seconds. The data will
     * be loaded once before this method returns and after that only if the {@link Snapshot#get()} method is invoked on the returned value.
     *
     * @param period the number of milliseconds that must pass between subsequent calls to {@link Snapshot#get()} on the provided <code>loader</code>.
     * @param loader the object that can refresh the data.
     *
     * @return an object through the up-to-date data can be obtained.
     */
    <T> Snapshot<T> register(long period, Snapshot<T> loader);

    /**
     * Represents some data that may be periodically refreshed by the {@link Updates} component.
     *
     * @param <T> the type of the data.
     *
     * @author Tibor Varga
     */
    interface Snapshot<T> {

        /**
         * Returns an up-to-date snapshot of the represented data.
         *
         * @return an up-to-date snapshot of the represented data.
         */
        T get();
    }

    /**
     * Provides settings to the {@link Updates} component.
     *
     * @author Tibor Varga
     */
    interface Settings {

        /**
         * The minimum number in milliseconds between subsequent calls to {@link Snapshot#get()} of a loader passed to {@link Updates#register(long,
         * Snapshot)}.
         *
         * @return a number greater than 0.
         */
        @Configuration.Property(key = Updates.PERIOD_PROPERTY, undefined = "1000")
        long period();
    }
}
