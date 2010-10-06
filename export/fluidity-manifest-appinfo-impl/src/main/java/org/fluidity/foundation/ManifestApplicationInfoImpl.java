/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.fluidity.composition.Component;

/**
 * Reads application key and name from two attributes in the host application's JAR manifest main section: Application-Key and Application-Name.
 *
 * <p/>
 *
 * This implementation is useful only for packaged applications such as <code>war</code> and <code>ear</code> files. Use the
 * <code>org.fluidity.maven:maven-composition-plugin</code> plugin's <code>application-info</code> goal to generate that MANIFEST file.
 *
 * <p/>
 *
 * For <code>jar</code> files configure Maven to provide these attributes by adding the following to your <code>pom.xml</code>:
 * <xmp>
 * ...
 * <plugin>
 *   <groupId>org.apache.maven.plugins</groupId>
 *   <artifactId>maven-jar-plugin</artifactId>
 *   <configuration>
 *     <archive>
 *       <manifestEntries>
 *         <Application-Key>${project.artifactId}</Application-Key>
 *         <Application-Name>${project.name} (${project.version})</Application-Name>
 *       </manifestEntries>
 *     </archive>
 *   </configuration>
 * </plugin>
 * ...
 * </xmp>
 */
@Component
final class ManifestApplicationInfoImpl implements ApplicationInfo {

    private final String key;
    private final String name;

    public ManifestApplicationInfoImpl(final Resources resources) throws IOException {
        final URL[] urls = resources.locateResources(JarFile.MANIFEST_NAME);

        String key = null;
        String name = null;

        for (final URL url : urls) {
            final Manifest manifest = new Manifest(url.openStream());
            final Attributes attributes = manifest.getMainAttributes();

            key = attributes.getValue(KEY_ATTRIBUTE);
            name = attributes.getValue(NAME_ATTRIBUTE);

            if (key != null & name != null) {
                break;
            }
        }

        this.key = key;
        this.name = name;

        if (this.key == null && this.name == null) {
            throw new RuntimeException("Either " + KEY_ATTRIBUTE + " or " + NAME_ATTRIBUTE + " or both are missing from JAR manifests.");
        }
    }

    public String key() {
        return key;
    }

    public String name() {
        return name;
    }
}