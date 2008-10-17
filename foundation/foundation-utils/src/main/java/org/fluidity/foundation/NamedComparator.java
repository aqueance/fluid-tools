/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package org.fluidity.foundation;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Defines a sort ordering between <code>Named</code> objects.
 *
 * @author Tibor Varga
 * @see org.fluidity.foundation.Named
 */
public class NamedComparator implements Comparator<Named> {

    private final Collator collator;

    private final Locale locale;

    private final boolean ignoreCase;

    public NamedComparator() {
        this(Collator.getInstance());
    }

    public NamedComparator(final Collator collator) {
        this.collator = collator;
        this.locale = null;
        this.ignoreCase = false;
    }

    public NamedComparator(final Locale locale, final boolean ignoreCase) {
        this.collator = Collator.getInstance(locale);
        this.locale = locale;
        this.ignoreCase = ignoreCase;
    }

    public int compare(final Named o1, final Named o2) {
        final String n1 = o1.name();
        final String n2 = o2.name();
        return collator.compare(ignoreCase ? n1.toLowerCase(locale) : n1, ignoreCase ? n2.toLowerCase(locale) : n2);
    }
}
