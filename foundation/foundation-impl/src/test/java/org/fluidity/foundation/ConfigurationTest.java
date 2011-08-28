/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.foundation.configuration.Configuration;
import org.fluidity.foundation.spi.PropertyProvider;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ConfigurationTest extends MockGroupAbstractTest {

    private final PropertyProvider provider = mock(PropertyProvider.class);
    private final Configuration.Definition definition = mock(Configuration.Definition.class);

    @SuppressWarnings("unchecked")
    private AtomicReference<PropertyProvider.PropertyChangeListener> configure(final Class settingsType) {
        EasyMock.expect(definition.value()).andReturn(settingsType);

        final AtomicReference<PropertyProvider.PropertyChangeListener> listener = new AtomicReference<PropertyProvider.PropertyChangeListener>();

        provider.addChangeListener(EasyMock.<PropertyProvider.PropertyChangeListener>notNull());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                listener.set((PropertyProvider.PropertyChangeListener) EasyMock.getCurrentArguments()[0]);
                return null;
            }
        });

        return listener;
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
    public void staticConfiguration() throws Exception {
        properties(provider, true, null, null, null, "value1", "value2", 5678);

        configure(Settings.class);

        replay();
        final Settings settings = new ConfigurationFactory.ConfigurationImpl<Settings>(definition, provider, null, (Configuration.Context[]) null).snapshot();
        verify();

        // properties must be cached, no reading should take place
        replay();
        checkSettings(settings, null, "default", "value1", "value2", 5678);
        verify();
    }

    @Test
    public void dynamicConfiguration() throws Exception {
        properties(provider, true, null, null, null, "value1", "value2", null);

        final AtomicReference<PropertyProvider.PropertyChangeListener> listener = configure(Settings.class);

        replay();
        final Configuration<Settings> configuration = new ConfigurationFactory.ConfigurationImpl<Settings>(definition, provider, null, (Configuration.Context[]) null);
        verify();

        // properties must be cached, no reading should take place
        replay();
        checkSettings(configuration.snapshot(), null, "default", "value1", "value2", 1234);
        verify();

        properties(provider, true, null, "value1", "value2", "value3", "value4", 5678);

        replay();
        listener.get().propertiesChanged(provider);
        verify();

        // properties must be cached, no reading should take place
        replay();
        checkSettings(configuration.snapshot(), "value1", "value2", "value3", "value4", 5678);
        verify();
    }

    @Test
    public void contextConfiguration() throws Exception {
        properties(provider, false, null, null, null, null, null, null);
        properties(provider, true, "context1.context2.context3", null, null, null, "value2", 5678);
        properties(provider, false, "context1.context2", null, null, null, null, null);
        properties(provider, false, "context1", null, null, "value1", null, null);

        final Configuration.Context context1 = localMock(Configuration.Context.class);
        final Configuration.Context context2 = localMock(Configuration.Context.class);
        final Configuration.Context context3 = localMock(Configuration.Context.class);

        EasyMock.expect(context1.value()).andReturn("context1");
        EasyMock.expect(context2.value()).andReturn("context2");
        EasyMock.expect(context3.value()).andReturn("context3");

        configure(Settings.class);

        replay();
        final Settings settings = new ConfigurationFactory.ConfigurationImpl<Settings>(definition, provider, null, context1, context2, context3).snapshot();
        verify();

        // properties must be cached, no reading should take place
        replay();
        checkSettings(settings, null, "default", "value1", "value2", 5678);
        verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void noConfiguration() throws Exception {
        EasyMock.expect(definition.value()).andReturn((Class) Settings.class);

        replay();
        final Settings settings = new ConfigurationFactory.ConfigurationImpl<Settings>(definition, null, null, (Configuration.Context[]) null).snapshot();
        verify();

        replay();
        checkSettings(settings, null, "default", null, "default", 1234);
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
        configure(ProvidedSettings.class);

        EasyMock.expect(provider.property("property")).andReturn("property");
        EasyMock.expect(provider.property("provided")).andReturn(null);
        EasyMock.expect(provider.property("undefined")).andReturn(null);

        replay();
        final ProvidedSettings settings = new ConfigurationFactory.ConfigurationImpl<ProvidedSettings>(definition, provider, new ProvidedSettingsImpl(), (Configuration.Context[]) null).snapshot();
        verify();

        assert "property".equals(settings.property()) : settings.property();
        assert "provided".equals(settings.provided()) : settings.provided();
        assert "undefined".equals(settings.undefined()) : settings.undefined();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void providedConfiguration() throws Exception {
        EasyMock.expect(definition.value()).andReturn((Class) ProvidedSettings.class);

        replay();
        final ProvidedSettings settings = new ConfigurationFactory.ConfigurationImpl<ProvidedSettings>(definition, null, new ProvidedSettingsImpl(), (Configuration.Context[]) null).snapshot();
        verify();

        assert "provided".equals(settings.property()) : settings.property();
        assert "provided".equals(settings.provided()) : settings.provided();
        assert "undefined".equals(settings.undefined()) : settings.undefined();
    }

    public interface CollectionSettings {

        @Configuration.Property(key = "integers", list = ",")
        int[] integers();

        @Configuration.Property(key = "integers.empty", list = ",")
        int[] empty_integers();

        @Configuration.Property(key = "integers.none", list = ",")
        int[] no_integers();

        @Configuration.Property(key = "flags", list = "|")
        boolean[] flags();

        @Configuration.Property(key = "flags.empty", list = "|")
        boolean[] empty_flags();

        @Configuration.Property(key = "flags.none", list = "|")
        boolean[] no_flags();

        @Configuration.Property(key = "strings", list = ":")
        List<String> strings();

        @Configuration.Property(key = "strings.empty", list = ":")
        List<String> empty_strings();

        @Configuration.Property(key = "strings.none", list = ":")
        List<String> no_strings();

        @Configuration.Property(key = "numbers", list = ",:", grouping = "<>")
        Map<String, Integer> numbers();

        @Configuration.Property(key = "numbers.empty", list = ":")
        Map<String, Integer> empty_numbers();

        @Configuration.Property(key = "numbers.none", list = ":")
        Map<String, Integer> no_numbers();

        @Configuration.Property(key = "insane", list = ",:")
        Map<List<Integer>, Map<String, List<long[]>>> insane();
    }

    @Test
    public void collectionConfiguration() throws Exception {
        configure(CollectionSettings.class);

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
        final CollectionSettings settings = new ConfigurationFactory.ConfigurationImpl<CollectionSettings>(definition, provider, null, (Configuration.Context[]) null).snapshot();
        verify();

        assert Arrays.equals(settings.integers(), new int[] { 1, 2, 3 }) : Arrays.toString(settings.integers());
        assert Arrays.equals(settings.empty_integers(), new int[0]) : Arrays.toString(settings.empty_integers());
        assert settings.no_integers() == null : Arrays.toString(settings.no_integers());

        assert Arrays.equals(settings.flags(), new boolean[] { true, false }) : Arrays.toString(settings.flags());
        assert Arrays.equals(settings.empty_flags(), new boolean[0]) : Arrays.toString(settings.empty_flags());
        assert settings.no_flags() == null : Arrays.toString(settings.no_flags());

        assert settings.strings().equals(Arrays.asList("good", "bad")) : settings.strings();
        assert settings.empty_strings().isEmpty() : settings.empty_strings();
        assert settings.no_strings() == null : settings.no_strings();

        final Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("key1", 12);
        map.put("key2", 34);

        assert settings.numbers().equals(map) : settings.numbers();
        assert settings.empty_numbers().isEmpty() : settings.empty_numbers();
        assert settings.no_numbers() == null : settings.no_numbers();

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
    }

    // TODO: test collection syntax errors

    public interface MultiTypeSettings {

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
    @SuppressWarnings("unchecked")
    public void defaultConversion() throws Exception {
        EasyMock.expect(definition.value()).andReturn((Class) MultiTypeSettings.class);

        replay();
        final MultiTypeSettings settings = new ConfigurationFactory.ConfigurationImpl<MultiTypeSettings>(definition, null, null, (Configuration.Context[]) null).snapshot();
        verify();

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
    }

    @Test
    public void propertyConversion() throws Exception {
        configure(MultiTypeSettings.class);

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
        final MultiTypeSettings settings = new ConfigurationFactory.ConfigurationImpl<MultiTypeSettings>(definition, provider, null, (Configuration.Context[]) null).snapshot();
        verify();

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
    }

    void collectionCheck(final Object expected, final Object actual) {
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

}
