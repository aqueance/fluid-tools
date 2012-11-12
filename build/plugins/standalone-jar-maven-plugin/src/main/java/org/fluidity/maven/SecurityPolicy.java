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

package org.fluidity.maven;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Streams;
import org.fluidity.foundation.jarjar.Handler;

/**
 * Handles security.policy files in nested archives.
 *
 * @author Tibor Varga
 */
public final class SecurityPolicy {

    /**
     * The default location for a security policy file in an archive.
     */
    public static final String SECURITY_POLICY_FILE = String.format("%s/%s", Archives.META_INF, "security.policy");

    private static final String JAVA_CLASS_PATH = "java.class.path";

    private final byte[] buffer;
    private final List<String> files[];
    private final boolean cached;

    /**
     * Creates a new security policy file processor.
     *
     * @param archive the archive to add at level 0; may be <code>null</code>.
     * @param cached  the flag to send to various methods of {@link Archives} when accessing archives.
     * @param levels  tells how many lists of files to maintain.
     * @param buffer  the I/O buffer to use when accessing archives.
     */
    @SuppressWarnings("unchecked")
    public SecurityPolicy(final File archive, final int levels, final boolean cached, final byte[] buffer) throws IOException {
        assert levels > 0 : levels;

        this.buffer = buffer;
        this.cached = cached;
        this.files = new List[levels];

        for (int i = 0; i < files.length; i++) {
            files[i] = new ArrayList<String>();
        }

        if (archive != null) {
            add(archive, 0, null);
        }
    }

    /**
     * Adds the security policy from the given <code>archive</code> the list at the given index.
     *
     * @param archive  the archive to load the security policy from.
     * @param level    identifies the file list to add the loaded policy to.
     * @param location the nested location of <code>archive</code> in the one being processed.
     *
     * @throws IOException when accessing <code>archive</code> fails.
     */
    public void add(final File archive, final int level, final String location) throws IOException {
        assert archive != null;

        final URL url = new URL(Archives.FILE, null, -1, archive.getAbsolutePath());
        final String entry = entry(url);

        if (entry != null) {
            final String content = Streams.load(Archives.open(cached, Archives.Nested.formatURL(url, entry)), "UTF-8", buffer, true);

            if (content != null && !content.isEmpty()) {
                final String file = archive.getName();
                final String replacement = relativeReferences(location, file, absoluteReferences(location, file, content));

                files[level].add(replacement);
            }
        }
    }

    /**
     * Tells if any security policy files have been loaded.
     *
     * @return <code>true</code> if there was at least one policy file loaded; <code>false</code> otherwise.
     */
    public boolean found() {
        for (final List<String> file : files) {
            if (!file.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generates a combined policy file from the ones maintained.
     *
     * @return the content to store in a security policy file.
     */
    public String generate() {
        final Lists.Delimited content = Lists.delimited(String.format("%n"));

        for (final List<String> list : files) {
            content.list(list);
        }

        return content.toString();
    }

    private String relativeReferences(final String location, final String archive, final String content) throws IOException {
        final StringBuffer replacement = new StringBuffer(content.length() + 128);
        final Pattern pattern = Pattern.compile(String.format("(\"(.*):(\\$\\{%s\\}|%s)(.*)\")", JAVA_CLASS_PATH, archive));
        final Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            final String protocol = matcher.group(2);
            String suffix = matcher.group(4);

            if (location != null && suffix.startsWith("/")) {
                suffix = Archives.Nested.DELIMITER.concat(suffix.substring(1));
            }

            if (location != null) {
                final String group = matcher.group();

                // remove the surrounding quotes
                final URL specified = Archives.parseURL(group.substring(1, group.length() - 1));

                // the archive that embeds the specified
                final URL base = Archives.Nested.rootURL(specified);

                // replace the base with the class path reference
                final URL root = Archives.parseURL(String.format("%s:\\${%s}", base.getProtocol(), JAVA_CLASS_PATH));

                // move the entire path under the new location
                final URL url = Archives.Nested.formatURL(Handler.formatURL(root, String.format("%s%s", location, archive)), Archives.resourcePath(specified, base));

                // add the surrounding quotes
                matcher.appendReplacement(replacement, String.format("\"%s\"", url.toExternalForm()));
            } else {
                matcher.appendReplacement(replacement, String.format("\"%s:\\${%s}%s\"", protocol, JAVA_CLASS_PATH, suffix));
            }
        }

        matcher.appendTail(replacement);

        return replacement.toString();
    }

    private String absoluteReferences(final String location, final String archive, final String content) {
        final StringBuffer replacement = new StringBuffer(content.length() + 128);
        final Pattern pattern = Pattern.compile(String.format("(\"%s(.*)\")", archive));
        final Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String suffix = matcher.group(2);

            if (location != null && suffix.startsWith("/")) {
                suffix = Archives.Nested.DELIMITER.concat(suffix.substring(1));
            }

            matcher.appendReplacement(replacement, String.format("\"\\${%s}%s\"", JAVA_CLASS_PATH, suffix));
        }

        matcher.appendTail(replacement);

        return replacement.toString();
    }

    private String entry(final URL url) throws IOException {
        return Archives.attributes(cached, url, Archives.SECURITY_POLICY)[0];
    }
}
