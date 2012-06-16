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
@SuppressWarnings("unchecked")
public class StringsTest extends MockGroupAbstractTest {

    @Test
    public void ordinaryType() throws Exception {
        final String string = Strings.printClass(true, Object.class);
        assert Object.class.toString().equals(string) : string;
    }

    @Test
    public void ordinaryName() throws Exception {
        final String string = Strings.printClass(false, Object.class);
        assert Object.class.getName().equals(string) : string;
    }

    @Test
    public void oneDimensionalArray() throws Exception {
        final String string = Strings.printClass(true, String[].class);
        assert String.format("%s[]", String.class).equals(string) : string;
    }

    @Test
    public void threeDimensionalArray() throws Exception {
        final String string = Strings.printClass(true, String[][][].class);
        assert String.format("%s[][][]", String.class).equals(string) : string;
    }

    @Test
    public void nakedAnnotation() throws Exception {
        final Documented annotation = localMock(Documented.class);

        EasyMock.expect(annotation.annotationType()).andReturn((Class) Documented.class);

        replay();
        final String string = Strings.printAnnotation(annotation);
        verify();

        assert String.format("@%s", Documented.class.getSimpleName()).equals(string) : string;
    }

    @Test
    public void plainValueAnnotation() throws Exception {
        final Retention annotation = localMock(Retention.class);

        EasyMock.expect(annotation.annotationType()).andReturn((Class) Retention.class);
        EasyMock.expect(annotation.value()).andReturn(RetentionPolicy.RUNTIME);

        replay();
        final String string = Strings.printAnnotation(annotation);
        verify();

        assert String.format("@%s(%s)", Retention.class.getSimpleName(), RetentionPolicy.RUNTIME).equals(string) : string;
    }

    @Test
    public void arrayValueAnnotation() throws Exception {
        final Target annotation = localMock(Target.class);

        final ElementType[] value = { ElementType.FIELD, ElementType.METHOD };

        EasyMock.expect(annotation.annotationType()).andReturn((Class) Target.class);
        EasyMock.expect(annotation.value()).andReturn(value);

        replay();
        final String string = Strings.printAnnotation(annotation);
        verify();

        assert String.format("@%s({%s,%s})", Target.class.getSimpleName(), value[0], value[1]).equals(string) : string;
    }

    @Test
    public void classArrayValueAnnotation() throws Exception {
        final ClassValued annotation = localMock(ClassValued.class);

        EasyMock.expect(annotation.annotationType()).andReturn((Class) ClassValued.class);
        EasyMock.expect(annotation.value()).andReturn(Object[].class);

        replay();
        final String string = Strings.printAnnotation(annotation);
        verify();

        assert String.format("@%s.%s(Object[].class)", getClass().getSimpleName(), ClassValued.class.getSimpleName()).equals(string) : string;
    }

    @Test
    public void multiValueAnnotation() throws Exception {
        final MultiValued annotation = localMock(MultiValued.class);

        final int id = 1234;
        final String[] list = { "abcd", "efgh", "ijkl" };

        EasyMock.expect(annotation.annotationType()).andReturn((Class) MultiValued.class);
        EasyMock.expect(annotation.id()).andReturn(id);
        EasyMock.expect(annotation.list()).andReturn(list);

        replay();
        final String string = Strings.printAnnotation(annotation);
        verify();

        assert String.format("@%s.%s(id=%d, list={%s,%s,%s})", getClass().getSimpleName(), MultiValued.class.getSimpleName(), id, list[0], list[1], list[2]).equals(string) : string;
    }

    @Test
    public void defaultValueAnnotation() throws Exception {
        final MultiValued annotation = localMock(MultiValued.class);

        final int id = -1;
        final String[] list = { };

        EasyMock.expect(annotation.annotationType()).andReturn((Class) MultiValued.class);
        EasyMock.expect(annotation.id()).andReturn(id);
        EasyMock.expect(annotation.list()).andReturn(list);

        replay();
        final String string = Strings.printAnnotation(annotation);
        verify();

        assert String.format("@%s.%s", getClass().getSimpleName(), MultiValued.class.getSimpleName()).equals(string) : string;
    }

    private static @interface MultiValued {
        int id() default -1;
        String[] list() default { };
    }

    private static @interface ClassValued {
        Class value() default Object.class;
    }
}
