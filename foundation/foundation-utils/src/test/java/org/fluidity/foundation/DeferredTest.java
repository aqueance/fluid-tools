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

package org.fluidity.foundation;

import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DeferredTest {

    private Deferred.Reference<String> reference(final String original) {
        return Deferred.reference(new Deferred.Factory<String>() {
            private final AtomicBoolean invoked = new AtomicBoolean(false);

            public String create() {
                assert invoked.compareAndSet(false, true) : "Invoked multiple times";
                return original;
            }
        });
    }

    @Test
    public void testLazyLoading() throws Exception {
        final String original = "This is the original text";

        final Deferred.Reference<String> reference = reference(original);

        final String first = reference.get();
        assert original.equals(first) : first;

        final String second = reference.get();
        assert original.equals(second) : second;
    }

    @Test
    public void testNullValue() throws Exception {
        final Deferred.Reference<String> reference = reference(null);

        final String first = reference.get();
        assert first == null : first;

        final String second = reference.get();
        assert second == null : second;
    }
}
