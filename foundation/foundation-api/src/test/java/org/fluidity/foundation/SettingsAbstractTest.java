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
package org.fluidity.foundation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 * @version $Revision$
 */
public abstract class SettingsAbstractTest extends MockGroupAbstractTest {

    protected abstract Settings newInstance(final Settings reference, final ApplicationInfo info) throws Exception;

    protected abstract void cleanup();

    @Test
    public void basicSettings() throws Exception {
        final Properties props = new Properties();

        props.setProperty("naked.string", "this is a string");
        props.setProperty("naked.int", "1234");
        props.setProperty("naked.bool", "true");
        props.setProperty("spaced/string", "another string");
        props.setProperty("spaced/int", "5678");
        props.setProperty("spaced2/bool", "false");

        final Settings settings = newInstance(new MockSettings(props), null);

        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys())), new HashSet<String>(Arrays.asList("naked.string", "naked.int", "naked.bool")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.namespaces())), new HashSet<String>(Arrays.asList("spaced", "spaced2")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys("spaced"))), new HashSet<String>(Arrays.asList("string", "int")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys("spaced2"))), new HashSet<String>(Arrays.asList("bool")));

        Assert.assertEquals(settings.setting("naked.string", null), props.getProperty("naked.string"));
        Assert.assertEquals(settings.setting("missing.string", "missing"), "missing");
        Assert.assertEquals(settings.setting("naked.int", 0), Integer.valueOf(props.getProperty("naked.int")).intValue());
        Assert.assertSame(settings.setting("missing.int", 12), 12);
        Assert.assertEquals(settings.setting("naked.bool", false), Boolean.valueOf(props.getProperty("naked.bool")).booleanValue());
        Assert.assertFalse(settings.setting("missing.bool", false));

        Assert.assertEquals(settings.setting("spaced", "string", null), props.getProperty("spaced/string"));
        Assert.assertEquals(settings.setting("spaced", "missing", "not again"), "not again");
        Assert.assertEquals(settings.setting("spaced", "int", 0), Integer.valueOf(props.getProperty("spaced/int")).intValue());
        Assert.assertSame(settings.setting("spaced", "missing", 34), 34);
        Assert.assertSame(settings.setting("spaced2", "bool", false), Boolean.valueOf(props.getProperty("spaced2/bool")));
        Assert.assertTrue(settings.setting("spaced2", "missing", true));
    }

    @Test
    public void specificSettingsOverrideGenericOnes() throws Exception {
        final Properties props = new Properties();

        props.setProperty("naked.string", "wrong value");
        props.setProperty("naked.int", "wrong value");
        props.setProperty("naked.bool", "wrong value");
        props.setProperty("spaced/string", "wrong value");
        props.setProperty("spaced/int", "wrong value");
        props.setProperty("spaced2/bool", "wrong value");
        props.setProperty("spaced3/bool", "some value");

        props.setProperty("app/naked.string", "this is a string");
        props.setProperty("app/naked.int", "1234");
        props.setProperty("app/naked.bool", "true");
        props.setProperty("app/spaced/string", "another string");
        props.setProperty("app/spaced/int", "5678");
        props.setProperty("app/spaced2/bool", "false");

        final Settings settings = newInstance(new MockSettings(props), new MockApplicationInfo("app", null));

        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys())), new HashSet<String>(Arrays.asList("naked.string", "naked.int", "naked.bool")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.namespaces())), new HashSet<String>(Arrays.asList("spaced", "spaced2", "spaced3")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys("spaced"))), new HashSet<String>(Arrays.asList("string", "int")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys("spaced2"))), new HashSet<String>(Arrays.asList("bool")));

        Assert.assertEquals(settings.setting("naked.string", null), props.getProperty("app/naked.string"));
        Assert.assertEquals(settings.setting("missing.string", "missing"), "missing");
        Assert.assertEquals(settings.setting("naked.int", 0), Integer.valueOf(props.getProperty("app/naked.int")).intValue());
        Assert.assertSame(settings.setting("missing.int", 12), 12);
        Assert.assertEquals(settings.setting("naked.bool", false), Boolean.valueOf(props.getProperty("app/naked.bool")).booleanValue());
        Assert.assertFalse(settings.setting("missing.bool", false));

        Assert.assertEquals(settings.setting("spaced", "string", null), props.getProperty("app/spaced/string"));
        Assert.assertEquals(settings.setting("spaced", "missing", "not again"), "not again");
        Assert.assertEquals(settings.setting("spaced", "int", 0), Integer.valueOf(props.getProperty("app/spaced/int")).intValue());
        Assert.assertSame(settings.setting("spaced", "missing", 34), 34);
        Assert.assertSame(settings.setting("spaced2", "bool", false), Boolean.valueOf(props.getProperty("app/spaced2/bool")));
        Assert.assertTrue(settings.setting("spaced2", "missing", true));
        Assert.assertSame(settings.setting("spaced3", "bool", false), Boolean.valueOf(props.getProperty("app/spaced3/bool")));
    }

    @Test
    public void defaultsToGenericSettings() throws Exception {
        final Properties props = new Properties();

        props.setProperty("app/naked.string", "this is a string");
        props.setProperty("app/naked.int", "1234");
        props.setProperty("app/naked.bool", "true");
        props.setProperty("app/spaced/string", "another string");
        props.setProperty("app/spaced/int", "5678");
        props.setProperty("app/spaced2/bool", "false");

        final Settings settings = newInstance(new MockSettings(props), new MockApplicationInfo("app", null));

        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys())), new HashSet<String>(Arrays.asList("naked.string", "naked.int", "naked.bool")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.namespaces())), new HashSet<String>(Arrays.asList("spaced", "spaced2")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys("spaced"))), new HashSet<String>(Arrays.asList("string", "int")));
        Assert.assertEquals(new HashSet<String>(Arrays.asList(settings.keys("spaced2"))), new HashSet<String>(Arrays.asList("bool")));

        Assert.assertEquals(settings.setting("naked.string", null), props.getProperty("app/naked.string"));
        Assert.assertEquals(settings.setting("missing.string", "missing"), "missing");
        Assert.assertEquals(settings.setting("naked.int", 0), Integer.valueOf(props.getProperty("app/naked.int")).intValue());
        Assert.assertSame(settings.setting("missing.int", 12), 12);
        Assert.assertEquals(settings.setting("naked.bool", false), Boolean.valueOf(props.getProperty("app/naked.bool")).booleanValue());
        Assert.assertFalse(settings.setting("missing.bool", false));

        Assert.assertEquals(settings.setting("spaced", "string", null), props.getProperty("app/spaced/string"));
        Assert.assertEquals(settings.setting("spaced", "missing", "not again"), "not again");
        Assert.assertEquals(settings.setting("spaced", "int", 0), Integer.valueOf(props.getProperty("app/spaced/int")).intValue());
        Assert.assertSame(settings.setting("spaced", "missing", 34), 34);
        Assert.assertSame(settings.setting("spaced2", "bool", false), Boolean.valueOf(props.getProperty("app/spaced2/bool")));
        Assert.assertTrue(settings.setting("spaced2", "missing", true));
    }
}