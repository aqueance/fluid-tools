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

import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public class StringsTest extends MockGroup {

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

        final String string = verify(new Work<String>() {
            public String run() throws Exception {
                return Strings.printAnnotation(annotation);
            }
        });

        assert String.format("@%s", Documented.class.getSimpleName()).equals(string) : string;
    }

    @Test
    public void plainValueAnnotation() throws Exception {
        final Retention annotation = localMock(Retention.class);

        EasyMock.expect(annotation.annotationType()).andReturn((Class) Retention.class);
        EasyMock.expect(annotation.value()).andReturn(RetentionPolicy.RUNTIME);

        final String string = verify(new Work<String>() {
            public String run() throws Exception {
                return Strings.printAnnotation(annotation);
            }
        });

        assert String.format("@%s(%s)", Retention.class.getSimpleName(), RetentionPolicy.RUNTIME).equals(string) : string;
    }

    @Test
    public void arrayValueAnnotation() throws Exception {
        final Target annotation = localMock(Target.class);

        final ElementType[] value = { ElementType.FIELD, ElementType.METHOD };

        EasyMock.expect(annotation.annotationType()).andReturn((Class) Target.class);
        EasyMock.expect(annotation.value()).andReturn(value);

        final String string = verify(new Work<String>() {
            public String run() throws Exception {
                return Strings.printAnnotation(annotation);
            }
        });

        assert String.format("@%s({%s,%s})", Target.class.getSimpleName(), value[0], value[1]).equals(string) : string;
    }

    @Test
    public void classArrayValueAnnotation() throws Exception {
        final ClassValued annotation = localMock(ClassValued.class);

        EasyMock.expect(annotation.annotationType()).andReturn((Class) ClassValued.class);
        EasyMock.expect(annotation.value()).andReturn(Object[].class);

        final String string = verify(new Work<String>() {
            public String run() throws Exception {
                return Strings.printAnnotation(annotation);
            }
        });

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

        final String string = verify(new Work<String>() {
            public String run() throws Exception {
                return Strings.printAnnotation(annotation);
            }
        });

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

        final String string = verify(new Work<String>() {
            public String run() throws Exception {
                return Strings.printAnnotation(annotation);
            }
        });

        assert String.format("@%s.%s", getClass().getSimpleName(), MultiValued.class.getSimpleName()).equals(string) : string;
    }

    @Test
    public void testObjectId() throws Exception {
        final Object proxy1 = Proxies.create(getClass().getClassLoader(), new Class[] { Interface1.class, Interface2.class }, null);
        final Object proxy2 = Proxies.create(getClass().getClassLoader(), new Class[] { Interface1.class, Interface2.class }, null);
        final Object object3 = new Default();

        final String text1 = String.format("proxy@%x[%s,%s]", System.identityHashCode(proxy1), Interface1.class.getName(), Interface2.class.getName());
        final String text2 = String.format("proxy@%x[%s,%s]", System.identityHashCode(proxy2), Interface1.class.getName(), Interface2.class.getName());
        final String text3 = String.format("%s@%x", Default.class.getName(), System.identityHashCode(object3));

        check(text1, Strings.printObjectId(proxy1));
        check(text2, Strings.printObjectId(proxy2));
        check(text3, Strings.printObjectId(object3));
        check(String.format("[%s, %s, %s]", text1, text2, text3), Strings.printObjectId(new Object[] { proxy1, proxy2, object3 }));
    }

    @Test
    public void testObjectText() throws Exception {
        final Object proxy1 = Proxies.create(getClass().getClassLoader(), new Class[] { Interface1.class, Interface2.class }, null);
        final Object proxy2 = Proxies.create(getClass().getClassLoader(), new Class[] { Interface1.class, Interface2.class }, null);
        final Object object3 = new Default();
        final Object object4 = new Overridden();
        final Object object5 = new Default();

        final String text1 = String.format("proxy[%s,%s]", Interface1.class.getName(), Interface2.class.getName());
        final String text2 = String.format("proxy@%x[%s,%s]", System.identityHashCode(proxy2), Interface1.class.getName(), Interface2.class.getName());
        final String text3 = String.format("%s@%x", Default.class.getName(), System.identityHashCode(object3));
        final String text4 = object4.toString();
        final String text5 = String.format("%s", Default.class.getName());

        check(text1, Strings.printObject(false, proxy1));
        check(text2, Strings.printObject(true, proxy2));
        check(text3, Strings.printObject(true, object3));
        check(text4, Strings.printObject(false, object4));
        check(text5, Strings.printObject(false, object5));
        check(String.format("[%s, %s, %s, %s, %s]", text1, Strings.printObject(false, proxy2), Strings.printObject(false, object3), text4, text5),
              Strings.printObject(false, new Object[] { proxy1, proxy2, object3, object4, object5 }));
    }

    @Test
    public void testListSurrounding() throws Exception {
        final Strings.Listing listing = Strings.delimited();

        listing.add("item");
        listing.surround("|");
        assert "|item|".equals(listing.toString()) : listing;

        listing.surround("[]");
        assert "[|item|]".equals(listing.toString()) : listing;

        listing.surround("({})");
        assert "({[|item|]})".equals(listing.toString()) : listing;

        listing.surround("<|>");
        assert "<|({[|item|]})|>".equals(listing.toString()) : listing;

        listing.set("item");
        listing.prepend("[").append("]");
        assert "[item]".equals(listing.toString()) : listing;
    }

    private void check(final String expected, final String actual) {
        assert expected.equals(actual) : String.format("Expected %s, got %s", expected, actual);
    }

    private static @interface MultiValued {
        int id() default -1;
        String[] list() default { };
    }

    private static @interface ClassValued {
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
}