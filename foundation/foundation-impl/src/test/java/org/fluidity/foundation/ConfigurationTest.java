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

import java.util.ArrayList;
import java.util.List;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.configuration.Configuration;
import org.fluidity.foundation.configuration.Setting;
import org.fluidity.foundation.spi.PropertyProvider;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ConfigurationTest extends MockGroupAbstractTest {

    private PropertyProvider propertyProvider = mock(PropertyProvider.class);

    private final ComponentContainer container = new ContainerBoundary();

    @BeforeMethod
    public void setupTest() throws Exception {
        TestPropertyProvider.delegate = propertyProvider;
    }

    public static interface Settings {

        @Setting(key = "missing.key1")
        String missingValue1();

        @Setting(key = "missing.key2", undefined = "default")
        String missingValue2();

        @Setting(key = "valid.key1")
        String validValue1();

        @Setting(key = "valid.key2", undefined = "default")
        String validValue2();

        @Setting(key = "valid.key3", undefined = "1234")
        int validValue3();
    }

    @Test
    public void staticConfiguration() throws Exception {
        convertedProperties();
        properties(1, null, null, null, "value1", "value2", 5678);
        properties(0, "context1.context2.context3", null, null, null, null, null);
        properties(0, "context1.context2", null, null, null, null, null);
        properties(0, "context1", null, null, null, null, null);

        replay();

        // force reloading the properties
        TestPropertyProvider.reload();

        final Configured configured = container.getComponent(StaticConfigured.class);
        assert configured != null;
        configured.checkSettings(null, "default", "value1", "value2", 5678);

        verify();
    }

    @Test
    public void dynamicConfiguration() throws Exception {
        convertedProperties();
        properties(1, null, null, null, "value1", "value2", null);
        properties(0, "context1.context2.context3", null, null, null, null, null);
        properties(0, "context1.context2", null, null, null, null, null);
        properties(0, "context1", null, null, null, null, null);

        replay();

        // force reloading the properties
        TestPropertyProvider.reload();

        final Configured configured = container.getComponent(DynamicConfigured.class);
        assert configured != null;

        verify();

        // properties must be cached, no reading should take place
        replay();
        configured.checkSettings(null, "default", "value1", "value2", 1234);
        verify();

        convertedProperties();
        properties(1, null, "value1", "value2", "value3", "value4", 5678);
        properties(0, "context1.context2.context3", null, null, null, null, null);
        properties(0, "context1.context2", null, null, null, null, null);
        properties(0, "context1", null, null, null, null, null);

        // invoke the property change listeners
        replay();
        TestPropertyProvider.reload();
        verify();

        // properties must be cached, no reading should take place
        replay();
        configured.checkSettings("value1", "value2", "value3", "value4", 5678);
        verify();
    }

    @Test
    public void contextConfiguration() throws Exception {
        convertedProperties();
        properties(0, null, null, null, null, null, null);
        properties(1, "context1.context2.context3", null, null, null, "value2", 5678);
        properties(0, "context1.context2", null, null, null, null, null);
        properties(0, "context1", null, null, "value1", null, null);

        replay();

        // force reloading the properties
        TestPropertyProvider.reload();

        final Configured configured = container.getComponent(ContextConfigured.class);
        assert configured != null;
        configured.checkSettings(null, "default", "value1", "value2", 5678);

        verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void noConfiguration() throws Exception {
        final ComponentContext context = localMock(ComponentContext.class);
        final Configuration.Definition definition = localMock(Configuration.Definition.class);

        EasyMock.expect(context.annotation(EasyMock.same(Configuration.Definition.class), EasyMock.<Class>notNull())).andReturn(definition);
        EasyMock.expect(definition.value()).andReturn((Class) Settings.class);
        EasyMock.expect(context.annotations(Configuration.Context.class)).andReturn(null);

        replay();

        final Configured configured = new StaticConfigured(new ConfigurationImpl<Settings>(null, context));
        configured.checkSettings(null, "default", null, "default", 1234);

        verify();
    }

    @Test
    public void defaultConversion() throws Exception {
        TestPropertyProvider.delegate = new EmptyPropertyProvider();

        assert container.getComponent(MultiTypeConfigured1.class) != null : MultiTypeConfigured1.class;
    }

    @Test
    public void propertyConversion() throws Exception {
        properties(0, null, null, null, null, null, 5678);
        properties(0, "context1.context2.context3", null, null, null, null, null);
        properties(0, "context1.context2", null, null, null, null, null);
        properties(0, "context1", null, null, null, null, null);

        EasyMock.expect(propertyProvider.property("boolean")).andReturn(1);
        EasyMock.expect(propertyProvider.property("Boolean")).andReturn("1.1");
        EasyMock.expect(propertyProvider.property("byte")).andReturn(true);
        EasyMock.expect(propertyProvider.property("Byte")).andReturn(1.1);
        EasyMock.expect(propertyProvider.property("short")).andReturn("1.1");
        EasyMock.expect(propertyProvider.property("Short")).andReturn("true");
        EasyMock.expect(propertyProvider.property("int")).andReturn(null);
        EasyMock.expect(propertyProvider.property("Integer")).andReturn(null);
        EasyMock.expect(propertyProvider.property("long")).andReturn(null);
        EasyMock.expect(propertyProvider.property("Long")).andReturn(null);
        EasyMock.expect(propertyProvider.property("float")).andReturn(1);
        EasyMock.expect(propertyProvider.property("Float")).andReturn("1.25");      // must be exact
        EasyMock.expect(propertyProvider.property("double")).andReturn("1");
        EasyMock.expect(propertyProvider.property("Double")).andReturn("true");
        EasyMock.expect(propertyProvider.property("class")).andReturn(null);
        EasyMock.expect(propertyProvider.property("enum")).andReturn(null);

        replay();

        // force reloading the properties
        TestPropertyProvider.reload();

        assert container.getComponent(MultiTypeConfigured2.class) != null : MultiTypeConfigured2.class;

        verify();
    }

    private void properties(final int times,
                            final String context,
                            final Object missing1,
                            final Object missing2,
                            final String value1,
                            final String value2,
                            final Integer value3) {
        final String prefix = context == null ? "" : context.concat(".");

        EasyMock.expect(propertyProvider.property(prefix.concat("missing.key1"))).andReturn(missing1).times(times, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property(prefix.concat("missing.key2"))).andReturn(missing2).times(times, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property(prefix.concat("valid.key1"))).andReturn(value1).times(times, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property(prefix.concat("valid.key2"))).andReturn(value2).times(times, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property(prefix.concat("valid.key3"))).andReturn(value3).times(times, Integer.MAX_VALUE);
    }

    private void convertedProperties() {
        EasyMock.expect(propertyProvider.property("boolean")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("Boolean")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("byte")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("Byte")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("short")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("Short")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("int")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("Integer")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("long")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("Long")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("float")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("Float")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("double")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("Double")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("class")).andReturn(null).times(0, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property("enum")).andReturn(null).times(0, Integer.MAX_VALUE);
    }

    public static interface MultiTypeSettings {

        @Setting(key = "boolean", undefined = "true")
        boolean booleanValue();

        @Setting(key = "Boolean", undefined = "true")
        Boolean BooleanValue();

        @Setting(key = "byte", undefined = "123")
        byte byteValue();

        @Setting(key = "Byte", undefined = "-123")
        Byte ByteValue();

        @Setting(key = "short", undefined = "1234")
        short shortValue();

        @Setting(key = "Short", undefined = "-1234")
        Short ShortValue();

        @Setting(key = "int", undefined = "12345")
        int intValue();

        @Setting(key = "Integer", undefined = "-12345")
        Integer IntegerValue();

        @Setting(key = "long", undefined = "123456")
        long longValue();

        @Setting(key = "Long", undefined = "-123456")
        Long LongValue();

        @Setting(key = "float", undefined = "123456.25")
        float floatValue();

        @Setting(key = "Float", undefined = "-123456.25")
        Float FloatValue();

        @Setting(key = "double", undefined = "1234567.25")
        double doubleValue();

        @Setting(key = "Double", undefined = "-1234567.25")
        Double DoubleValue();

        @Setting(key = "class", undefined = "java.lang.Object")
        Class classValue();

        @Setting(key = "enum", undefined = "SAMPLE")
        EnumType enumValue();
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

    // just for convenience
    private static abstract class Configured {

        protected void checkSettings(final Settings configuration,
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

        public abstract void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2, final int valid3);
    }

    @Component
    public static class StaticConfigured extends Configured {

        private final Settings configuration;

        public StaticConfigured(final @Configuration.Definition(Settings.class) Configuration<Settings> settings) {
            configuration = settings.snapshot();
            assert configuration != null;
        }

        public void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2, final int valid3) {
            checkSettings(configuration, missing1, missing2, valid1, valid2, valid3);
        }

    }

    @Component
    public static class DynamicConfigured extends Configured {

        private final Configuration<Settings> settings;

        public DynamicConfigured(final @Configuration.Definition(Settings.class) Configuration<Settings> settings) {
            this.settings = settings;
        }
        public void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2, final int valid3) {
            checkSettings(settings.snapshot(), missing1, missing2, valid1, valid2, valid3);
        }

    }

    @Component
    @Configuration.Context("context1")
    public static class ContextConfigured extends Configured {

        private final Settings configuration;

        @Configuration.Context("context2")
        public ContextConfigured(final @Configuration.Definition(Settings.class) @Configuration.Context("context3") Configuration<Settings> settings) {
            configuration = settings.snapshot();
            assert configuration != null;
        }

        public void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2, final int valid3) {
            checkSettings(configuration, missing1, missing2, valid1, valid2, valid3);
        }
    }

    @Component
    public static class TestPropertyProvider implements PropertyProvider {

        public static TestPropertyProvider instance;
        public static PropertyProvider delegate;

        private final List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();

        public TestPropertyProvider() {
            assert TestPropertyProvider.instance == null;
            TestPropertyProvider.instance = this;
        }

        public Object property(final String key) {
            return delegate.property(key);
        }

        public void addChangeListener(final PropertyChangeListener listener) {
            listeners.add(listener);
        }

        public static void reload() {
            if (instance != null) {
                instance.notifyListeners();
            }
        }

        private void notifyListeners() {
            for (final PropertyChangeListener listener : listeners) {
                listener.propertiesChanged(this);
            }
        }
    }

    @Component(automatic = false)
    public static class EmptyPropertyProvider implements PropertyProvider {

        public Object property(final String key) {
            return null;
        }

        public void addChangeListener(final PropertyChangeListener listener) {
            // ignore
        }
    }

    @Component
    public static class MultiTypeConfigured1 {

        public MultiTypeConfigured1(final @Configuration.Definition(MultiTypeSettings.class) Configuration<MultiTypeSettings> settings) {
            final MultiTypeSettings configuration = settings.snapshot();
            assert configuration != null;

            assert configuration.booleanValue();
            assert configuration.BooleanValue();
            assert configuration.byteValue() == 123 : configuration.byteValue();
            assert configuration.ByteValue() == -123 : configuration.ByteValue();
            assert configuration.shortValue() == 1234 : configuration.shortValue();
            assert configuration.ShortValue() == -1234 : configuration.ShortValue();
            assert configuration.intValue() == 12345 : configuration.intValue();
            assert configuration.IntegerValue() == -12345 : configuration.IntegerValue();
            assert configuration.longValue() == 123456 : configuration.longValue();
            assert configuration.LongValue() == -123456 : configuration.LongValue();
            assert configuration.floatValue() == 123456.25 : configuration.floatValue();
            assert configuration.FloatValue() == -123456.25 : configuration.FloatValue();
            assert configuration.doubleValue() == 1234567.25 : configuration.doubleValue();
            assert configuration.DoubleValue() == -1234567.25 : configuration.DoubleValue();
            assert configuration.classValue() == Object.class : configuration.classValue();
            assert configuration.enumValue() == EnumType.SAMPLE : configuration.enumValue();
        }
    }

    @Component
    public static class MultiTypeConfigured2 {

        public MultiTypeConfigured2(final @Configuration.Definition(MultiTypeSettings.class) Configuration<MultiTypeSettings> settings) {
            final MultiTypeSettings configuration = settings.snapshot();
            assert configuration != null;

            assert configuration.booleanValue();
            assert configuration.BooleanValue();
            assert configuration.byteValue() == 1 : configuration.byteValue();
            assert configuration.ByteValue() == 1 : configuration.ByteValue();
            assert configuration.shortValue() == 1 : configuration.shortValue();
            assert configuration.ShortValue() == 1 : configuration.ShortValue();
            assert configuration.intValue() == 12345 : configuration.intValue();
            assert configuration.IntegerValue() == -12345 : configuration.IntegerValue();
            assert configuration.longValue() == 123456 : configuration.longValue();
            assert configuration.LongValue() == -123456 : configuration.LongValue();
            assert configuration.floatValue() == 1 : configuration.floatValue();
            assert configuration.FloatValue() == 1.25 : configuration.FloatValue();
            assert configuration.doubleValue() == 1 : configuration.doubleValue();
            assert configuration.DoubleValue() == 1 : configuration.DoubleValue();
            assert configuration.classValue() == Object.class : configuration.classValue();
            assert configuration.enumValue() == EnumType.SAMPLE : configuration.enumValue();
        }
    }
}
