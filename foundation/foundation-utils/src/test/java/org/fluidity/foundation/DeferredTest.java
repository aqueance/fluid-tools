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

package org.fluidity.foundation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DeferredTest {

    private Deferred.Reference<String> reference(final String original, final boolean safe) {
        final AtomicBoolean invoked = new AtomicBoolean(false);

        final Supplier<String> factory = () -> {
            assert invoked.compareAndSet(false, true) : "Invoked multiple times";
            return original;
        };

        return safe ? Deferred.shared(factory) : Deferred.local(factory);
    }

    @DataProvider(name = "safety")
    public Object[][] safety() {
        return new Object[][] {
                new Object[] { true },
                new Object[] { false },
        };
    }

    @Test(dataProvider = "safety")
    public void testLazyLoading(final boolean safe) throws Exception {
        final String original = "This is the original text";

        final Deferred.Reference<String> reference = reference(original, safe);

        assert !reference.resolved();

        final String first = reference.get();
        assert reference.resolved();
        assert original.equals(first) : first;

        final String second = reference.get();
        assert original.equals(second) : second;
    }

    @Test(dataProvider = "safety")
    public void testInvalidation(final boolean safe) throws Exception {
        final String original = "This is the original text";

        final Deferred.Reference<String> reference = reference(original, safe);

        reference.get();
        assert reference.resolved();

        reference.invalidate();
        assert !reference.resolved();
    }

    @Test(dataProvider = "safety")
    public void testNullValue(final boolean safe) throws Exception {
        final Deferred.Reference<String> reference = reference(null, safe);

        final String first = reference.get();
        assert first == null : first;

        final String second = reference.get();
        assert second == null : second;
    }

    @Test
    public void testLabels() throws Exception {
        final String text = "some text 1";

        final Object label1 = Deferred.label("some %s %d", "text", 1);

        assert text.equals(label1.toString()) : label1;

        final AtomicInteger counter = new AtomicInteger();

        final Object label2 = Deferred.label(() -> {
            counter.incrementAndGet();
            return text;
        });

        assert counter.get() == 0 : counter.get();
        assert text.equals(label2.toString()) : label2;
        assert counter.get() == 1 : counter.get();
        assert text.equals(label2.toString()) : label2;
        assert counter.get() == 1 : counter.get();
    }
}
