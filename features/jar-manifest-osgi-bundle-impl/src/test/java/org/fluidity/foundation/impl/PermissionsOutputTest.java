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

package org.fluidity.foundation.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.fluidity.deployment.plugin.spi.SecurityPolicy;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Lists;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.osgi.framework.PackagePermission;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class PermissionsOutputTest extends Simulator {

    private static final String NL = String.format("%n");

    private static final String TRIGGER = "trigger.jar";
    private static final String NO_TRIGGER = "no_trigger.jar";

    private static final String DEPENDENCY_1 = "META-INF/dependencies/dependency-1.jar";
    private static final String DEPENDENCY_2 = "META-INF/dependencies/dependency-2.jar";

    private static final String JAVA_PERMISSIONS =   "  permission java.util.PropertyPermission \"java.protocol.handler.pkgs\", \"read\";" + NL
                                                   + "  permission java.lang.RuntimePermission \"getClassLoader\";" + NL
                                                   + "  permission java.lang.RuntimePermission \"createClassLoader\", signedBy \"whoever\";" + NL
                                                   + "  permission java.io.FilePermission \"<<ALL FILES>>\", \"read\", signedBy \"whoever\";";
    private static final String SECURITY_POLICY = String.format("grant codebase \"file:${java.class.path}\" {\n%1$s\n};\n"
                                                       + "\n"
                                                       + "grant signedBy \"whatever\", codebase \"jarjar:file:${java.class.path}!:/%2$s\", principal some.class.name \"some name\" {\n%1$s\n};\n"
                                                       + "\n"
                                                       + "grant principal some.class.name \"some name\", codebase \"jarjar:file:${java.class.path}!:/%3$s\", signedBy \"whatever\" {\n%1$s\n};\n"
                                                       + "\n"
                                                       + "grant signedBy \"whatever\" {\n%1$s\n};", JAVA_PERMISSIONS, DEPENDENCY_1, DEPENDENCY_2);

    private static final String OSGI_PERMISSIONS1 = "(java.util.PropertyPermission \"java.protocol.handler.pkgs\" \"read\")" + NL
                                                   + "(java.lang.RuntimePermission \"getClassLoader\")" + NL
                                                   + "(java.lang.RuntimePermission \"createClassLoader\")" + NL
                                                   + "(java.io.FilePermission \"<<ALL FILES>>\" \"read\")";
    private static final String OSGI_PERMISSIONS2 = "# (java.util.PropertyPermission \"java.protocol.handler.pkgs\" \"read\")" + NL
                                                   + "# (java.lang.RuntimePermission \"getClassLoader\")" + NL
                                                   + "# (java.lang.RuntimePermission \"createClassLoader\")" + NL
                                                   + "# (java.io.FilePermission \"<<ALL FILES>>\" \"read\")";
    private static final String LOCAL_PERMISSIONS = String.format("%3$s%5$s%5$s# %1$s%5$s%4$s%5$s%5$s# %2$s%5$s%4$s",
                                                                  DEPENDENCY_1,
                                                                  DEPENDENCY_2,
                                                                  OSGI_PERMISSIONS1,
                                                                  OSGI_PERMISSIONS2,
                                                                  NL);

    private final byte[] buffer = new byte[1024];
    private final MockObjects components = dependencies();

    private final SecurityPolicy.Output delegate = components.normal(SecurityPolicy.Output.class);

    private SecurityPolicy.Output output;

    private void setup(final String archive, final String dynamicImports, final String staticImports, final String staticExports) throws IOException {
        final File file = Archives.localFile(new URL(Archives.containing(PermissionsOutputTest.class), archive));
        output = new OsgiLocalPermissions.PermissionsOutput(file, delegate, buffer, dynamicImports, staticImports, staticExports, new File[0]);
    }

    private void expect(final String expected) throws IOException {
        delegate.save(EasyMock.eq(OsgiLocalPermissions.SECURITY_POLICY_FILE), EasyMock.<String>notNull());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                final String actual = (String) EasyMock.getCurrentArguments()[1];
                assert expected.equals(actual) : String.format("%nExpected: %s%nActual  : %s\n",
                                                               expected.replaceAll("[\\r]", "r").replaceAll("[\\n]", "n").replaceAll("[\\s]", "_"),
                                                               actual.replaceAll("[\\r]", "r").replaceAll("[\\n]", "n").replaceAll("[\\s]", "_"));
                return null;
            }
        });
    }

    @Test
    public void testNoTrigger() throws Exception {
        setup(NO_TRIGGER, null, null, null);

        verify(new Task() {
            public void run() throws Exception {
                output.save(null, SECURITY_POLICY);
            }
        });
    }

    @Test
    public void testGrantEntriesOnly() throws Exception {
        setup(TRIGGER, null, null, null);

        expect(LOCAL_PERMISSIONS);

        verify(new Task() {
            public void run() throws Exception {
                output.save(null, SECURITY_POLICY);
            }
        });
    }

    @Test
    public void testGarbageSecurity() throws Exception {
        setup(TRIGGER, null, null, null);

        verify(new Task() {
            public void run() throws Exception {
                output.save(null, "grant some { garbage }");
            }
        });
    }

    @Test
    public void testSimplePackages() throws Exception {
        final String dynamicImport = "simple.dynamic.import";
        final String staticImport = "simple.static.import";
        final String staticExport = "simple.static.export";

        setup(TRIGGER, dynamicImport, staticImport, staticExport);

        expect(String.format("# dynamic package imports%7$s"
                             + "(%1$s \"%4$s\" \"%2$s\")%7$s%7$s"
                             + "# static package imports%7$s"
                             + "(%1$s \"%5$s\" \"%2$s\")%7$s%7$s"
                             + "# static package exports%7$s"
                             + "(%1$s \"%6$s\" \"%3$s\")",
                             PackagePermission.class.getName(),
                             PackagePermission.IMPORT,
                             PackagePermission.EXPORTONLY,
                             dynamicImport,
                             staticImport,
                             staticExport,
                             NL));

        verify(new Task() {
            public void run() throws Exception {
                output.save(null, null);
            }
        });
    }

    @Test
    public void testPatternedPackages() throws Exception {
        final String version = ";version=whatever";
        final String something = ";something=other";

        final String dynamicImport = "simple.dynamic.import.*";
        final String staticImport = "simple.static.import";
        final String staticExport = "simple.static.export";

        setup(TRIGGER, dynamicImport, staticImport + version, staticExport + version + something);

        expect(String.format("# dynamic package imports%7$s"
                             + "(%1$s \"%4$s\" \"%2$s\")%7$s%7$s"
                             + "# static package imports%7$s"
                             + "(%1$s \"%5$s\" \"%2$s\")%7$s%7$s"
                             + "# static package exports%7$s"
                             + "(%1$s \"%6$s\" \"%3$s\")",
                             PackagePermission.class.getName(),
                             PackagePermission.IMPORT,
                             PackagePermission.EXPORTONLY,
                             dynamicImport,
                             staticImport,
                             staticExport,
                             NL));

        verify(new Task() {
            public void run() throws Exception {
                output.save(null, null);
            }
        });
    }

    @Test
    public void testMultiplePackages() throws Exception {
        final String version = ";version=something";
        final String something = ";something=other";

        final String dynamicImport1 = "simple.dynamic.import1.*";
        final String dynamicImport2 = "simple.dynamic.import2.*";
        final String staticImport1 = "simple.static.import1";
        final String staticImport2 = "simple.static.import2";
        final String staticImport3 = "simple.static.import3";
        final String staticExport1 = "simple.static.export1";
        final String staticExport2 = "simple.static.export2";

        final String dynamicImport = Lists.delimited(",", dynamicImport1 + version, dynamicImport2);
        final String staticImport = Lists.delimited(",", staticImport1, staticImport2 + version, staticImport3);
        final String staticExport = Lists.delimited(",", staticExport1 + version + something, staticExport2 + version);

        setup(TRIGGER, dynamicImport, staticImport, staticExport);

        expect(String.format("# dynamic package imports%11$s"
                             + "(%1$s \"%4$s\" \"%2$s\")%11$s"
                             + "(%1$s \"%5$s\" \"%2$s\")%11$s%11$s"
                             + "# static package imports%11$s"
                             + "(%1$s \"%6$s\" \"%2$s\")%11$s"
                             + "(%1$s \"%7$s\" \"%2$s\")%11$s"
                             + "(%1$s \"%8$s\" \"%2$s\")%11$s%11$s"
                             + "# static package exports%11$s"
                             + "(%1$s \"%9$s\" \"%3$s\")%11$s"
                             + "(%1$s \"%10$s\" \"%3$s\")",
                             PackagePermission.class.getName(),
                             PackagePermission.IMPORT,
                             PackagePermission.EXPORTONLY,
                             dynamicImport1,
                             dynamicImport2,
                             staticImport1,
                             staticImport2,
                             staticImport3,
                             staticExport1,
                             staticExport2,
                             NL));

        verify(new Task() {
            public void run() throws Exception {
                output.save(null, null);
            }
        });
    }
}
