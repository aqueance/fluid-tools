/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.composition.ServiceProvider;
import org.fluidity.deployment.maven.ClassReaders;
import org.fluidity.deployment.maven.ClassRepository;
import org.fluidity.deployment.osgi.BundleComponents;
import org.fluidity.deployment.osgi.Service;
import org.fluidity.deployment.plugin.spi.SecurityPolicy;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.IOStreams;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Methods;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.foundation.Strings;

import org.apache.maven.artifact.Artifact;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServicePermission;

/**
 * Generates an OSGi local permissions file based on the Java permissions needed by the bundle, and by its manually edited permissions file, if found.
 *
 * @author Tibor Varga
 */
final class OsgiLocalPermissions implements SecurityPolicy {

    static final String SECURITY_POLICY_FILE = String.format("%s/permissions.perm", Archives.OSGI_INF);

    private final List<Dependency> dependencies = new ArrayList<>();
    private final SecurityPolicy delegate;
    private final File[] classpath;
    private final String dynamicImports;
    private final String staticImports;
    private final String staticExports;

    OsgiLocalPermissions(final SecurityPolicy delegate,
                         final Collection<Artifact> classpath,
                         final String dynamicImports,
                         final String staticImports,
                         final String staticExports) {
        this.delegate = delegate;
        this.dynamicImports = dynamicImports;
        this.staticImports = staticImports;
        this.staticExports = staticExports;
        this.classpath = new File[classpath.size()];

        int i = 0;
        for (final Artifact artifact : classpath) {
            this.classpath[i++] = artifact.getFile();
        }
    }

    public String name(final File file) {
        return SECURITY_POLICY_FILE;
    }

    public File archive() {
        return delegate.archive();
    }

    public byte[] buffer() {
        return delegate.buffer();
    }

    public void add(final File archive, final int level, final String location) throws IOException {
        delegate.add(archive, level, location);
        dependencies.add(new Dependency() {
            public File file() {
                return archive;
            }

            public String location() {
                return location;
            }
        });
    }

    public void update(final Output metadata) throws IOException {
        delegate.update((name, content) -> metadata.save(name, null));
    }

    public void save(final Output output) throws IOException {
        delegate.save(new PermissionsOutput(delegate.archive(),
                                            output,
                                            buffer(),
                                            dynamicImports,
                                            staticImports,
                                            staticExports,
                                            classpath,
                                            Lists.asArray(Dependency.class, dependencies)));
    }

    /**
     * @author Tibor Varga
     */
    interface Dependency {

        File file();

        String location();
    }

    /**
     * @author Tibor Varga
     */
    static final class PermissionsOutput implements Output {

        private static final String NL = "\n";

        private final String serviceType;

        private static final String REGISTRATION_TYPE_PARAM = Methods.get(Service.Type.class, Service.Type::value)[0].getName();

        private final File archive;
        private final Output output;
        private final byte[] buffer;
        private final String dynamicImports;
        private final String staticImports;
        private final String staticExports;
        private final File[] runtime;
        private final Dependency[] dependencies;

        PermissionsOutput(final File archive,
                          final Output output,
                          final byte[] buffer,
                          final String dynamicImports,
                          final String staticImports,
                          final String staticExports,
                          final File[] runtime,
                          final Dependency... dependencies) {
            this.archive = archive;
            this.output = output;
            this.buffer = buffer;
            this.dynamicImports = dynamicImports;
            this.staticImports = staticImports;
            this.staticExports = staticExports;
            this.runtime = runtime;
            this.dependencies = dependencies;

            final Method method = Methods.get(ServiceProvider.class, ServiceProvider::type)[0];

            final String type = Exceptions.wrap(() -> (String) method.invoke(BundleComponents.Managed.class.getAnnotation(ServiceProvider.class)));

            serviceType = type == null ? (String) method.getDefaultValue() : type;
        }

        private enum Keyword {
            GRANT, CODEBASE, PERMISSION, SIGNEDBY, PRINCIPAL
        }

