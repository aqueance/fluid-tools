/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.foundation.settings;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import org.fluidity.foundation.ApplicationInfo;
import org.fluidity.foundation.MockApplicationInfo;
import org.fluidity.foundation.Settings;
import org.fluidity.foundation.SettingsAbstractTest;
import org.fluidity.foundation.SystemSettings;
import org.fluidity.foundation.logging.BootstrapLog;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class PropertySettingsImplTest extends SettingsAbstractTest {

    static {
        SystemSettings.set(BootstrapLog.SUPPRESS_LOGS, BootstrapLog.ALL_LOGS);
    }

    private final URL defaultUrl;
    private final URL appUrl;

    public PropertySettingsImplTest() throws MalformedURLException {
        defaultUrl = new URL("file", "", 0, "/default.properties");
        appUrl = new URL("file", "", 0, "/app.properties");
    }

    protected void setupMockControls() throws Exception {
        // empty
    }

    protected Settings newInstance(final Settings reference, final ApplicationInfo info) throws Exception {
        final Properties props = new Properties();
        String[] keys = reference.keys();

        for (final String key : keys) {
            props.setProperty(key, reference.setting(key, null));
        }

        for (final String namespace : reference.namespaces()) {
            keys = reference.keys(namespace);

            for (final String key : keys) {
                props.setProperty(namespace + "/" + key, reference.setting(namespace, key, null));
            }
        }

        final PropertySettingsImpl settings = new PropertySettingsImpl(info);
        settings.overrideProperties(defaultUrl, props);
        return settings;
    }

    protected void cleanup() {
        // empty
    }

    @Test
    public void laterSettingsOverrideEarlierSettings() throws Exception {
        final Properties defaultProps = new Properties();

        defaultProps.setProperty("naked.string", "wrong generic value");
        defaultProps.setProperty("naked.int", "wrong generic value");
        defaultProps.setProperty("naked.bool", "wrong generic value");
        defaultProps.setProperty("spaced/string", "wrong generic value");
        defaultProps.setProperty("spaced/int", "wrong generic value");
        defaultProps.setProperty("spaced2/bool", "wrong generic value");

        defaultProps.setProperty("app/naked.string", "wrong specific value");
        defaultProps.setProperty("app/naked.int", "wrong specific value");
        defaultProps.setProperty("app/naked.bool", "wrong specific value");
        defaultProps.setProperty("app/spaced/string", "wrong specific value");
        defaultProps.setProperty("app/spaced/int", "wrong specific value");
        defaultProps.setProperty("app/spaced2/bool", "wrong specific value");

        final PropertySettingsImpl settings = new PropertySettingsImpl(new MockApplicationInfo("app", null));

        settings.overrideProperties(defaultUrl, defaultProps);

        final Properties applicationProps = new Properties();

        applicationProps.setProperty("naked.string", "this is a string");
        applicationProps.setProperty("naked.int", "1234");
        applicationProps.setProperty("naked.bool", "true");
        applicationProps.setProperty("spaced/string", "another string");
        applicationProps.setProperty("spaced/int", "5678");
        applicationProps.setProperty("spaced2/bool", "false");
        applicationProps.setProperty("spaced3/bool", "some value");

        settings.overrideProperties(appUrl, applicationProps);

        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys())), new HashSet<String>(Arrays.asList("naked.string", "naked.int", "naked.bool")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.namespaces())), new HashSet<String>(Arrays.asList("spaced", "spaced2", "spaced3")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys("spaced"))), new HashSet<String>(Arrays.asList("string", "int")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys("spaced2"))), new HashSet<String>(Arrays.asList("bool")));

        Assert.assertEquals(settings.setting("naked.string", null), applicationProps.getProperty("naked.string"));
        Assert.assertEquals(settings.setting("missing.string", "missing"), "missing");
        Assert.assertEquals(settings.setting("naked.int", 0), Integer.valueOf(applicationProps.getProperty("naked.int")).intValue());
        Assert.assertSame(settings.setting("missing.int", 12), 12);
        Assert.assertEquals(settings.setting("naked.bool", false), Boolean.valueOf(applicationProps.getProperty("naked.bool")).booleanValue());
        Assert.assertFalse(settings.setting("missing.bool", false));

        Assert.assertEquals(settings.setting("spaced", "string", null), applicationProps.getProperty("spaced/string"));
        Assert.assertEquals(settings.setting("spaced", "missing", "not again"), "not again");
        Assert.assertEquals(settings.setting("spaced", "int", 0), Integer.valueOf(applicationProps.getProperty("spaced/int")).intValue());
        Assert.assertSame(settings.setting("spaced", "missing", 34), 34);
        Assert.assertSame(settings.setting("spaced2", "bool", false), Boolean.valueOf(applicationProps.getProperty("spaced2/bool")));
        Assert.assertTrue(settings.setting("spaced2", "missing", true));
        Assert.assertSame(settings.setting("spaced3", "bool", false), Boolean.valueOf(applicationProps.getProperty("spaced3/bool")));
    }
}