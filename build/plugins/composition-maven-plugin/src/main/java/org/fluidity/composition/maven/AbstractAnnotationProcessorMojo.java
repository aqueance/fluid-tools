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

package org.fluidity.composition.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.maven.annotation.ComponentProcessor;
import org.fluidity.composition.maven.annotation.ServiceProviderProcessor;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.deployment.maven.ClassReaders;
import org.fluidity.deployment.maven.ClassRepository;
import org.fluidity.deployment.maven.DependenciesSupport;
import org.fluidity.deployment.maven.Logger;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Methods;
import org.fluidity.foundation.ServiceProviders;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Mojos that find in a bunch of class files all implementations of a service provider interface, create a service provider file as per the JAR file
 * specification, find all components, generate a package bindings class for each and add these package bindings as service provider descriptor files.
 * <p>
 * Subclasses should call {@link AbstractAnnotationProcessorMojo#processDirectory(java.io.File, java.io.File...)} with the directory containing the classes to
 * process. The Maven build object can be obtained by calling {@link AbstractAnnotationProcessorMojo#build()}.
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractAnnotationProcessorMojo extends AbstractMojo implements Opcodes {

    private static final String OBJECT_CLASS_NAME = Type.getInternalName(Object.class);
    private static final String EMPTY_BINDINGS_CLASS_NAME = Type.getInternalName(EmptyPackageBindings.class);

    private static final String PACKAGE_BINDINGS = PackageBindings.class.getName();
    private static final String GENERATED_PACKAGE_BINDINGS = PACKAGE_BINDINGS.substring(PACKAGE_BINDINGS.lastIndexOf(".") + 1).concat("$");

    private final Method implementedMethod;
    private final Method invokedMethod;

    @SuppressWarnings("unchecked")
    protected AbstractAnnotationProcessorMojo() {
        implementedMethod = Methods.get(PackageBindings.class, capture -> capture.bindComponents(null))[0];

        final Class<?>[] implementedParameters = implementedMethod.getParameterTypes();
        assert implementedParameters.length == 1 : implementedMethod;
        assert implementedParameters[0] == ComponentContainer.Registry.class : implementedMethod;

        invokedMethod = Methods.get(ComponentContainer.Registry.class, capture -> capture.bindComponent(null))[0];

        final Class<?>[] invokedParameter = invokedMethod.getParameterTypes();
        assert invokedParameter.length == 2 : invokedMethod;
        assert invokedParameter[0] == Class.class : invokedMethod;
        assert invokedParameter[1] == Class[].class : invokedMethod;
    }

    /**
     * Reference to the maven project
     */
    @Parameter(property = "project", required = true)
    private MavenProject project;

    /**
     * Tells the plugin to emit details about its operation. The default value of this parameter is <code>false</code>.
     */
    @Parameter(property = "fluidity.maven.verbose")
    private boolean verbose;

    private String projectName;

    protected final Build build() {
        return project.getBuild();
    }

    @SuppressWarnings({ "RedundantCast", "ResultOfMethodCallIgnored" })
    protected final void processDirectory(final File classesDirectory, final File... directories) throws MojoExecutionException {
        projectName = getProjectNameId();

        if (!classesDirectory.exists()) {
            return;
        }

        final Map<String, Map<String, Collection<String>>> serviceProviderMap = new HashMap<>();
        final Map<String, Collection<String>> componentMap = new HashMap<>();
        final Map<String, Collection<String>> componentGroupMap = new HashMap<>();

        final List<URL> urls = new ArrayList<>();

        try {
            urls.add(classesDirectory.toURI().toURL());

            for (File directory : directories) {
                if (directory.exists()) {
                    urls.add(directory.toURI().toURL());
                }
            }

            for (final Artifact artifact : (Set<Artifact>) project.getArtifacts()) {
                if (Objects.equals(artifact.getType(), DependenciesSupport.JAR_TYPE)) {
                    urls.add(artifact.getFile().toURI().toURL());
                }
            }
        } catch (final MalformedURLException e) {
            assert false : e;
        }

        final Logger log = Logger.initialize(getLog(), verbose);

        try {
            processClasses(log, ClassLoaders.create(urls, null, null), classesDirectory, serviceProviderMap, componentMap, componentGroupMap);
        } catch (final MojoExecutionException e) {
            throw e;
        } catch (final Exception e) {
            throw new MojoExecutionException("Error processing service providers", e);
        }

        for (final Map.Entry<String, Map<String, Collection<String>>> entry : serviceProviderMap.entrySet()) {
            final String type = entry.getKey();
            final Map<String, Collection<String>> providerMap = entry.getValue();

            final String root = ServiceProviders.location(type);
            final File servicesDirectory = new File(classesDirectory, root);

            for (final Map.Entry<String, Collection<String>> providerEntry : providerMap.entrySet()) {
                final Collection<String> list = providerEntry.getValue();

                if (!list.isEmpty()) {
                    servicesDirectory.mkdirs();

                    if (!servicesDirectory.exists()) {
                        throw new MojoExecutionException(String.format("Could not create %s", classesDirectory));
                    }
                }
            }

            for (final Map.Entry<String, Collection<String>> providerEntry : providerMap.entrySet()) {
                if (!providerEntry.getValue().isEmpty()) {
                    log.detail("Service provider descriptor %s/%s contains:", root, providerEntry.getKey());

                    final File serviceProviderFile = new File(servicesDirectory, providerEntry.getKey());
                    serviceProviderFile.delete();

                    try (final PrintWriter writer = new PrintWriter(new FileWriter(serviceProviderFile))) {
                        for (final String className : providerEntry.getValue()) {
                            writer.println(className);
                            log.detail("  %s%s", className, componentMap.containsKey(className) ? " (generated)" : "");
                        }
                    } catch (final IOException e) {
                        throw new MojoExecutionException(String.format("Error opening file %s", serviceProviderFile), e);
                    }
                }
            }
        }

        final Map<String, Collection<String>> bindingsMap = serviceProviderMap.get(PackageBindings.SERVICE_TYPE);

        if (bindingsMap != null) {
            for (final Map.Entry<String, Collection<String>> entry : componentMap.entrySet()) {
                final String bindingsClassName = entry.getKey();
                final Collection<String> bindings = bindingsMap.get(PACKAGE_BINDINGS);
                assert bindings != null && bindings.contains(bindingsClassName);

                log.detail("Class %s registers:", bindingsClassName);

                final Collection<String> allBindings = entry.getValue();

                if (log.active()) {
                    printBindings(log, "  ", "Component", allBindings);
                }

                final Collection<String> groupBindings = componentGroupMap.remove(bindingsClassName);
                if (groupBindings != null) {
                    if (log.active()) {
                        printBindings(log, "  ", "Group member", groupBindings);
                    }

                    allBindings.addAll(groupBindings);
                }

                generateBindingClass(bindingsClassName, allBindings, classesDirectory);
            }

            for (final Map.Entry<String, Collection<String>> entry : componentGroupMap.entrySet()) {
                final String bindingsClassName = entry.getKey();

                final Collection<String> bindings = bindingsMap.get(PACKAGE_BINDINGS);
                assert bindings != null && bindings.contains(bindingsClassName);

                final Collection<String> groupBindings = entry.getValue();

                if (log.active()) {
                    log.detail("Binding %s adds:", bindingsClassName);
                    printBindings(log, "  ", "Group", groupBindings);
                }

                generateBindingClass(bindingsClassName, groupBindings, classesDirectory);
            }
        }
    }

    private void printBindings(final Logger log, final String indent, final String type, final Collection<String> bindings) {
        log.detail("%s%ss:", indent, type);

        for (final String implementationName : bindings) {
            log.detail("%s%s%s", indent, indent, implementationName);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void generateBindingClass(final String className, final Collection<String> bindings, final File classesDirectory) throws MojoExecutionException {
        final ClassWriter generator = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        generator.visit(V1_5, ACC_FINAL | ACC_PUBLIC, ClassReaders.internalName(className), null, EMPTY_BINDINGS_CLASS_NAME, null);

        {
            final String constructorDesc = Type.getMethodDescriptor(Type.getType(void.class));
            final MethodVisitor method = generator.visitMethod(ACC_PUBLIC, ClassReaders.CONSTRUCTOR_METHOD_NAME, constructorDesc, null, null);
            method.visitCode();

            method.visitVarInsn(ALOAD, 0);
            method.visitMethodInsn(INVOKESPECIAL, EMPTY_BINDINGS_CLASS_NAME, ClassReaders.CONSTRUCTOR_METHOD_NAME, constructorDesc, false);
            method.visitInsn(RETURN);

            method.visitMaxs(0, 0);
            method.visitEnd();
        }

        {
            final String implementedDesc = Type.getMethodDescriptor(implementedMethod);
            final String invokedDesc = Type.getMethodDescriptor(invokedMethod);

            final MethodVisitor method = generator.visitMethod(ACC_PUBLIC, implementedMethod.getName(), implementedDesc, null, null);
            method.visitCode();

            for (final String implementationName : bindings) {
                method.visitVarInsn(ALOAD, 1);
                method.visitLdcInsn(Type.getObjectType(ClassReaders.internalName(implementationName)));
                method.visitInsn(ACONST_NULL);
                method.visitTypeInsn(CHECKCAST, Type.getInternalName(Class[].class));
                method.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(invokedMethod.getDeclaringClass()), invokedMethod.getName(), invokedDesc, true);
            }

            method.visitInsn(Type.getReturnType(implementedDesc).getOpcode(IRETURN));

            method.visitMaxs(0, 0);
            method.visitEnd();
        }

        generator.visitEnd();

        final File file = new File(classesDirectory, ClassReaders.fileName(className));
        file.getParentFile().mkdirs();

        writeClassContents(file, generator);
    }

    private boolean processClass(final ClassReader classData, final ClassProcessor command) throws MojoExecutionException, IOException {
        try {
            return command.run(classData);
        } catch (final IllegalStateException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof MojoExecutionException) {
                throw ((MojoExecutionException) cause);
            } else {
                throw e;
            }
        }
    }

    @SuppressWarnings({ "ResultOfMethodCallIgnored", "MismatchedQueryAndUpdateOfCollection" })
    private void processClasses(final Logger log,
                                final ClassLoader loader,
                                final File classesDirectory,
                                final Map<String, Map<String, Collection<String>>> serviceProviderMap,
                                final Map<String, Collection<String>> componentMap,
                                final Map<String, Collection<String>> componentGroupMap)
            throws IOException, ClassNotFoundException, MojoExecutionException {
        final DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(classesDirectory);
        scanner.setIncludes(new String[] { "**/*.class" });

        scanner.addDefaultExcludes();
        scanner.scan();

        final ClassRepository repository = new ClassRepository(loader);

        final Set<String> publicApis = new HashSet<>();
        final Map<String, Collection<String>> serviceProviders = new HashMap<>();

        for (final String fileName : scanner.getIncludedFiles()) {
            final String className = fileName.substring(0, fileName.length() - ClassLoaders.CLASS_SUFFIX.length()).replace(File.separatorChar, '.');
            final String componentPackage = className.substring(0, className.lastIndexOf(".") + 1);
            final String bindingClassName = componentPackage + GENERATED_PACKAGE_BINDINGS + projectName;

            if (Objects.equals(className, bindingClassName)) {
                new File(classesDirectory, fileName).delete();
            } else {
                final ClassReader classData = repository.reader(className);
                assert classData != null : className;

                final Map<String, Set<String>> serviceProviderApis = new HashMap<>();

                class ClassFlags {
                    boolean ignored;
                    boolean component;
                    boolean group;
                    boolean dependent;
                    Type scope;
                }

                final ClassFlags flags = new ClassFlags();

                final ClassProcessor processor = new ClassProcessor() {
                    public ClassVisitor visitor(final ClassReader reader) {
                        return new ClassVisitor(ASM5) {
                            private final Type serviceProviderType = Type.getType(ServiceProvider.class);
                            private final Type componentType = Type.getType(Component.class);
                            private final Type componentGroupType = Type.getType(ComponentGroup.class);

                            private final String name = ClassReaders.internalName(className);
                            private boolean original;

                            @Override
                            public void visit(final int version,
                                              final int access,
                                              final String name,
                                              final String signature,
                                              final String superName,
                                              final String[] interfaces) {
                                original = Objects.equals(name, this.name);
                                super.visit(version, access, name, signature, superName, interfaces);
                            }

                            @Override
                            public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
                                if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
                                    flags.dependent = name.startsWith("this$");
                                }

                                return super.visitField(access, name, desc, signature, value);
                            }

                            @Override
                            public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
                                final Type type = Type.getType(desc);

                                if (Objects.equals(type, serviceProviderType)) {
                                    return new ServiceProviderProcessor(repository, reader, processor -> {
                                        final String _type = processor.type();
                                        final Set<String> list = serviceProviderApis.computeIfAbsent(_type, ignored -> new HashSet<>());

                                        list.addAll(processor.apiSet());

                                        if (Objects.equals(_type, ServiceProviders.TYPE)) {
                                            publicApis.addAll(list);
                                        }
                                    });
                                } else if (Objects.equals(type, componentType)) {
                                    return new ComponentProcessor(processor -> {
                                        if (original) {
                                            flags.ignored = !processor.isAutomatic();
                                            flags.scope = processor.scope();
                                        }

                                        flags.component = !ClassReaders.isAbstract(classData) && !ClassReaders.isInterface(classData);
                                    });
                                } else if (Objects.equals(type, componentGroupType)) {
                                    return new ComponentProcessor(processor -> flags.group = !flags.dependent);
                                } else {
                                    return null;
                                }
                            }
                        };
                    }

                    public boolean run(final ClassReader classData) throws IOException, MojoExecutionException {
                        try {
                            classData.accept(visitor(classData), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

                            // components and service providers are always concrete classes that can be instantiated on their own
                            final boolean instantiable = !ClassReaders.isAbstract(classData);

                            if (instantiable) {
                                processAncestry(this, classData, repository);
                            }

                            return instantiable;
                        } catch (final Exceptions.Wrapper e) {
                            throw e.rethrow(IOException.class).rethrow(MojoExecutionException.class);
                        }
                    }
                };

                if (processClass(classData, processor)) {
                    final Map<String, Collection<String>> providerMap = providerMap(PackageBindings.SERVICE_TYPE, serviceProviderMap);
                    final String externalName = ClassReaders.externalName(classData);

                    if (flags.scope != null) {
                        if (flags.ignored) {
                            log.warn("Superfluous 'automatic = false' setting for scoped component %s", externalName);
                        }

                        addServiceProvider(providerMap(Component.SCOPE, serviceProviderMap), flags.scope.getClassName(), externalName);
                    } else if (!flags.ignored) {
                        if (flags.component) {
                            addBinding(bindingClassName, externalName, providerMap, componentMap);
                        }

                        if (flags.group) {
                            addBinding(bindingClassName, externalName, providerMap, componentGroupMap);
                        }
                    }

                    if (!flags.dependent) {
                        for (final Map.Entry<String, Set<String>> entry : serviceProviderApis.entrySet()) {
                            final Set<String> providerNames = entry.getValue();

                            addServiceProviders(className, providerNames, providerMap(entry.getKey(), serviceProviderMap));

                            for (final String api : providerNames) {
                                addServiceProvider(serviceProviders, api, className);
                            }
                        }
                    }
                }
            }
        }

        for (final String className : publicApis) {
            final Collection<String> providers = serviceProviders.get(className);

            if (providers != null) {
                for (final String provider : providers) {
                    makePublic(provider, classesDirectory, repository);
                }
            } else {
                assert ClassReaders.isAbstract(repository.reader(ClassReaders.externalName(className))) : className;
            }
        }
    }

    private Map<String, Collection<String>> providerMap(final String type, final Map<String, Map<String, Collection<String>>> serviceProviderMap) {
        return serviceProviderMap.computeIfAbsent(type, ignored -> new HashMap<>());
    }

    private void makePublic(final String className, final File classesDirectory, final ClassRepository repository) throws MojoExecutionException, IOException {
        final File file = new File(classesDirectory, ClassReaders.fileName(className));

        if (file.exists()) {
            writeClassContents(file, ClassReaders.makePublic(className, repository.reader(className)));
        }
    }

    private void writeClassContents(final File file, final ClassWriter writer) throws MojoExecutionException {
        final OutputStream stream;

        try {
            stream = new FileOutputStream(file);
        } catch (final FileNotFoundException e) {
            throw new MojoExecutionException(String.format("Could not write %s", file), e);
        }

        try {
            stream.write(writer.toByteArray());
        } catch (final IOException e) {
            throw new MojoExecutionException(String.format("Could not write %s", file), e);
        } finally {
            try {
                stream.close();
            } catch (final IOException e) {
                // ignore
            }
        }
    }

    private void processAncestry(final ClassProcessor processor, final ClassReader descendant, final ClassRepository repository)
            throws IOException, MojoExecutionException {
        final String superName = descendant.getSuperName();

        if (!Objects.equals(superName, OBJECT_CLASS_NAME)) {
            final ClassReader superClass = repository.reader(superName);

            if (superClass != null) {
                processClass(superClass, processor);
                processAncestry(processor, superClass, repository);
            }
        }

        for (final String api : descendant.getInterfaces()) {
            final ClassReader interfaceClass = repository.reader(api);

            if (interfaceClass != null) {
                processClass(interfaceClass, processor);
                processAncestry(processor, interfaceClass, repository);
            }
        }
    }

    private void addBinding(final String bindingName,
                            final String componentClass,
                            final Map<String, Collection<String>> serviceProviderMap,
                            final Map<String, Collection<String>> componentMap) throws MojoExecutionException {
        addServiceProvider(componentMap, bindingName, componentClass);

        final Collection<String> bindings = serviceProviderMap.get(PACKAGE_BINDINGS);

        if (bindings == null || !bindings.contains(bindingName)) {
            addServiceProviders(bindingName, Collections.singleton(PACKAGE_BINDINGS), serviceProviderMap);
        }
    }

    private void addServiceProvider(final Map<String, Collection<String>> map, final String api, final String className) {
        map.computeIfAbsent(api, ignored -> new HashSet<>()).add(className);
    }

    private void addServiceProviders(final String className, final Collection<String> providerNames, final Map<String, Collection<String>> serviceProviderMap)
            throws MojoExecutionException {
        for (final String providerName : providerNames) {
            final String key = ClassReaders.externalName(providerName);
            Collection<String> list = serviceProviderMap.get(key);

            if (list == null) {
                serviceProviderMap.put(key, list = new HashSet<>());
            } else if (list.contains(className)) {
                throw new MojoExecutionException(String.format("Duplicate service provider class %s", className));
            }

            list.add(className);
        }
    }

    protected String getProjectNameId() {
        final StringBuilder answer = new StringBuilder();
        final CharSequence name = project.getArtifactId();

        boolean capitalize = true;
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);

            if (Character.isJavaIdentifierPart(c)) {
                answer.append(capitalize ? Character.toUpperCase(c) : Character.toLowerCase(c));
                capitalize = false;
            } else {
                capitalize = true;
            }
        }

        return answer.toString();
    }

    public interface ClassProcessor {

        boolean run(ClassReader classData) throws IOException, MojoExecutionException;
    }
}