        public void save(final String name, final String original) throws IOException {
            final String supplied = permissions(archive);

            if (supplied != null) {
                final Map<String, Collection<String>> permissions = new LinkedHashMap<>();

                if (!supplied.isEmpty()) {
                    permissions("source entries", permissions).addAll(Arrays.asList(supplied.split("\"\\r?\\n|\\r")));
                }

                components(permissions, dependencies);

                packages("dynamic package imports", dynamicImports, permissions, PackagePermission.IMPORT);
                packages("static package imports", staticImports, permissions, PackagePermission.IMPORT);
                packages("static package exports", staticExports, permissions, PackagePermission.EXPORTONLY);

                if (original != null) {
                    policy(permissions, original);
                }

                if (!permissions.isEmpty()) {
                    final Set<String> duplicates = new HashSet<>();
                    final StringJoiner delimited = new StringJoiner(NL);

                    for (final Map.Entry<String, Collection<String>> entry : permissions.entrySet()) {
                        final Collection<String> list = entry.getValue();

                        if (!list.isEmpty()) {
                            final String archive = entry.getKey();

                            if (delimited.length() != 0) {
                                delimited.add("");
                            }

                            if (!archive.isEmpty()) {
                                delimited.add("# ".concat(archive));
                            }

                            for (final String permission : list) {
                                delimited.add(duplicates.add(permission) ? permission : "# ".concat(permission));
                            }
                        }
                    }

                    output.save(SECURITY_POLICY_FILE, delimited.toString().replaceAll(NL.concat("{3,}"), NL.concat(NL)));
                }
            }
        }

        private void packages(final String key, final String header, final Map<String, Collection<String>> permissions, final String type) {
            if (header != null) {
                final Collection<String> list = permissions(key, permissions);

                for (final String description : header.split("\\s*,\\s*")) {
                    list.add(String.format("(%s \"%s\" \"%s\")", PackagePermission.class.getName(), description.split("\\s*;\\s*")[0], type));
                }
            }
        }

        @SuppressWarnings("unchecked")
        private static final Collection<Class<?>> IGNORED_TYPES = Arrays.asList((Class<?>) BundleComponents.Stoppable.class,
                                                                                (Class<?>) BundleComponents.Managed.class,
                                                                                (Class<?>) BundleComponents.Registration.class,
                                                                                (Class<?>) BundleComponents.Registration.Listener.class);

        private static final Collection<String> IGNORED_TYPES_INTERNAL = internalNames(IGNORED_TYPES);
        private static final Collection<String> IGNORED_TYPES_EXTERNAL = externalNames(IGNORED_TYPES);

        private static Collection<String> internalNames(final Collection<Class<?>> types) {
            final List<String> list = new ArrayList<>();

            for (final Class<?> type : types) {
                list.add(Type.getInternalName(type));
            }

            return list;
        }

        private static Collection<String> externalNames(final Collection<Class<?>> types) {
            final List<String> list = new ArrayList<>();

            for (final Class<?> type : types) {
                list.add(type.getName());
            }

            return list;
        }

