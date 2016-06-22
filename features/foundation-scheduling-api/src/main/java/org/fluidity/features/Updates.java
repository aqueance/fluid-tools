/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.features;

import java.util.function.Supplier;

/**
 * Periodic update facility. Components wishing to periodically update some data should call {@link #snapshot(long, Supplier)} upon initialization and then
 * use {@link Supplier#get()} on the returned snapshot every time they wish to access the periodically updated data.
 * <p>
 * <b>NOTE</b>: This component is designed not to keep a hard reference to any object that registers for updates and thus is a preferred way to the
 * {@link Scheduler} component of implementing periodic updates.
 * <p>
 * The granularity of the updates can be configured by implementing a {@link org.fluidity.foundation.spi.PropertyProvider} component that returns a valid
 * number for the {@link #UPDATE_GRANULARITY} key. The default period granularity is 1 second.
 * <p>
 * See {@link org.fluidity.foundation.Configuration} for details on configuration.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * public final class <span class="hl2">MyComponent</span> {
 *
 *   private static final long period = {@linkplain java.util.concurrent.TimeUnit#MILLISECONDS}.{@linkplain java.util.concurrent.TimeUnit#convert(long, java.util.concurrent.TimeUnit) convert}(1, {@linkplain java.util.concurrent.TimeUnit#SECONDS});
 *
 *   private final <span class="hl1">Updates.Snapshot</span>&lt;<span class="hl3">Data</span>&gt; data;
 *
 *   <span class="hl2">MyComponent</span>(final <span class="hl1">Updates</span> updates) {
 *     data = updates.<span class="hl1">snapshot</span>(period, () -&gt; {
 *       &hellip;
 *       return new <span class="hl3">Data</span>(&hellip;);    // create or update the data
 *     });
 *   }
 *
 *   public int someMethod() {
 *     final <span class="hl3">Data</span> snapshot = data.<span class="hl1">get</span>();
 *     &hellip;
 *     return snapshot.&hellip;; // access the data snapshot
 *   }
 *
 *   &hellip;
 *
 *   private static class <span class="hl3">Data</span> {
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface Updates {

    /**
     * The configuration property that specifies the update granularity in milliseconds.
     */
    String UPDATE_GRANULARITY = "org.fluidity.features.update-granularity-ms";

    /**
     * Creates a periodically updated on-demand snapshot for some data. The the data is loaded by the given <code>loader</code>, which will be invoked <i>no
     * more frequently</i> than every <code>period</code> milliseconds.
     * <p>
     * The actual data update is implemented by the supplied <code>loader</code>, which will be invoked at most once every <code>period</code> milliseconds.
     * The data will be loaded once before this method returns and after that <i>only</i> when the {@link Supplier#get()} method is invoked on the returned
     * value.
     * <p>
     * The {@linkplain Updates#UPDATE_GRANULARITY update granularity} configured for this component will pose as the lower bound to any <i>positive</i>
     * <code>period</code> specified to this method.If the <code>period</code> specified is <code>0</code>, the snapshot will be taken once and then cached
     * forever. If the <code>period</code> is negative, no snapshot will be cached and the loader will be invoked at every invocation of {@link Supplier#get()}.
     *
     * @param <T>    the type of data the given <code>loader</code> provides.
     *
     * @param period the number of milliseconds that must pass between subsequent calls to {@link Supplier#get()} on the provided <code>loader</code>.
     * @param loader the object that can refresh the data.
     * @return an object through which the up-to-date data can be obtained.
     */
    <T> Supplier<T> snapshot(long period, Supplier<T> loader);
}
