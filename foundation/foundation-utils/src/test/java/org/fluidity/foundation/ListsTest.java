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

import java.io.Serializable;
import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ListsTest {

    @Test
    public void testNotNull() throws Exception {
        final Serializable[] list = Lists.notNull(Serializable.class, null);

        assert list != null;
        assert list.getClass().getComponentType() == Serializable.class;

        final Serializable[] array = new Serializable[1];
        assert Lists.notNull(Serializable.class, array) == array;
    }

    @Test
    public void testConcatenation() throws Exception {
        final Serializable[] array1 = new Serializable[] { new Item() };
        final Serializable[] array2 = null;
        final Serializable[] array3 = new Serializable[0];
        final Serializable[] array4 = new Serializable[] { new Item(), new Item() };
        final Serializable[] array5 = new Serializable[] { new Item(), new Item(), new Item() };

        final Serializable[] list = Lists.concatenate(Serializable.class, array1, array2, array3, array4, array5);

        assert list != null;
        assert list.length == 6 : list.length;

        assert list[0] == array1[0];
        assert list[1] == array4[0];
        assert list[2] == array4[1];
        assert list[3] == array5[0];
        assert list[4] == array5[1];
        assert list[5] == array5[2];
    }

    @Test
    public void testArrayConversion() throws Exception {
        final Item[] array = { new Item(), new Item(), new Item(), new Item(), new Item() };
        assert Arrays.equals(array, Lists.asArray(Item.class, Arrays.asList(array)));
    }

    private static class Item implements Serializable { }
}
