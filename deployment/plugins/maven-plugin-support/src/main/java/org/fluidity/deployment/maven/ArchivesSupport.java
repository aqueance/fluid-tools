package org.fluidity.deployment.maven;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.foundation.Streams;
import org.fluidity.foundation.Utility;

import org.apache.maven.plugin.logging.Log;

/**
 * @author Tibor Varga
 */
public class ArchivesSupport extends Utility {

    private ArchivesSupport() { }

    /**
     * Iterates through the {@link JarFile}s returned by the given {@link Feed feed} and expands it into the given {@link JarOutputStream output}.
     *
     * @param output the JAR output stream to expand the JAR inputs into.
     * @param buffer the buffer to use when copying data.
     * @param log    the Maven log to use.
     * @param feed   the provider of the list of JAR inputs to expand.
     *
     * @return the map of JAR manifest entries encountered in the input JAR inputs.
     *
     * @throws IOException when JAR handling fails.
     */
    public static Map<String, Attributes> expand(final JarOutputStream output, final byte[] buffer, final Log log, final Feed feed) throws IOException {
        final Map<String, Attributes> attributesMap = new HashMap<String, Attributes>();

        JarFile input;
        while ((input = feed.next()) != null) {
            try {
                for (final Enumeration<JarEntry> entries = input.entries(); entries.hasMoreElements(); ) {
                    final JarEntry entry = entries.nextElement();
                    final String entryName = entry.getName();

                    if (!attributesMap.containsKey(entryName)) {

                        // copy all entries except the MANIFEST
                        if (!entryName.equals(JarFile.MANIFEST_NAME)) {
                            output.putNextEntry(entry);
                            Streams.copy(input.getInputStream(entry), output, buffer, false);
                            attributesMap.put(entryName, entry.getAttributes());
                        }

                        // TODO: handle service provider files, indexes, etc.
                    } else if (!entry.isDirectory()) {
                        log.warn(String.format("Duplicate entry: %s", entryName));
                    }
                }
            } finally {
                try {
                    input.close();
                } catch (final IOException ignored) {
                    // ignored
                }
            }
        }

        return attributesMap;
    }

    /**
     * Adds the given JAR manifest entries to the given JAR manifest.
     *
     * @param entries  the entries.
     * @param manifest the manifest.
     */
    public static void include(final Map<String, Attributes> entries, final Manifest manifest) {
        final Map<String, Attributes> attributes = manifest.getEntries();

        for (final Map.Entry<String, Attributes> entry : entries.entrySet()) {
            final Attributes value = entry.getValue();

            if (value != null) {
                final String key = entry.getKey();
                final Attributes list = attributes.get(key);

                if (list == null) {
                    attributes.put(key, value);
                } else {
                    for (final Map.Entry<Object, Object> item : value.entrySet()) {
                        list.put(item.getKey(), item.getValue());
                    }
                }
            }
        }
    }

    /**
     * Provides {@link JarFile} input files to {@link ArchivesSupport#expand(JarOutputStream, byte[], Log, ArchivesSupport.Feed)}.
     */
    public interface Feed {

        /**
         * Returns the next {@link JarFile} to {@link ArchivesSupport#expand(JarOutputStream, byte[], Log, ArchivesSupport.Feed)}.
         *
         * @return a {@link JarFile} object or <code>null</code> if there is no more.
         *
         * @throws IOException when constructing the return object fails.
         */
        JarFile next() throws IOException;
    }
}
