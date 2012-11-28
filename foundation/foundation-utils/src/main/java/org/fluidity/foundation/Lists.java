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

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * Utilities related to arrays.
 * <h3>Usage Example</h3>
 * <pre>
 * final <span class="hl3">String</span> <span class="hl2">items</span> = <span class="hl1">Lists</span>.{@linkplain #delimited(String, Object...) delimited}("item 1", "item 2", "item 3");
 * final <span class="hl3">String</span>[] array = <span class="hl1">Lists</span>.{@linkplain #asArray(Class, Collection) asArray}(<span class="hl3">{@linkplain String}</span>.class, {@linkplain java.util.Arrays Arrays}.{@linkplain java.util.Arrays#asList(Object[]) asList}(<span class="hl2">items</span>.{@linkplain String#split(String) split}("\\s*,\\s*")));
 * </pre>
 *
 * @author Tibor Varga
 */
public class Lists extends Utility {

    private Lists() { }

    /**
     * Returns either the <code>array</code> parameter or, if it is <code>null</code>, an empty array with the given component <code>type</code>.
     *
     * @param type  the component type of the array.
     * @param array the array.
     * @param <T>   the component type of the array.
     *
     * @return either the <code>array</code> parameter or, if it is <code>null</code>, an empty array with the given component <code>type</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] notNull(final Class<T> type, final T[] array) {
        return array == null ? (T[]) Array.newInstance(type, 0) : array;
    }

    /**
     * Concatenates the given list of <code>arrays</code>.
     *
     * @param type   the component type of the arrays; any item may be <code>null</code>.
     * @param arrays the array list.
     * @param <T>    the component type of the arrays.
     *
     * @return a new array containing the values from all non-<code>null</code> <code>arrays</code>.
     */
    public static <T> T[] concatenate(final Class<T> type, final T[]... arrays) {
        int length = 0;

        for (final T[] array : arrays) {
            length += array == null ? 0 : array.length;
        }

        @SuppressWarnings("unchecked")
        final T[] all = (T[]) Array.newInstance(type, length);

        int tail = 0;
        for (final T[] array : arrays) {
            int size = array == null ? 0 : array.length;

            if (size > 0) {
                System.arraycopy(array, 0, all, tail, size);
                tail += size;
            }
        }

        return all;
    }

    /**
     * Complements {@link java.util.Arrays#asList(Object[])} with a conversion to the opposite direction. This is a convenience method that calls {@link
     * Collection#toArray(Object[])} on the received <code>list</code> with the <code>list</code>'s sized array and, in most cases, does all the unchecked type
     * casts needed for the operation.
     *
     * @param type the item type of the list/array; may not be <code>null</code>.
     * @param list is the list to convert to an array; may be <code>null</code>.
     * @param <T>  the component type of the given <code>list</code> and the returned array.
     *
     * @return an array containing the elements of the given list; never <code>null</code>.
     */
    public static <T> T[] asArray(final Class<? super T> type, final Collection<T> list) {
        return asArray(type, true, list);
    }

    /**
     * Complements {@link java.util.Arrays#asList(Object[])} with a conversion to the opposite direction. This is a convenience method that calls {@link
     * Collection#toArray(Object[])} on the received <code>list</code> with the <code>list</code>'s sized array and, in most cases, does all the unchecked type
     * casts needed for the operation.
     *
     * @param type  the item type of the list/array; may not be <code>null</code>.
     * @param array tells whether a <code>null</code> or empty list should be returned as an empty array (<code>true</code>) or <code>null</code> (<code>false</code>).
     * @param list  is the list to convert to an array; may be <code>null</code>.
     * @param <T>   the component type of the given <code>list</code> and the returned array.
     *
     * @return an array containing the elements of the given list; may be <code>null</code> if <code>empty</code> is <code>false</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] asArray(final Class<? super T> type, final boolean array, final Collection<T> list) {
        return list == null || list.isEmpty() ? array ? (T[]) Array.newInstance(type, 0) : null : list.toArray((T[]) Array.newInstance(type, list.size()));
    }

    /**
     * Returns a comma {@linkplain Lists.Delimited delimited list}.
     *
     * @return a comma {@linkplain Lists.Delimited delimited list}.
     */
    public static Delimited delimited() {
        return delimited(", ");
    }

    /**
     * Returns a comma delimited list of the given items.
     *
     * @return a comma delimited list of the given items.
     */
    public static String delimited(final Collection<?> items) {
        return delimited().list(items).toString();
    }

    /**
     * Returns a comma delimited list of the given items.
     *
     * @return a comma delimited list of the given items.
     */
    public static String delimited(final Object... items) {
        return delimited().list(items).toString();
    }

    /**
     * Returns {@linkplain Lists.Delimited delimited list} with the given delimiter.
     *
     * @return {@linkplain Lists.Delimited delimited list} with the given delimiter.
     */
    public static Delimited delimited(final String delimiter) {
        return new Delimited(delimiter);
    }

    /**
     * Returns a delimited list of the given items.
     *
     * @return a delimited list of the given items.
     */
    public static String delimited(final String delimiter, final Collection<?> items) {
        return new Delimited(delimiter).list(items).toString();
    }

    /**
     * Returns a delimited list of the given items.
     *
     * @return a delimited list of the given items.
     */
    public static String delimited(final String delimiter, final Object... items) {
        return new Delimited(delimiter).list(items).toString();
    }

    /**
     * A string listing tool that makes working with a {@link StringBuilder} simpler when collecting list items.
     *
     * @author Tibor Varga
     */
    public static final class Delimited {

        /**
         * The underlying {@link StringBuilder} object.
         */
        public final StringBuilder text = new StringBuilder();

        private final String delimiter;

        /**
         * Creates a new string listing tool with the given delimiter.
         *
         * @param delimiter the delimiter.
         */
        Delimited(final String delimiter) {
            this.delimiter = delimiter;
        }

        /**
         * Appends the delimiter to the string builder if it is not empty.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder next() {
            return text.length() > 0 ? text.append(delimiter) : text;
        }

        /**
         * Prepends the delimiter to the string builder if it is not empty.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder previous() {
            return text.length() > 0 ? text.insert(0, delimiter) : text;
        }

        /**
         * Collects the given text, appending the delimiter as necessary.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder add(final Object text) {
            return next().append(text);
        }

        /**
         * Collects the given list of objects, appending the delimiter as necessary.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder list(final Collection<?> list) {
            for (final Object text : list) {
                next().append(text instanceof String ? text: Strings.formatObject(false, true, text));
            }

            return text;
        }

        /**
         * Collects the given list of objects, appending the delimiter as necessary.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder list(final Object... list) {
            for (final Object text : list) {
                next().append(text instanceof String ? text: Strings.formatObject(false, true, text));
            }

            return text;
        }

        /**
         * Sets the value of the listing.
         *
         * @param text the new value of the list.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder set(final Object text) {
            this.text.setLength(0);
            this.text.append(text);
            return this.text;
        }

        /**
         * Appends some text to the list <i>without</i> the delimiter.
         *
         * @param text the text to append to the list.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder append(final Object text) {
            return this.text.append(text);
        }

        /**
         * Copies the first half of <code>bracket</code> to the beginning of the list and the second half to the end.
         *
         * @param bracket the character pairs to surround the current value with; may have odd number of characters, in which case the middle one will be
         *                copied to both ends.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder surround(final String bracket) {
            if (!bracket.isEmpty()) {
                final int length = bracket.length();
                final int half = length >>> 1;
                surround(bracket.substring(0, half + length % 2), bracket.substring(half));
            }

            return text;
        }

        /**
         * Copies the <code>prefix</code> to the beginning of the list and <code>suffix</code> to the end.
         *
         * @param prefix the text to prepend to the current value.
         * @param suffix the text to append to the current value.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder surround(final String prefix, final String suffix) {
            return text.insert(0, prefix).append(suffix);
        }

        /**
         * Copies the <code>prefix</code> to the beginning of the list.
         *
         * @param prefix the text to prepend to the current value.
         *
         * @return the underlying {@link StringBuilder} object.
         */
        public StringBuilder prepend(final String prefix) {
            return text.insert(0, prefix);
        }

        /**
         * Tells if the underlying {@link StringBuilder} is empty.
         *
         * @return <code>true</code> if the underlying {@link StringBuilder} is empty; <code>false</code> otherwise.
         */
        public boolean isEmpty() {
            return text.length() == 0;
        }

        @Override
        public String toString() {
            return text.toString();
        }
    }
}
