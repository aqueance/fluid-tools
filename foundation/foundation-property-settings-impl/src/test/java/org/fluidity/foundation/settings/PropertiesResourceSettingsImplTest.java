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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
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
        EasyMock.expect(resources.locateResources(resourceName)).andReturn(new URL[0]);

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

        final String defaultsFile = "default.properties";

        final URL url = new URL("test", "", 0, resourceName, new TestURLStreamHandler(baos.toByteArray()));

        EasyMock.expect(resources.resourceName(defaultsFile)).andReturn(resourceName);
        EasyMock.expect(resources.locateResources(resourceName)).andReturn(new URL[] { url });

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

        final String defaultsFile = "default.properties";
        final String applicationFile = appName + ".properties";

        final URL defaultUrl = new URL("test", "", 0, defaultsFile, new TestURLStreamHandler(dbaos.toByteArray()));
        final URL applicationUrl =
            new URL("test", "", 0, applicationFile, new TestURLStreamHandler(dbaos.toByteArray()));

        EasyMock.expect(resources.resourceName(defaultsFile)).andReturn(defaultsFile);
        EasyMock.expect(resources.locateResources(defaultsFile)).andReturn(new URL[] { defaultUrl });

        EasyMock.expect(info.applicationShortName()).andReturn(appName);
        EasyMock.expect(resources.resourceName(applicationFile)).andReturn(applicationFile);
        EasyMock.expect(resources.locateResources(applicationFile)).andReturn(new URL[] { applicationUrl });

        settings.overrideProperties(defaultUrl, defaultProperties);
        settings.overrideProperties(applicationUrl, appProperties);

        replay();
        new PropertiesResourceSettingsImpl(settings, resources, info);
        verify();
    }

    @Test
    public void multipleResources() throws Exception {
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

        final String defaultsFile = "default.properties";
        final String applicationFile = appName + ".properties";

        final URL defaultUrl1 = new URL("test", "", 0, defaultsFile, new TestURLStreamHandler(dbaos.toByteArray()));
        final URL defaultUrl2 = new URL("test", "", 0, defaultsFile, new TestURLStreamHandler(dbaos.toByteArray()));
        final URL defaultUrl3 = new URL("test", "", 0, defaultsFile, new TestURLStreamHandler(dbaos.toByteArray()));

        final URL applicationUrl1 =
            new URL("test", "", 0, applicationFile, new TestURLStreamHandler(dbaos.toByteArray()));
        final URL applicationUrl2 =
            new URL("test", "", 0, applicationFile, new TestURLStreamHandler(dbaos.toByteArray()));

        EasyMock.expect(resources.resourceName(defaultsFile)).andReturn(defaultsFile);
        EasyMock.expect(resources.locateResources(defaultsFile))
            .andReturn(new URL[] { defaultUrl1, defaultUrl2, defaultUrl3 });

        EasyMock.expect(info.applicationShortName()).andReturn(appName);
        EasyMock.expect(resources.resourceName(applicationFile)).andReturn(applicationFile);
        EasyMock.expect(resources.locateResources(applicationFile))
            .andReturn(new URL[] { applicationUrl1, applicationUrl2 });

        settings.overrideProperties(defaultUrl1, defaultProperties);
        settings.overrideProperties(defaultUrl2, defaultProperties);
        settings.overrideProperties(defaultUrl3, defaultProperties);
        settings.overrideProperties(applicationUrl1, appProperties);
        settings.overrideProperties(applicationUrl2, appProperties);

        replay();
        new PropertiesResourceSettingsImpl(settings, resources, info);
        verify();
    }

    private static class TestURLStreamHandler extends URLStreamHandler {

        private final byte[] bytes;

        public TestURLStreamHandler(final byte[] bytes) {
            this.bytes = bytes;
        }

        protected URLConnection openConnection(URL u) throws IOException {
            return new URLConnection(u) {

                public void connect() throws IOException {
                    // empty
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(bytes);
                }
            };
        }
    }
}