        @SuppressWarnings("ThrowFromFinallyBlock")
        private void components(final Map<String, Collection<String>> permissions, final Dependency... dependencies) throws IOException {
            final Collection<URL> classpath = new LinkedHashSet<>();

            for (final Dependency dependency : dependencies) {
                classpath.add(dependency.file().toURI().toURL());
            }

            if (!classpath.isEmpty()) {
                for (final File file : runtime) {
                    classpath.add(file.toURI().toURL());
                }

                permissions("service acquisition", permissions).add(String.format("(%s \"*\" \"%s\")", ServicePermission.class.getName(), ServicePermission.GET));

                final ClassLoader loader = ClassLoaders.create(classpath, null, null);
                final ClassRepository repository = new ClassRepository(loader);
                final Type registration = Type.getType(Service.Type.class);

                for (final Dependency dependency : dependencies) {
                    final URL file = dependency.file().toURI().toURL();

                    try (final InputStream provider = provider(file)) {
                        if (provider != null) {
                            final BufferedReader metadata = new BufferedReader(new InputStreamReader(provider, Strings.UTF_8));
                            String content;

                            while ((content = metadata.readLine()) != null) {
                                final int hash = content.indexOf('#');
                                final String line = (hash < 0 ? content : content.substring(0, hash)).trim();

                                if (!line.isEmpty()) {
                                    final ClassReader reader = repository.reader(line);
                                    final Set<String> interfaces = ClassReaders.findInterfaces(reader, repository);

                                    if (interfaces.contains(BundleComponents.Registration.class.getName())) {
                                        interfaces.removeAll(IGNORED_TYPES_EXTERNAL);

                                        final List<String> types = new ArrayList<>();

                                        final RegistrationTypes visitor = new RegistrationTypes(registration, types);
                                        final int configuration = ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE;

                                        reader.accept(visitor, configuration);

                                        if (types.isEmpty()) {
                                            services(reader, types, repository);
                                        }

                                        if (types.isEmpty()) {
                                            throw new IllegalStateException(String.format("Managed component %s does not have or inherit @%s",
                                                                                          line,
                                                                                          Strings.formatClass(false, false, Service.Type.class)));
                                        }

                                        final Collection<String> list = permissions(dependency.location().concat(dependency.file().getName()), permissions);

                                        for (final String type : types) {
                                            final String permission = String.format("(%s \"%s\" \"%s\")", ServicePermission.class.getName(), type, ServicePermission.REGISTER);

                                            if (!list.contains(permission)) {
                                                list.add(permission);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private Collection<String> permissions(final String key, final Map<String, Collection<String>> permissions) {
            final Collection<String> list;

            if (!permissions.containsKey(key)) {
                permissions.put(key, list = new ArrayList<>());
            } else {
                list = permissions.get(key);
            }

            return list;
        }

        private void services(final ClassReader reader, final List<String> types, final ClassRepository repository) throws IOException {
            final Collection<String> interfaces = new ArrayList<>(Arrays.asList(reader.getInterfaces()));
            interfaces.removeAll(IGNORED_TYPES_INTERNAL);

            if (interfaces.isEmpty()) {
                if (!Objects.equals(Type.getInternalName(Object.class), reader.getSuperName())) {
                    services(repository.reader(reader.getSuperName()), types, repository);
                } else {
                    types.add(ClassReaders.externalName(reader.getClassName()));
                }
            } else {
                for (String api : interfaces) {
                    types.add(ClassReaders.externalName(api));
                }
            }
        }

        private InputStream provider(final URL file) throws IOException {
            final URL url = Archives.Nested.formatURL(file, String.format("%s/%s", ServiceProviders.location(serviceType), BundleComponents.Managed.class.getName()));

            try {
                return Archives.open(url, true);
            } catch (final FileNotFoundException e) {
                return null;
            }
        }

        private void policy(final Map<String, Collection<String>> permissions, final String original) throws IOException {
            final List<String> texts = new ArrayList<>();

            // pattern for a non-reserved word
            final String word = String.format("%d:\\d+", StreamTokenizer.TT_WORD);

            // pattern for quoted text
            final String text = String.format("%d:\\d+", (int) '"');

            // matches a signedBy clause with a comma before or after
            final String signature = String.format(" %1$d %2$s %3$s|%2$s %3$s %1$d ", (int) ',', keyword(Keyword.SIGNEDBY), text);

            // matches a principal list with a comma before or after
            final String principal = String.format(" %1$d %2$s %3$s %4$s|%2$s %3$s %4$s %1$d ", (int) ',', keyword(Keyword.PRINCIPAL), word, text);

            // removes signedBy clauses and principal lists
            final String content = reduce(" ", original, texts).replaceAll(String.format("(%s|%s)", signature, principal), "");

            // matches a permission line within a grant entry
            final Pattern permission = Pattern.compile(String.format("%3$s (%1$s( %2$s( %4$d %2$s)? %5$d)?)",
                                                                     word,
                                                                     text,
                                                                     keyword(Keyword.PERMISSION),
                                                                     (int) ',',
                                                                     (int) ';'));

            // matches a grant entry
            final Pattern grant = Pattern.compile(String.format("%2$s %3$s (%1$s) %4$d(( %7$s)+) %5$d %6$d",
                                                                text,
                                                                keyword(Keyword.GRANT),
                                                                keyword(Keyword.CODEBASE),
                                                                (int) '{',
                                                                (int) '}',
                                                                (int) ';',
                                                                permission));

            final Matcher entry = grant.matcher(content);

            while (entry.find()) {
                final StringJoiner path = new StringJoiner(":");

                final String[] parts = text(entry.group(1), texts).replaceAll(String.format("\\$\\{%s\\}", JAVA_CLASS_PATH), archive.getName()).split(Archives.Nested.DELIMITER);
                for (int i = 1, limit = parts.length; i < limit; i++) {
                    path.add(parts[i]);
                }

                final Collection<String> list = permissions(path.toString(), permissions);
                final Matcher line = permission.matcher(entry.group(2));

                while (line.find()) {
                    list.add('(' + permission(line.group(1), texts) + ')');
                }
            }
        }

        private String permissions(final File archive) throws IOException {

            // check if the bundle has a local permissions file
            try (final InputStream input = Archives.open(Archives.Nested.formatURL(archive.toURI().toURL(), SECURITY_POLICY_FILE), false)) {
                return IOStreams.load(input, Strings.UTF_8, buffer);
            } catch (final FileNotFoundException ignored) {

                // do not generate one if the bundle has none
                return null;
            }
        }

        private String reduce(final String delimiter, final String original, final List<String> values) throws IOException {
            final StreamTokenizer parser = new StreamTokenizer(new BufferedReader(new StringReader(original)));

            parser.resetSyntax();
            parser.wordChars('a', 'z');
            parser.wordChars('A', 'Z');
            parser.wordChars('.', '.');
            parser.wordChars('0', '9');
            parser.wordChars('_', '_');
            parser.wordChars('$', '$');
            parser.wordChars(128 + 32, 255);
            parser.whitespaceChars(0, ' ');
            parser.commentChar('/');
            parser.quoteChar('\'');
            parser.quoteChar('"');
            parser.lowerCaseMode(false);
            parser.ordinaryChar('/');
            parser.slashSlashComments(true);
            parser.slashStarComments(true);

            final StringJoiner sequence = new StringJoiner(delimiter);
            for (int type = parser.nextToken(); type != StreamTokenizer.TT_EOF; type = parser.nextToken()) {
                final String value = parser.sval == null ? null : parser.sval.toLowerCase();

                if (value == null) {
                    sequence.add(String.format("%d", type));
                } else {
                    try {
                        sequence.add(keyword(Keyword.valueOf(value.toUpperCase())));
                        assert type == StreamTokenizer.TT_WORD : value;
                    } catch (final IllegalArgumentException e) {
                        sequence.add(String.format("%d:%d", type, values.size()));
                        values.add(parser.sval);
                    }
                }
            }

            return sequence.toString();
        }

        private String keyword(final Keyword keyword) {
            return String.format(":%d", keyword.ordinal());
        }

        private String text(final String match, final List<String> texts) {
            return texts.get(Integer.parseInt(match.substring(match.indexOf(':') + 1)));
        }

        private String permission(final String match, final List<String> texts) {
            final StringJoiner lines = new StringJoiner(" ");

            for (final String token : match.split(" ")) {
                if (!token.isEmpty()) {
                    final int colon = token.indexOf(':');
                    assert colon != 0 : match;

                    if (colon > 0) {
                        final String text = texts.get(Integer.parseInt(token.substring(colon + 1)));

                        if (lines.length() == 0) {
                            lines.add(text);
                        } else {
                            lines.add('"' + text + '"');
                        }
                    }
                }
            }

            return lines.toString();
        }

        /**
         * @author Tibor Varga
         */
        private static class RegistrationTypes extends ClassVisitor {

            private final Type registration;
            private final List<String> types;

            RegistrationTypes(final Type registration, final List<String> types) {
                super(Opcodes.ASM5);
                this.registration = registration;
                this.types = types;
            }

            @Override
            @SuppressWarnings("EqualsReplaceableByObjectsCall")
            public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
                final Type type = Type.getType(desc);

                if (registration.equals(type)) {
                    return new AnnotationVisitor(Opcodes.ASM5) {
                        @Override
                        public AnnotationVisitor visitArray(final String name) {
                            assert REGISTRATION_TYPE_PARAM.equals(name) : name;
                            return new AnnotationVisitor(api) {
                                @Override
                                public void visit(final String ignore, final Object value) {
                                    assert ignore == null : ignore;
                                    types.add(((Type) value).getClassName());
                                }
                            };
                        }
                    };
                }

                return null;
            }
        }
    }
}
