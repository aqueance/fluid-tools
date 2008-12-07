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
 * <code>org.fluidity.maven:maven-composition-plugin</code> plugin's <code>application-goal</code> to generate that MANIFEST file.
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