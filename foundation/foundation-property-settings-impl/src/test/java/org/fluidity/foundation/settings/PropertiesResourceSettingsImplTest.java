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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Properties;

import org.easymock.EasyMock;
import org.fluidity.foundation.ApplicationInfo;
import org.fluidity.foundation.Resources;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 * @version $Revision$
 */
public class PropertiesResourceSettingsImplTest extends MockGroupAbstractTest {

    private final PropertySettings settings = addControl(PropertySettings.class);

    private final Resources resources = addControl(Resources.class);

    private final ApplicationInfo info = addControl(ApplicationInfo.class);

    @Test
    public void noResourceFound() throws Exception {
        final String resourceName = "whatever";
        EasyMock.expect(resources.resourceName("default.properties")).andReturn(resourceName);
        EasyMock.expect(resources.loadResource(resourceName)).andReturn(null);

        replay();
        new PropertiesResourceSettingsImpl(settings, resources);
        verify();
    }

    @Test
    public void withoutApplicationInfo() throws Exception {
        final Properties properties = new Properties();

        properties.setProperty("a", "b");
        properties.setProperty("a.b.c", "def");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store(baos, null);

        final String resourceName = "whatever";
        final URL url = new URL("file://properties.properties");

        EasyMock.expect(resources.resourceName("default.properties")).andReturn(resourceName);
        EasyMock.expect(resources.loadResource(resourceName)).andReturn(new ByteArrayInputStream(baos.toByteArray()));
        EasyMock.expect(resources.locateResource(resourceName)).andReturn(url);
        settings.overrideProperties(url, properties);

        replay();
        new PropertiesResourceSettingsImpl(settings, resources);
        verify();
    }

    @Test
    public void withApplicationInfo() throws Exception {
        final Properties defaultProperties = new Properties();

        defaultProperties.setProperty("a", "b");
        defaultProperties.setProperty("a.b.c", "def");

        final ByteArrayOutputStream dbaos = new ByteArrayOutputStream();
        defaultProperties.store(dbaos, null);

        final Properties appProperties = new Properties();

        appProperties.setProperty("a", "b");
        appProperties.setProperty("a.b.c", "def");

        final ByteArrayOutputStream abaos = new ByteArrayOutputStream();
        appProperties.store(abaos, null);

        final String appName = "whatever";

        final URL defaultUrl = new URL("file://default.properties");
        final URL appUrl = new URL("file://app.properties");

        EasyMock.expect(resources.resourceName("default.properties")).andReturn("default.properties");
        EasyMock.expect(resources.loadResource("default.properties"))
            .andReturn(new ByteArrayInputStream(dbaos.toByteArray()));
        EasyMock.expect(resources.locateResource("default.properties")).andReturn(defaultUrl);

        EasyMock.expect(info.applicationShortName()).andReturn(appName);
        EasyMock.expect(resources.resourceName(appName + ".properties")).andReturn(appName + ".properties");
        EasyMock.expect(resources.loadResource(appName + ".properties"))
            .andReturn(new ByteArrayInputStream(abaos.toByteArray()));
        EasyMock.expect(resources.locateResource(appName + ".properties")).andReturn(appUrl);

        settings.overrideProperties(defaultUrl, defaultProperties);
        settings.overrideProperties(appUrl, appProperties);

        replay();
        new PropertiesResourceSettingsImpl(settings, resources, info);
        verify();
    }
}