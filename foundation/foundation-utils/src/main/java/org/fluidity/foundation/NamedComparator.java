/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.foundation;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Defines a sort ordering between {@link Named} objects.
 *
 * @author Tibor Varga
 */
public class NamedComparator implements Comparator<Named> {

    private final Collator collator;

    private final Locale locale;

    private final boolean ignoreCase;

    public NamedComparator() {
        this(false);
    }

    public NamedComparator(final boolean ignoreCase) {
        this(Collator.getInstance(), ignoreCase);
    }

    public NamedComparator(final Collator collator, final boolean ignoreCase) {
        this.collator = collator;
        this.locale = null;
        this.ignoreCase = ignoreCase;
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
