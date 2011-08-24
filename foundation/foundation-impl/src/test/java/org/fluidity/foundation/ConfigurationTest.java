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
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.configuration.Configuration;
import org.fluidity.foundation.configuration.Setting;
import org.fluidity.foundation.spi.PropertyProvider;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ConfigurationTest extends MockGroupAbstractTest {

    private PropertyProvider propertyProvider = mock(PropertyProvider.class);

    private final ComponentContainer container = new ContainerBoundary();

    @BeforeClass
    public void setupClass() throws Exception {
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
    }

    @Test
    public void staticConfiguration() throws Exception {

        // must read up all properties defined for Settings interface methods.
        properties(1, null, null, null, "value1", "value2");
        properties(0, "context1.context2", null, null, "value1", "value2");

        replay();

        // force reloading the properties
        TestPropertyProvider.reload();

        final Configured configured = container.getComponent(StaticConfigured.class);
        assert configured != null;
        configured.checkSettings(null, "default", "value1", "value2");

        verify();
    }

    @Test
    public void dynamicConfiguration() throws Exception {

        properties(1, null, null, null, "value1", "value2");
        properties(0, "context1.context2", null, null, "value1", "value2");

        replay();

        // force reloading the properties
        TestPropertyProvider.reload();

        final Configured configured = container.getComponent(DynamicConfigured.class);
        assert configured != null;

        verify();

        // properties must be cached, no reading should take place
        replay();
        configured.checkSettings(null, "default", "value1", "value2");
        verify();

        properties(1, null, "value1", "value2", "value3", "value4");
        properties(0, "context1.context2", null, null, "value1", "value2");

        // invoke the property change listeners
        replay();
        TestPropertyProvider.reload();
        verify();

        // properties must be cached, no reading should take place
        replay();
        configured.checkSettings("value1", "value2", "value3", "value4");
        verify();
    }

    @Test
    public void contextConfiguration() throws Exception {

        properties(0, null, null, null, "value1", "value2");
        properties(1, "context1.context2", null, null, "value1", "value2");

        replay();

        // force reloading the properties
        TestPropertyProvider.reload();

        final Configured configured = container.getComponent(ContextConfigured.class);
        assert configured != null;
        configured.checkSettings(null, "default", "value1", "value2");

        verify();
    }

    // TODO: context fallback (context1.context2.xxx falls back if not defined to context1.xxx and then to xxx)

    private void properties(final int times, final String context, final Object missing1, final Object missing2, final String value1, final String value2) {
        final String prefix = context == null ? "" : context.concat(".");

        EasyMock.expect(propertyProvider.property(prefix.concat("missing.key1"))).andReturn(missing1).times(times, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property(prefix.concat("missing.key2"))).andReturn(missing2).times(times, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property(prefix.concat("valid.key1"))).andReturn(value1).times(times, Integer.MAX_VALUE);
        EasyMock.expect(propertyProvider.property(prefix.concat("valid.key2"))).andReturn(value2).times(times, Integer.MAX_VALUE);
    }

    public static interface MultiTypeSettings {

        @Setting(key = "boolean", undefined = "true")
        boolean booleanValue();

        @Setting(key = "Boolean", undefined = "true")
        Boolean BooleanValue();

        @Setting(key = "int", undefined = "123")
        byte byteValue();

        @Setting(key = "int", undefined = "-123")
        Byte ByteValue();

        @Setting(key = "int", undefined = "1234")
        short shortValue();

        @Setting(key = "int", undefined = "-1234")
        Short ShortValue();

        @Setting(key = "int", undefined = "12345")
        int intValue();

        @Setting(key = "int", undefined = "-12345")
        Integer IntegerValue();

        @Setting(key = "int", undefined = "123456")
        long longValue();

        @Setting(key = "int", undefined = "-123456")
        Long LongValue();

        @Setting(key = "float", undefined = "123456.25")
        float floatValue();

        @Setting(key = "float", undefined = "-123456.25")
        Float FloatValue();

        @Setting(key = "float", undefined = "1234567.25")
        double doubleValue();

        @Setting(key = "float", undefined = "-1234567.25")
        Double DoubleValue();

        @Setting(key = "class", undefined = "java.lang.Object")
        Class classValue();

        @Setting(key = "enum", undefined = "SAMPLE")
        EnumType enumValue();
    }

    public static enum EnumType {
        SAMPLE
    }

    @Test
    public void typeCasts() throws Exception {
        TestPropertyProvider.delegate = new EmptyPropertyProvider();

        final MultTypeConfigured component = container.getComponent(MultTypeConfigured.class);
        assert component != null : MultTypeConfigured.class;
    }

    static void assertValue(final String value, final String expected) {
        if (expected == null) {
            assert value == null : String.format("Expected null, got >%s<", value);
        } else {
            assert expected.equals(value) : String.format("Expected %s, got >%s<", expected, value);
        }
    }

    // just for convenience
    private static abstract class Configured {

        protected void checkSettings(final Settings configuration, final String missing1, final String missing2, final String valid1, final String valid2) {
            assert configuration != null;

            assertValue(configuration.missingValue1(), missing1);
            assertValue(configuration.missingValue2(), missing2);

            assertValue(configuration.validValue1(), valid1);
            assertValue(configuration.validValue2(), valid2);
        }

        public abstract void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2);
    }

    @Component
    public static class StaticConfigured extends Configured {

        private final Settings configuration;

        public StaticConfigured(final @Configuration.Definition(Settings.class) Configuration<Settings> settings) {
            configuration = settings.snapshot();
            assert configuration != null;
        }

        public void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2) {
            checkSettings(configuration, missing1, missing2, valid1, valid2);
        }

    }

    @Component
    public static class DynamicConfigured extends Configured {

        private final Configuration<Settings> settings;

        public DynamicConfigured(final @Configuration.Definition(Settings.class) Configuration<Settings> settings) {
            this.settings = settings;
        }
        public void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2) {
            checkSettings(settings.snapshot(), missing1, missing2, valid1, valid2);
        }

    }

    @Component
    @Configuration.Context("context1")
    public static class ContextConfigured extends Configured {

        private final Settings configuration;

        public ContextConfigured(final @Configuration.Definition(Settings.class) @Configuration.Context("context2") Configuration<Settings> settings) {
            configuration = settings.snapshot();
            assert configuration != null;
        }

        public void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2) {
            checkSettings(configuration, missing1, missing2, valid1, valid2);
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
    public static class MultTypeConfigured {

        public MultTypeConfigured(final @Configuration.Definition(MultiTypeSettings.class) Configuration<MultiTypeSettings> settings) {
            final MultiTypeSettings configuration = settings.snapshot();
            assert configuration != null;

            assert configuration.booleanValue();
            assert configuration.BooleanValue();
            assert configuration.byteValue() == 123;
            assert configuration.ByteValue() == -123;
            assert configuration.shortValue() == 1234;
            assert configuration.ShortValue() == -1234;
            assert configuration.intValue() == 12345;
            assert configuration.IntegerValue() == -12345;
            assert configuration.longValue() == 123456;
            assert configuration.LongValue() == -123456;
            assert configuration.floatValue() == 123456.25;
            assert configuration.FloatValue() == -123456.25;
            assert configuration.doubleValue() == 1234567.25;
            assert configuration.DoubleValue() == -1234567.25;
            assert configuration.classValue() == Object.class;
            assert configuration.enumValue() == EnumType.SAMPLE;
        }
    }
}
