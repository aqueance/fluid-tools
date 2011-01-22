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

import java.util.ArrayList;
import java.util.List;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.configuration.Configuration;
import org.fluidity.foundation.configuration.DynamicConfiguration;
import org.fluidity.foundation.configuration.Properties;
import org.fluidity.foundation.configuration.Setting;
import org.fluidity.foundation.configuration.StaticConfiguration;
import org.fluidity.foundation.spi.PropertyProvider;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ConfigurationTest extends MockGroupAbstractTest {

    private PropertyProvider propertyProvider = addControl(PropertyProvider.class);

    private final ComponentContainer container = new ContainerBoundary();

    @BeforeClass
    public void setupClass() throws Exception {
        StaticPropertyProvider.delegate = propertyProvider;
        DynamicPropertyProvider.delegate = propertyProvider;
    }

    @BeforeMethod
    public void setupTest() throws Exception {
        DynamicPropertyProvider.instance = null;
    }

    public static interface Settings {

        @Setting(key = "missing.key1")
        String missingValue1();

        @Setting(key = "missing.key2", fallback = "default")
        String missingValue2();

        @Setting(key = "valid.key1")
        String validValue1();

        @Setting(key = "valid.key2", fallback = "default")
        String validValue2();
    }

    @Test
    public void staticConfiguration() throws Exception {

        // must read up all properties defined for Settings interface methods.
        EasyMock.expect(propertyProvider.property("missing.key1")).andReturn(null);
        EasyMock.expect(propertyProvider.property("missing.key2")).andReturn(null);
        EasyMock.expect(propertyProvider.property("valid.key1")).andReturn("value1");
        EasyMock.expect(propertyProvider.property("valid.key2")).andReturn("value2");

        replay();
        final Configured configured = container.getComponent(StaticConfigured.class);
        assert configured != null;
        verify();

        replay();
        configured.checkSettings(null, "default", "value1", "value2");
        verify();
    }

    @Test
    public void dynamicConfiguration() throws Exception {

        // must read up all properties defined for Settings interface methods.
        EasyMock.expect(propertyProvider.property("missing.key1")).andReturn(null);
        EasyMock.expect(propertyProvider.property("missing.key2")).andReturn(null);
        EasyMock.expect(propertyProvider.property("valid.key1")).andReturn("value1");
        EasyMock.expect(propertyProvider.property("valid.key2")).andReturn("value2");

        replay();
        final Configured configured = container.getComponent(DynamicConfigured.class);
        assert configured != null;
        verify();

        // properties must be cached, no reading should take place
        replay();
        configured.checkSettings(null, "default", "value1", "value2");
        verify();

        // must read up all properties defined for Settings interface methods when we simulate property change listener invocation
        EasyMock.expect(propertyProvider.property("missing.key1")).andReturn("value1");
        EasyMock.expect(propertyProvider.property("missing.key2")).andReturn("value2");
        EasyMock.expect(propertyProvider.property("valid.key1")).andReturn("value3");
        EasyMock.expect(propertyProvider.property("valid.key2")).andReturn("value4");

        // invoke the property change listeners
        replay();
        DynamicPropertyProvider.update();
        verify();

        // properties must be cached, no reading should take place
        replay();
        configured.checkSettings("value1", "value2", "value3", "value4");
        verify();
    }

    public static interface MultiTypeSettings {

        @Setting(key = "boolean", fallback = "true")
        boolean booleanValue();

        @Setting(key = "Boolean", fallback = "true")
        Boolean BooleanValue();

        @Setting(key = "int", fallback = "123")
        byte byteValue();

        @Setting(key = "int", fallback = "-123")
        Byte ByteValue();

        @Setting(key = "int", fallback = "1234")
        short shortValue();

        @Setting(key = "int", fallback = "-1234")
        Short ShortValue();

        @Setting(key = "int", fallback = "12345")
        int intValue();

        @Setting(key = "int", fallback = "-12345")
        Integer IntegerValue();

        @Setting(key = "int", fallback = "123456")
        long longValue();

        @Setting(key = "int", fallback = "-123456")
        Long LongValue();

        @Setting(key = "float", fallback = "123456.25")
        float floatValue();

        @Setting(key = "float", fallback = "-123456.25")
        Float FloatValue();

        @Setting(key = "float", fallback = "1234567.25")
        double doubleValue();

        @Setting(key = "float", fallback = "-1234567.25")
        Double DoubleValue();

        @Setting(key = "class", fallback = "java.lang.Object")
        Class classValue();

        @Setting(key = "enum", fallback = "SAMPLE")
        EnumType enumValue();
    }

    public static enum EnumType {
        SAMPLE
    }

    @Test
    public void typeCasts() throws Exception {
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

        // Uses StaticPropertyProvider for actual property lookup
        public StaticConfigured(@Properties(api = Settings.class, provider = StaticPropertyProvider.class) final StaticConfiguration<Settings> settings) {
            configuration = settings.configuration();
            assert configuration != null;
        }

        public void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2) {
            checkSettings(configuration, missing1, missing2, valid1, valid2);
        }

    }

    @Component
    public static class DynamicConfigured extends Configured {

        private final Configuration<Settings> settings;

        public DynamicConfigured(@Properties(api = Settings.class, provider = DynamicPropertyProvider.class) final DynamicConfiguration<Settings> settings) {
            this.settings = settings;
        }
        public void checkSettings(final String missing1, final String missing2, final String valid1, final String valid2) {
            checkSettings(settings.configuration(), missing1, missing2, valid1, valid2);
        }

    }

    @Component(fallback = true)
    public static class StaticPropertyProvider implements PropertyProvider {
        private static PropertyProvider delegate;

        public Object property(final String key) {
            return delegate.property(key);
        }

        public void addChangeListener(final PropertyChangeListener listener) {
            // this is a static property provider
        }
    }

    @Component(fallback = true)
    public static class DynamicPropertyProvider implements PropertyProvider {

        private static DynamicPropertyProvider instance;
        private static PropertyProvider delegate;

        private final List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();

        public DynamicPropertyProvider() {
            assert DynamicPropertyProvider.instance == null;
            DynamicPropertyProvider.instance = this;
        }

        public Object property(final String key) {
            return delegate.property(key);
        }

        public void addChangeListener(final PropertyChangeListener listener) {
            listeners.add(listener);
        }

        public static void update() {
            instance.notifyListeners();
        }

        private void notifyListeners() {
            for (final PropertyChangeListener listener : listeners) {
                listener.propertiesChanged(this);
            }
        }
    }

    @Component(fallback = true)
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

        public MultTypeConfigured(
                @Properties(api = MultiTypeSettings.class, provider = EmptyPropertyProvider.class) final StaticConfiguration<MultiTypeSettings> settings) {
            final MultiTypeSettings configuration = settings.configuration();
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
