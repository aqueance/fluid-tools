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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class StringsTests extends MockGroupAbstractTest {

    @Test
    public void ordinaryType() throws Exception {
        final String string = Strings.arrayNotation(Object.class);
        assert Object.class.toString().equals(string) : string;
    }

    @Test
    public void oneDimensionalArray() throws Exception {
        final String string = Strings.arrayNotation(String[].class);
        assert String.format("%s[]", String.class).equals(string) : string;
    }

    @Test
    public void threeDimensionalArray() throws Exception {
        final String string = Strings.arrayNotation(String[][][].class);
        assert String.format("%s[][][]", String.class).equals(string) : string;
    }

    @Test
    public void nakedAnnotation() throws Exception {
        final Documented annotation = addLocalNiceControl(Documented.class);

        replay();
        final String string = Strings.simpleNotation(annotation);
        verify();

        assert String.format("@%s", Documented.class.getSimpleName()).equals(string) : string;
    }

    @Test
    public void plainValueAnnotation() throws Exception {
        final Retention annotation = addLocalNiceControl(Retention.class);
        EasyMock.expect(annotation.value()).andReturn(RetentionPolicy.RUNTIME);

        replay();
        final String string = Strings.simpleNotation(annotation);
        verify();

        assert String.format("@%s(%s)", Retention.class.getSimpleName(), RetentionPolicy.RUNTIME).equals(string) : string;
    }

    @Test
    public void arrayValueAnnotation() throws Exception {
        final Target annotation = addLocalNiceControl(Target.class);

        final ElementType[] value = { ElementType.FIELD, ElementType.METHOD };

        EasyMock.expect(annotation.value()).andReturn(value);

        replay();
        final String string = Strings.simpleNotation(annotation);
        verify();

        assert String.format("@%s({%s,%s})", Target.class.getSimpleName(), value[0], value[1]).equals(string) : string;
    }

    @Test
    public void multiValueAnnotation() throws Exception {
        final MultiValued annotation = addLocalNiceControl(MultiValued.class);

        final int id = 1234;
        final String[] list = { "abcd", "efgh", "ijkl" };

        EasyMock.expect(annotation.id()).andReturn(id);
        EasyMock.expect(annotation.list()).andReturn(list);

        replay();
        final String string = Strings.simpleNotation(annotation);
        verify();

        assert String.format("@%s(id=%d, list={%s,%s,%s})", MultiValued.class.getSimpleName(), id, list[0], list[1], list[2]).equals(string) : string;
    }

    @Test
    public void defaultValueAnnotation() throws Exception {
        final MultiValued annotation = addLocalNiceControl(MultiValued.class);

        final int id = -1;
        final String[] list = { };

        EasyMock.expect(annotation.id()).andReturn(id);
        EasyMock.expect(annotation.list()).andReturn(list);

        replay();
        final String string = Strings.simpleNotation(annotation);
        verify();

        assert String.format("@%s([id=%d], [list={}])", MultiValued.class.getSimpleName(), id).equals(string) : string;
    }

    private static @interface MultiValued {
        int id() default -1;
        String[] list() default {};
    }
}
