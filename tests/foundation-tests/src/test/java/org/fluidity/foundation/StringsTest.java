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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public class StringsTest extends Simulator {

    private String className(final Class<?> type) {
        return type.getName().replace('$', '.');
    }

    @Test
    public void primitiveType() throws Exception {
        final String string = Strings.formatClass(true, true, Integer.TYPE);
        assert String.format("type %s", Integer.TYPE).equals(string) : string;
    }

    @Test
    public void ordinaryType() throws Exception {
        final String string = Strings.formatClass(true, true, Object.class);
        assert Object.class.toString().equals(string) : string;
    }

    @Test
    public void ordinaryName() throws Exception {
        final String string = Strings.formatClass(false, true, Object.class);
        assert className(Object.class).equals(string) : string;
    }

    @Test
    public void oneDimensionalArray() throws Exception {
        final String string = Strings.formatClass(true, true, String[].class);
        assert String.format("%s[]", String.class).equals(string) : string;
    }

    @Test
    public void threeDimensionalArray() throws Exception {
        final String string = Strings.formatClass(true, true, String[][][].class);
        assert String.format("%s[][][]", String.class).equals(string) : string;
    }

    @Test
    public void nakedAnnotation() throws Exception {
        final Documented annotation = arguments().normal(Documented.class);

        EasyMock.expect(annotation.annotationType()).andReturn((Class) Documented.class);

        final String string = verify(() -> Strings.describeAnnotation(false, annotation));

        assert String.format("@%s", Documented.class.getSimpleName()).equals(string) : string;
    }

    @Test
    public void plainValueAnnotation() throws Exception {
        final Retention annotation = arguments().normal(Retention.class);

        EasyMock.expect(annotation.annotationType()).andReturn((Class) Retention.class);
        EasyMock.expect(annotation.value()).andReturn(RetentionPolicy.RUNTIME);

        final String string = verify(() -> Strings.describeAnnotation(false, annotation));

        assert String.format("@%s(%s)", Retention.class.getSimpleName(), RetentionPolicy.RUNTIME).equals(string) : string;
    }

    @Test
    public void arrayValueAnnotation() throws Exception {
        final Target annotation = arguments().normal(Target.class);

        final ElementType[] value = { ElementType.FIELD, ElementType.METHOD };

        EasyMock.expect(annotation.annotationType()).andReturn((Class) Target.class);
        EasyMock.expect(annotation.value()).andReturn(value);

        final String string = verify(() -> Strings.describeAnnotation(false, annotation));

        assert String.format("@%s({%s,%s})", Target.class.getSimpleName(), value[0], value[1]).equals(string) : string;
    }

    @Test
    public void classArrayValueAnnotation() throws Exception {
        final ClassValued annotation = arguments().normal(ClassValued.class);

        EasyMock.expect(annotation.annotationType()).andReturn((Class) ClassValued.class);
        EasyMock.expect(annotation.value()).andReturn(Object[].class);

        final String string = verify(() -> Strings.describeAnnotation(false, annotation));

        assert String.format("@%s.%s(Object[].class)", getClass().getSimpleName(), ClassValued.class.getSimpleName()).equals(string) : string;
    }

    @Test
    public void multiValueAnnotation() throws Exception {
        final MultiValued annotation = arguments().normal(MultiValued.class);

        final int id = 1234;
        final String[] list = { "abcd", "efgh", "ijkl" };

        EasyMock.expect(annotation.annotationType()).andReturn((Class) MultiValued.class);
        EasyMock.expect(annotation.id()).andReturn(id);
        EasyMock.expect(annotation.list()).andReturn(list);

        final String string = verify(() -> Strings.describeAnnotation(false, annotation));

        assert String.format("@%s.%s(id=%d, list={%s,%s,%s})", getClass().getSimpleName(), MultiValued.class.getSimpleName(), id, list[0], list[1], list[2]).equals(string) : string;
    }

    @Test
    public void defaultValueAnnotation() throws Exception {
        final MultiValued annotation = arguments().normal(MultiValued.class);

        final int id = -1;
        final String[] list = { };

        EasyMock.expect(annotation.annotationType()).andReturn((Class) MultiValued.class);
        EasyMock.expect(annotation.id()).andReturn(id);
        EasyMock.expect(annotation.list()).andReturn(list);

        final String string = verify(() -> Strings.describeAnnotation(false, annotation));

        assert String.format("@%s.%s", getClass().getSimpleName(), MultiValued.class.getSimpleName()).equals(string) : string;
    }

    @Test
    public void testObjectId() throws Exception {
        final Object proxy1 = Proxies.create(getClass().getClassLoader(), new Class[] { Interface1.class, Interface2.class }, null);
        final Object proxy2 = Proxies.create(getClass().getClassLoader(), new Class[] { Interface1.class, Interface2.class }, null);
        final Object object3 = new Default();

        final String text1 = String.format("proxy@%x[%s,%s]", System.identityHashCode(proxy1), className(Interface1.class), className(Interface2.class));
        final String text2 = String.format("proxy@%x[%s,%s]", System.identityHashCode(proxy2), className(Interface1.class), className(Interface2.class));
        final String text3 = String.format("%s@%x", className(Default.class), System.identityHashCode(object3));

        check(text1, Strings.formatId(proxy1));
        check(text2, Strings.formatId(proxy2));
        check(text3, Strings.formatId(object3));
        check(String.format("[%s, %s, %s]", text1, text2, text3), Strings.formatId(new Object[] { proxy1, proxy2, object3 }));
    }

    @Test
    public void testObjectText() throws Exception {
        final Object proxy1 = Proxies.create(getClass().getClassLoader(), new Class[] { Interface1.class, Interface2.class }, null);
        final Object proxy2 = Proxies.create(getClass().getClassLoader(), new Class[] { Interface1.class, Interface2.class }, null);
        final Object object3 = new Default();
        final Object object4 = new Overridden();
        final Object object5 = new Default();

        final String text1 = String.format("proxy[%s,%s]", className(Interface1.class), className(Interface2.class));
        final String text2 = String.format("proxy@%x[%s,%s]", System.identityHashCode(proxy2), className(Interface1.class), className(Interface2.class));
        final String text3 = String.format("%s@%x", className(Default.class), System.identityHashCode(object3));
        final String text4 = object4.toString();
        final String text5 = String.format("%s", className(Default.class));

        check(text1, Strings.formatObject(false, true, proxy1));
        check(text2, Strings.formatObject(true, true, proxy2));
        check(text3, Strings.formatObject(true, true, object3));
        check(text4, Strings.formatObject(false, true, object4));
        check(text5, Strings.formatObject(false, true, object5));
        check(String.format("[%s, %s, %s, %s, %s]", text1, Strings.formatObject(false, true, proxy2), Strings.formatObject(false, true, object3), text4, text5),
              Strings.formatObject(false, true, new Object[] { proxy1, proxy2, object3, object4, object5 }));
    }

    @Test
    public void testClassNamesWithDollarSign() throws Exception {
        checkClassName(Name$1.Name$2.class, "StringsTest.Name$1.Name$2", false, false);
        checkClassName(Name$1.Name$2.class, "class StringsTest.Name$1.Name$2", true, false);
        checkClassName(Name$1.Name$2.class, "org.fluidity.foundation.StringsTest.Name$1.Name$2", false, true);
        checkClassName(Name$1.Name$2.class, "class org.fluidity.foundation.StringsTest.Name$1.Name$2", true, true);
    }

    @Test
    public void testCustomAnnotationIdentity() throws Exception {
        final CustomAnnotationImpl annotation1 = new CustomAnnotationImpl("1");
        final CustomAnnotationImpl annotation2 = new CustomAnnotationImpl("2");

        final String nirvana1 = Strings.describeAnnotation(false, annotation1);
        final String nirvana2 = Strings.describeAnnotation(false, annotation2);

        assert nirvana1.equals(nirvana2) : String.format("%s != %s", nirvana1, nirvana2);

        final String identity1 = Strings.describeAnnotation(true, annotation1);
        final String identity2 = Strings.describeAnnotation(true, annotation2);

        assert !identity1.equals(identity2) : String.format("%s == %s", identity1, identity2);
    }

    private void checkClassName(final Class<?> type, final String expected, final boolean textual, final boolean qualified) {
        final String actual = Strings.formatClass(textual, qualified, type);
        assert expected.equals(actual) : String.format("Expected '%s', got '%s'", expected, actual);
    }

    private void check(final String expected, final String actual) {
        assert expected.equals(actual) : String.format("Expected %s, got %s", expected, actual);
    }

    private @interface MultiValued {
        int id() default -1;
        String[] list() default { };
    }

    private @interface ClassValued {
        Class value() default Object.class;
    }

    private interface Interface1 { }

    private interface Interface2 { }

    private static class Default { }

    private static class Overridden {

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Name$1 {
        private static class Name$2 { }
    }

    // A custom annotation type with a value.
    private interface CustomAnnotation extends Annotation {

        String value();
    }

    // A custom annotation that hides its values.
    private static class CustomAnnotationImpl implements CustomAnnotation {

        private final String value;

        private CustomAnnotationImpl(final String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return CustomAnnotation.class;
        }

        @Override
        public String toString() {
            return "@" + annotationType().getSimpleName();
        }
    }
}
