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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContext;
import org.fluidity.foundation.spi.PropertyProvider;
import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public class ConfigurationTest extends MockGroup {

    private final PropertyProvider provider = new PropertyProvider() {
        private final PropertyProvider mock = mock(PropertyProvider.class);

        public Object property(final String key) {
            return mock.property(key);
        }

        public void properties(final Runnable reader) {
            reader.run();
        }
    };

    private final ComponentContext context = mock(ComponentContext.class);
    private final Component.Reference reference = mock(Component.Reference.class);

    @SuppressWarnings("unchecked")
    private <T> Configuration<T> configure(final Class<T> settingsType,
                                           final PropertyProvider provider,
                                           final T defaults,
                                           final Configuration.Context... contexts) {
        EasyMock.expect(context.annotations(Configuration.Context.class)).andReturn(contexts);
        EasyMock.expect(context.annotation(Component.Reference.class, Configuration.class)).andReturn(reference);
        EasyMock.expect(reference.parameter(0)).andReturn((Class) settingsType);

        replay();
        final Configuration<T> configuration = new ConfigurationImpl<T>(context, provider, defaults);
        verify();

        return configuration;
    }

    public interface Settings {

        @Configuration.Property(key = "missing.key1")
        String missingValue1();

        @Configuration.Property(key = "missing.key2", undefined = "default")
        String missingValue2();

        @Configuration.Property(key = "valid.key1")
        String validValue1();

        @Configuration.Property(key = "valid.key2", undefined = "default")
        String validValue2();

        @Configuration.Property(key = "valid.key3", undefined = "1234")
        int validValue3();
    }

    @Test
    public void contextConfiguration() throws Exception {
        final Configuration.Context context1 = localMock(Configuration.Context.class);
        final Configuration.Context context2 = localMock(Configuration.Context.class);
        final Configuration.Context context3 = localMock(Configuration.Context.class);

        EasyMock.expect(context1.value()).andReturn("context1");
        EasyMock.expect(context2.value()).andReturn("context2");
        EasyMock.expect(context3.value()).andReturn("context3");

        final Configuration<Settings> configuration = configure(Settings.class, provider, null, context1, context2, context3);

        properties(provider, false, null, null, null, null, null, null);
        properties(provider, true, "context1.context2.context3", null, null, null, "value2", 5678);
        properties(provider, false, "context2.context3", null, null, null, null, null);
        properties(provider, false, "context3", null, null, null, null, null);
        properties(provider, false, "context1.context2", null, null, null, null, null);
        properties(provider, false, "context1", null, null, "value1", null, null);

        replay();
        configuration.query(new Configuration.Query<Settings, Void>() {
            public Void read(final Settings settings) {
                checkSettings(settings, null, "default", "value1", "value2", 5678);
                return null;
            }
        });
        verify();
    }

    @Test
    public void noConfiguration() throws Exception {
        final Configuration<Settings> configuration = configure(Settings.class, null, null);

        replay();
        configuration.query(new Configuration.Query<Settings, Void>() {
            public Void read(final Settings settings) {
                checkSettings(settings, null, "default", null, "default", 1234);
                return null;
            }
        });
        verify();
    }

    public interface ProvidedSettings {

        @Configuration.Property(key = "property", undefined = "undefined")
        String property();

        @Configuration.Property(key = "undefined", undefined = "undefined")
        String undefined();

        @Configuration.Property(key = "provided", undefined = "undefined")
        String provided();
    }

    @Test
    public void overriddenConfiguration() throws Exception {
        final Configuration<ProvidedSettings> configuration = configure(ProvidedSettings.class, provider, new ProvidedSettingsImpl());

        EasyMock.expect(provider.property("property")).andReturn("property");
        EasyMock.expect(provider.property("provided")).andReturn(null);
        EasyMock.expect(provider.property("undefined")).andReturn(null);

        replay();
        configuration.query(new Configuration.Query<ProvidedSettings, Void>() {
            public Void read(final ProvidedSettings settings) {
                assert "property".equals(settings.property()) : settings.property();
                assert "provided".equals(settings.provided()) : settings.provided();
                assert "undefined".equals(settings.undefined()) : settings.undefined();

                return null;
            }
        });
        verify();
    }

    @Test
    public void providedConfiguration() throws Exception {
        final Configuration<ProvidedSettings> configuration = configure(ProvidedSettings.class, null, new ProvidedSettingsImpl());

        replay();
        configuration.query(new Configuration.Query<ProvidedSettings, Void>() {
            public Void read(final ProvidedSettings settings) {
                assert "provided".equals(settings.property()) : settings.property();
                assert "provided".equals(settings.provided()) : settings.provided();
                assert "undefined".equals(settings.undefined()) : settings.undefined();

                return null;
            }
        });
        verify();
    }

    public interface CollectionSettings {

        @Configuration.Property(key = "integers", split = ",")
        int[] integers();

        @Configuration.Property(key = "integers.empty", split = ",")
        int[] empty_integers();

        @Configuration.Property(key = "integers.none", split = ",")
        int[] no_integers();

        @Configuration.Property(key = "flags", split = "|")
        boolean[] flags();

        @Configuration.Property(key = "flags.empty", split = "|")
        boolean[] empty_flags();

        @Configuration.Property(key = "flags.none", split = "|")
        boolean[] no_flags();

        @Configuration.Property(key = "strings", split = ":")
        List<String> strings();

        @Configuration.Property(key = "strings.empty", split = ":")
        List<String> empty_strings();

        @Configuration.Property(key = "strings.none", split = ":")
        List<String> no_strings();

        @Configuration.Property(key = "numbers", split = ",:", grouping = "<>")
        Map<String, Integer> numbers();

        @Configuration.Property(key = "numbers.empty", split = ":")
        Map<String, Integer> empty_numbers();

        @Configuration.Property(key = "numbers.none", split = ":")
        Map<String, Integer> no_numbers();

        @Configuration.Property(key = "insane", split = ",:")
        Map<List<Integer>, Map<String, List<long[]>>> insane();
    }

    @Test
    public void collectionConfiguration() throws Exception {
        final Configuration<CollectionSettings> configuration = configure(CollectionSettings.class, provider, null);

        EasyMock.expect(provider.property("integers")).andReturn("1, 2, 3");
        EasyMock.expect(provider.property("integers.empty")).andReturn("");
        EasyMock.expect(provider.property("integers.none")).andReturn(null);
        EasyMock.expect(provider.property("flags")).andReturn("true | false");
        EasyMock.expect(provider.property("flags.empty")).andReturn("");
        EasyMock.expect(provider.property("flags.none")).andReturn(null);
        EasyMock.expect(provider.property("strings")).andReturn("good : bad");
        EasyMock.expect(provider.property("strings.empty")).andReturn("");
        EasyMock.expect(provider.property("strings.none")).andReturn(null);
        EasyMock.expect(provider.property("numbers")).andReturn("key1: 12, key2: 34");
        EasyMock.expect(provider.property("numbers.empty")).andReturn("");
        EasyMock.expect(provider.property("numbers.none")).andReturn(null);
//      Map<List<Integer>, Map<String, List<long[]>>>
        EasyMock.expect(provider.property("insane")).andReturn("[1,2]: {a: [[1, 1], [1, 2], [1, 3]], b: [[2, 1], [2, 2]], c: [[3, 1]]}, [3,4]: {d: []}, [5,6]: [], [7,8]: {e: [[5, 1], [5, 2]]}");

        replay();
        configuration.query(new Configuration.Query<CollectionSettings, Void>() {
            public Void read(final CollectionSettings settings) {
                checkObjects(new int[] { 1, 2, 3 }, settings.integers());
                checkObjects(new int[0], settings.empty_integers());
                checkObjects(null, settings.no_integers());

                checkObjects(new boolean[] { true, false }, settings.flags());
                checkObjects(new boolean[0], settings.empty_flags());
                checkObjects(null, settings.no_flags());

                checkObjects(Arrays.asList("good", "bad"), settings.strings());
                checkObjects(Collections.EMPTY_LIST, settings.empty_strings());
                checkObjects(null, settings.no_strings());

                final Map<String, Integer> map = new HashMap<String, Integer>();
                map.put("key1", 12);
                map.put("key2", 34);

                checkObjects(map, settings.numbers());
                checkObjects(Collections.EMPTY_MAP, settings.empty_numbers());
                checkObjects(null, settings.no_numbers());

                final Map<List<Integer>, Map<String, List<long[]>>> insane = new HashMap<List<Integer>, Map<String, List<long[]>>>();

                final HashMap<String, List<long[]>> value1 = new HashMap<String, List<long[]>>();
                value1.put("a", Arrays.asList(new long[][] { { 1L, 1L }, { 1L, 2L }, { 1L, 3L } }));
                value1.put("b", Arrays.asList(new long[][] { { 2L, 1L }, { 2L, 2L } }));
                value1.put("c", Arrays.asList(new long[][] { { 3L, 1L } }));
                insane.put(Arrays.asList(1, 2), value1);

                final HashMap<String, List<long[]>> value2 = new HashMap<String, List<long[]>>();
                value2.put("d", new ArrayList<long[]>());
                insane.put(Arrays.asList(3, 4), value2);

                insane.put(Arrays.asList(5, 6), new HashMap<String, List<long[]>>());

                final HashMap<String, List<long[]>> value3 = new HashMap<String, List<long[]>>();
                value3.put("e", Arrays.asList(new long[][] { { 5L, 1L }, { 5L, 2L } }));
                insane.put(Arrays.asList(7, 8), value3);

                collectionCheck(settings.insane(), insane);

                return null;
            }
        });
        verify();
    }

    // TODO: test collection syntax errors

    public interface MultiTypeSettings {

        @Configuration.Property(key = "undefined")
        long undefined();

        @Configuration.Property(key = "char", undefined = "x")
        char charValue();

        @Configuration.Property(key = "boolean", undefined = "true")
        boolean booleanValue();

        @Configuration.Property(key = "Boolean", undefined = "true")
        Boolean BooleanValue();

        @Configuration.Property(key = "byte", undefined = "123")
        byte byteValue();

        @Configuration.Property(key = "Byte", undefined = "-123")
        Byte ByteValue();

        @Configuration.Property(key = "short", undefined = "1234")
        short shortValue();

        @Configuration.Property(key = "Short", undefined = "-1234")
        Short ShortValue();

        @Configuration.Property(key = "int", undefined = "12345")
        int intValue();

        @Configuration.Property(key = "Integer", undefined = "-12345")
        Integer IntegerValue();

        @Configuration.Property(key = "long", undefined = "123456")
        long longValue();

        @Configuration.Property(key = "Long", undefined = "-123456")
        Long LongValue();

        @Configuration.Property(key = "float", undefined = "123456.25")
        float floatValue();

        @Configuration.Property(key = "Float", undefined = "-123456.25")
        Float FloatValue();

        @Configuration.Property(key = "double", undefined = "1234567.25")
        double doubleValue();

        @Configuration.Property(key = "Double", undefined = "-1234567.25")
        Double DoubleValue();

        @Configuration.Property(key = "class", undefined = "java.lang.Object")
        Class classValue();

        @Configuration.Property(key = "enum", undefined = "SAMPLE")
        EnumType enumValue();
    }

    @Test
    public void defaultConversion() throws Exception {
        final Configuration<MultiTypeSettings> configuration = configure(MultiTypeSettings.class, null, null);

        replay();
        configuration.query(new Configuration.Query<MultiTypeSettings, Void>() {
            public Void read(final MultiTypeSettings settings) {
                assert settings.undefined() == 0L : settings.undefined();
                assert settings.charValue() == 'x' : settings.charValue();
                assert settings.booleanValue();
                assert settings.BooleanValue();
                assert settings.byteValue() == (byte) 123 : settings.byteValue();
                assert settings.ByteValue() == (byte) -123 : settings.ByteValue();
                assert settings.shortValue() == (short) 1234 : settings.shortValue();
                assert settings.ShortValue() == (short) -1234 : settings.ShortValue();
                assert settings.intValue() == 12345 : settings.intValue();
                assert settings.IntegerValue() == -12345 : settings.IntegerValue();
                assert settings.longValue() == 123456l : settings.longValue();
                assert settings.LongValue() == -123456l : settings.LongValue();
                assert settings.floatValue() == 123456.25f : settings.floatValue();
                assert settings.FloatValue() == -123456.25f : settings.FloatValue();
                assert settings.doubleValue() == 1234567.25 : settings.doubleValue();
                assert settings.DoubleValue() == -1234567.25 : settings.DoubleValue();
                assert settings.classValue() == Object.class : settings.classValue();
                assert settings.enumValue() == EnumType.SAMPLE : settings.enumValue();
                return null;
            }
        });
        verify();
    }

    @Test
    public void propertyConversion() throws Exception {
        final Configuration<MultiTypeSettings> configuration = configure(MultiTypeSettings.class, provider, null);

        EasyMock.expect(provider.property("boolean")).andReturn(1);
        EasyMock.expect(provider.property("Boolean")).andReturn("1.1");
        EasyMock.expect(provider.property("byte")).andReturn(true);
        EasyMock.expect(provider.property("Byte")).andReturn(1.1);
        EasyMock.expect(provider.property("short")).andReturn("1.1");
        EasyMock.expect(provider.property("Short")).andReturn("true");
        EasyMock.expect(provider.property("int")).andReturn(null);
        EasyMock.expect(provider.property("Integer")).andReturn(null);
        EasyMock.expect(provider.property("long")).andReturn(null);
        EasyMock.expect(provider.property("Long")).andReturn(null);
        EasyMock.expect(provider.property("float")).andReturn(1);
        EasyMock.expect(provider.property("Float")).andReturn("1.25");      // must be exact
        EasyMock.expect(provider.property("double")).andReturn("1");
        EasyMock.expect(provider.property("Double")).andReturn("true");
        EasyMock.expect(provider.property("class")).andReturn(null);
        EasyMock.expect(provider.property("enum")).andReturn(null);

        replay();
        configuration.query(new Configuration.Query<MultiTypeSettings, Void>() {
            public Void read(final MultiTypeSettings settings) {
                assert settings.booleanValue();
                assert settings.BooleanValue();
                assert settings.byteValue() == (byte) 1 : settings.byteValue();
                assert settings.ByteValue() == (byte) 1 : settings.ByteValue();
                assert settings.shortValue() == (short) 1 : settings.shortValue();
                assert settings.ShortValue() == (short) 1 : settings.ShortValue();
                assert settings.intValue() == 12345 : settings.intValue();
                assert settings.IntegerValue() == -12345 : settings.IntegerValue();
                assert settings.longValue() == 123456l : settings.longValue();
                assert settings.LongValue() == -123456l : settings.LongValue();
                assert settings.floatValue() == 1f : settings.floatValue();
                assert settings.FloatValue() == 1.25f : settings.FloatValue();
                assert settings.doubleValue() == 1d : settings.doubleValue();
                assert settings.DoubleValue() == 1d : settings.DoubleValue();
                assert settings.classValue() == Object.class : settings.classValue();
                assert settings.enumValue() == EnumType.SAMPLE : settings.enumValue();

                return null;
            }
        });
        verify();
    }

    interface SubstitutedSettings {

        @Configuration.Property(key = "property.%s")
        String property1(String arg0);

        @Configuration.Property(key = "property.%s.%s", undefined = "value.%s.%s")
        String property2(String arg0, String arg1);
    }

    @DataProvider(name = "substitution-params")
    public Object[][] substitutionParams() {
        return new Object[][] {
                new Object[] {true},
                new Object[] {false},
        };
    }

    @Test(dataProvider = "substitution-params")
    public void testPropertyNameSubstitution(boolean useDefaults) throws Exception {
        final SubstitutedSettings defaults;

        if (useDefaults) {
            defaults = localMock(SubstitutedSettings.class);
        } else {
            defaults = null;
        }

        final Configuration<SubstitutedSettings> configuration = configure(SubstitutedSettings.class, provider, defaults);

        if (useDefaults) {
            EasyMock.expect(defaults.property2("a", "b")).andReturn("value.a.b");
        }

        EasyMock.expect(provider.property("property.abcd")).andReturn("value11");
        EasyMock.expect(provider.property("property.efgh")).andReturn("value12");
        EasyMock.expect(provider.property("property.abcd.efgh")).andReturn("value21");
        EasyMock.expect(provider.property("property.efgh.ijkl")).andReturn("value22");

        EasyMock.expect(provider.property("property.a.b")).andReturn(null);

        replay();
        configuration.query(new Configuration.Query<SubstitutedSettings, Void>() {
            public Void read(final SubstitutedSettings settings) {
                assert "value11".equals(settings.property1("abcd")) : settings.property1("abcd");
                assert "value12".equals(settings.property1("efgh")) : settings.property1("efgh");
                assert "value21".equals(settings.property2("abcd", "efgh")) : settings.property2("abcd", "efgh");
                assert "value22".equals(settings.property2("efgh", "ijkl")) : settings.property2("efgh", "ijkl");

                final String missing = settings.property2("a", "b");
                assert "value.a.b".equals(missing) : missing;

                return null;
            }
        });
        verify();
    }

    interface CustomSettings {

        @Configuration.Property(key = "nested")
        Nested nestedType();

        @Configuration.Property(key = "custom")
        Custom customType();

        @Configuration.Property(key = "interface")
        Interface interfaceType();

        @Configuration.Property(key = "dynamic")
        Dynamic dynamic();

        class Nested {

            @Configuration.Property(key = "number")
            public int number;

            public Nested() { }

            public Nested(final int number) {
                this.number = number;
            }
        }

        class Custom {

            @Configuration.Property(key = "nested")
            public Nested nested;

            public Custom() { }

            public Custom(final Nested nested) {
                this.nested = nested;
            }
        }

        interface Interface {

            @Configuration.Property(key = "number")
            int number();
        }

        interface Dynamic {

            @Configuration.Property(key = "number.%d.%s")
            int number(int parameter, String argument);
        }
    }

    @Test
    public void testCustomTypes() throws Exception {
        final Configuration<CustomSettings> configuration = configure(CustomSettings.class, provider, null);

        // nestedType()
        EasyMock.expect(provider.property("nested.number")).andReturn("1234");

        replay();
        configuration.query(new Configuration.Query<CustomSettings, Void>() {
            public Void read(final CustomSettings settings) {
                final int number = settings.nestedType().number;
                assert number == 1234 : number;
                return null;
            }
        });
        verify();
    }

    @Test
    public void testDynamicCustomTypes() throws Exception {
        final CustomSettings settings = configure(CustomSettings.class, provider, null).settings();

        // nestedType()
        EasyMock.expect(provider.property("dynamic.number.1.a")).andReturn("1234");
        EasyMock.expect(provider.property("dynamic.number.2.b")).andReturn("5678");

        replay();
        final int number1 = settings.dynamic().number(1, "a");
        final int number2 = settings.dynamic().number(2, "b");
        verify();

        assert number1 == 1234 : number1;
        assert number2 == 5678 : number2;
    }

    @Test
    public void testNestedCustomTypes() throws Exception {
        final Configuration<CustomSettings> configuration = configure(CustomSettings.class, provider, null);

        // nestedType()
        EasyMock.expect(provider.property("custom.nested.number")).andReturn("1234");

        replay();
        configuration.query(new Configuration.Query<CustomSettings, Void>() {
            public Void read(final CustomSettings settings) {
                final int number = settings.customType().nested.number;
                assert number == 1234 : number;
                return null;
            }
        });
        verify();
    }

    interface ListSettings {

        @Configuration.Property(key = "text.%s", ids ="ids", list="text.%s.value.%%s.string", split = ":")
        List<String> textList(String type);

        @Configuration.Property(key = "text.%s", ids ="ids")
        Set<String> textSet(String type);

        @Configuration.Property(key = "item", ids ="ids")
        Item1[] itemList();

        @Configuration.Property(key = "item", ids ="ids")
        Map<Integer, Item2> itemMap();

        @Configuration.Property(key = "map", ids ="ids")
        Map<String, String>[] stringMaps();

        final class Item1 {

            @Configuration.Property(key = "number")
            public int number;

            @Configuration.Property(key = "text")
            public String text;

            public Item1() {
                // required
            }

            public Item1(final int number, final String text) {
                this.number = number;
                this.text = text;
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                }

                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                final Item1 that = (Item1) o;
                return number == that.number && text.equals(that.text);
            }

            @Override
            public int hashCode() {
                int result = number;
                result = 31 * result + text.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return String.format("number: %d, text: %s", number, text);
            }
        }

        interface Item2 {

            @Configuration.Property(key = "number")
            public int number();

            @Configuration.Property(key = "text")
            public String text();
        }
    }

    @Test
    public void testListSetting() throws Exception {
        final Configuration<ListSettings> configuration = configure(ListSettings.class, provider, null);

        // textList()
        EasyMock.expect(provider.property("text.red.ids")).andReturn("1:2");
        EasyMock.expect(provider.property("text.red.value.1.string")).andReturn("value11");
        EasyMock.expect(provider.property("text.red.value.2.string")).andReturn("value12");

        // textSet()
        EasyMock.expect(provider.property("text.green.ids")).andReturn("1, 2");
        EasyMock.expect(provider.property("text.green.1")).andReturn("value21");
        EasyMock.expect(provider.property("text.green.2")).andReturn("value22");

        // itemList()
        EasyMock.expect(provider.property("item.ids")).andReturn("1, 2");
        EasyMock.expect(provider.property("item.1.number")).andReturn("12");
        EasyMock.expect(provider.property("item.1.text")).andReturn("23");
        EasyMock.expect(provider.property("item.2.number")).andReturn("34");
        EasyMock.expect(provider.property("item.2.text")).andReturn("45");

        // itemMap()
        EasyMock.expect(provider.property("item.ids")).andReturn("1, 2");
        EasyMock.expect(provider.property("item.1.number")).andReturn("12");
        EasyMock.expect(provider.property("item.1.text")).andReturn("23");
        EasyMock.expect(provider.property("item.2.number")).andReturn("34");
        EasyMock.expect(provider.property("item.2.text")).andReturn("45");

        // stringMaps()
        EasyMock.expect(provider.property("map.ids")).andReturn("1, 2");
        EasyMock.expect(provider.property("map.1")).andReturn("a: 1, b: 2");
        EasyMock.expect(provider.property("map.2")).andReturn("c: 3, d: 4");

        replay();
        configuration.query(new Configuration.Query<ListSettings, Void>() {
            public Void read(final ListSettings settings) {
                checkObjects(Arrays.asList("value11", "value12"), settings.textList("red"));
                checkObjects(new HashSet<String>(Arrays.asList("value21", "value22")), settings.textSet("green"));
                checkArrays(new ListSettings.Item1[] { new ListSettings.Item1(12, "23"), new ListSettings.Item1(34, "45") }, settings.itemList());

                final Map<Integer, ListSettings.Item2> itemMap = new HashMap<Integer, ListSettings.Item2>();
                itemMap.put(1, new MyItem2(12, "23"));
                itemMap.put(2, new MyItem2(34, "45"));

                checkObjects(itemMap, settings.itemMap());

                @SuppressWarnings({ "unchecked" })
                final Map<String, String>[] stringMaps = new Map[] { new HashMap(), new HashMap() };
                stringMaps[0].put("a", "1");
                stringMaps[0].put("b", "2");
                stringMaps[1].put("c", "3");
                stringMaps[1].put("d", "4");

                checkArrays(stringMaps, settings.stringMaps());

                return null;
            }
        });
        verify();
    }

    static void checkObjects(final Object expected, final Object actual) {
        if (expected == null) {
            assert actual == null : String.format("Expected null, got %s", actual);
        } else {
            final Class<?> type = expected.getClass();

            if (type.isArray()) {
                checkArrays(expected, actual);
            } else if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
                collectionCheck(expected, actual);
            } else {
                assert expected.equals(actual) : String.format("Expected %s, got %s", expected, actual);
            }
        }
    }

    private static void checkArrays(final Object expected, final Object actual) {
        assert actual != null : String.format("Expected %s, got null", array(expected));

        final int length = Array.getLength(actual);
        assert Array.getLength(expected) == length : String.format("Expected %d, got %d", Array.getLength(expected), length);

        for (int i = 0; i < length; ++i) {
            checkObjects(Array.get(expected, i), Array.get(actual, i));
        }
    }

    private static String array(final Object actual) {
        final Strings.Listing text = Strings.delimited();

        for (int i = 0, length = Array.getLength(actual); i < length; ++i) {
            text.next().append(Array.get(actual, i));
        }

        return text.surround("[]").toString();
    }

    private static void collectionCheck(final Object expected, final Object actual) {
        if (expected instanceof Map) {
            assert actual instanceof Map : actual;

            final int length = ((Map) expected).size();
            assert length == ((Map) actual).size() : String.format("%s: %s", length, ((Map) actual).size());

            for (final Object key : ((Map) expected).keySet()) {
                assert ((Map) actual).containsKey(key) : key;
                collectionCheck(((Map) expected).get(key), ((Map) actual).get(key));
            }
        } else if (expected instanceof Collection) {
            assert actual instanceof Collection : actual;
            collectionCheck(((Collection) expected).toArray(), ((Collection) actual).toArray());
        } else if (expected == null) {
            assert actual == null : actual;
        } else if (expected.getClass().isArray()) {
            assert actual != null && actual.getClass().isArray() : String.format("%s: %s", expected, actual);

            final int length = Array.getLength(expected);
            assert length == Array.getLength(actual) : String.format("%s: %s", Array.getLength(expected), Array.getLength(actual));

            for (int i = 0; i < length; ++i) {
                collectionCheck(Array.get(expected, i), Array.get(actual, i));
            }
        } else {
            assert expected.equals(actual) : String.format("%s: %s", expected, actual);
        }
    }

    private void properties(final PropertyProvider propertyProvider,
                            final boolean required,
                            final String context,
                            final Object missing1,
                            final Object missing2,
                            final String value1,
                            final String value2,
                            final Integer value3) {
        final String prefix = context == null ? "" : context.concat(".");
        final int times = required ? 1 : 0;

        EasyMock.expect(propertyProvider.property(prefix.concat("missing.key1"))).andReturn(missing1).times(times, 1);
        EasyMock.expect(propertyProvider.property(prefix.concat("missing.key2"))).andReturn(missing2).times(times, 1);
        EasyMock.expect(propertyProvider.property(prefix.concat("valid.key1"))).andReturn(value1).times(times, 1);
        EasyMock.expect(propertyProvider.property(prefix.concat("valid.key2"))).andReturn(value2).times(times, 1);
        EasyMock.expect(propertyProvider.property(prefix.concat("valid.key3"))).andReturn(value3).times(times, 1);
    }

    public static enum EnumType {
        SAMPLE
    }

    static void assertValue(final Object value, final Object expected) {
        if (expected == null) {
            assert value == null : String.format("Expected null, got >%s<", value);
        } else {
            assert expected.equals(value) : String.format("Expected %s, got >%s<", expected, value);
        }
    }

    static void checkSettings(final Settings configuration,
                              final String missing1,
                              final String missing2,
                              final String valid1,
                              final String valid2,
                              final int valid3) {
        assert configuration != null;

        assertValue(configuration.missingValue1(), missing1);
        assertValue(configuration.missingValue2(), missing2);

        assertValue(configuration.validValue1(), valid1);
        assertValue(configuration.validValue2(), valid2);
        assertValue(configuration.validValue3(), valid3);
    }

    public static class ProvidedSettingsImpl implements ProvidedSettings {

        public String property() {
            return "provided";
        }

        public String undefined() {
            return null;
        }

        public String provided() {
            return "provided";
        }
    }

    private static class MyItem2 implements ListSettings.Item2 {

        private final int number;
        private final String text;

        public MyItem2(final int number, final String text) {
            this.number = number;
            this.text = text;
        }

        public int number() {
            return number;
        }

        public String text() {
            return text;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ListSettings.Item2)) {
                return false;
            }

            final ListSettings.Item2 that = (ListSettings.Item2) o;
            return number == that.number() && text.equals(that.text());
        }

        @Override
        public int hashCode() {
            int result = number;
            result = 31 * result + text.hashCode();
            return result;
        }
    }
}
