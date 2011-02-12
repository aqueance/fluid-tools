/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.maven.annotation.ComponentProcessor;
import org.fluidity.composition.maven.annotation.ProcessorCallback;
import org.fluidity.composition.maven.annotation.ServiceProviderProcessor;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Exceptions;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Mojos that find in a bunch of class files all implementations of a service provider interface, create a service provider file as per the JAR file
 * specification, find all components, generate a package bindings class for each and add these package bindings as service provider descriptor files.
 * <p/>
 * Subclasses should call {@link AbstractAnnotationProcessorMojo#processDirectory(java.io.File)} with the directory containing the classes to process. The Maven
 * build object can be obtained by calling {@link AbstractAnnotationProcessorMojo#build()}.
 *
 * @threadSafe
 */
public abstract class AbstractAnnotationProcessorMojo extends AbstractMojo implements Opcodes {

    private static final String OBJECT_CLASS_NAME = Type.getInternalName(Object.class);
    private static final String EMPTY_BINDINGS_CLASS_NAME = Type.getInternalName(EmptyPackageBindings.class);
    private static final String REGISTRY_CLASS_NAME = Type.getInternalName(ComponentContainer.Registry.class);

    private static final String PACKAGE_BINDINGS = PackageBindings.class.getName();
    private static final String GENERATED_PACKAGE_BINDINGS = PACKAGE_BINDINGS.substring(PACKAGE_BINDINGS.lastIndexOf(".") + 1).concat("$");

    /**
     * Reference of the maven project
     *
     * @parameter expression="${project}"
     * @required
     */
    @SuppressWarnings("UnusedDeclaration")
    private MavenProject project;

    private final Log log = getLog();

    private String projectName;

    protected final Build build() {
        return project.getBuild();
    }

    @SuppressWarnings({ "RedundantCast", "ResultOfMethodCallIgnored" })
    protected final void processDirectory(final File classesDirectory) throws MojoExecutionException {
        projectName = getProjectNameId();

        if (!classesDirectory.exists()) {
            return;
        }

        final Map<String, Map<String, Collection<String>>> serviceProviderMap = new HashMap<String, Map<String, Collection<String>>>();
        final Map<String, Map<String, Set<String>>> componentMap = new HashMap<String, Map<String, Set<String>>>();
        final Map<String, Map<String, Set<String>>> componentGroupMap = new HashMap<String, Map<String, Set<String>>>();

        final List<URL> urls = new ArrayList<URL>();

        try {
            urls.add(classesDirectory.toURI().toURL());

            for (final Artifact artifact : (Set<Artifact>) project.getArtifacts()) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        } catch (final MalformedURLException e) {
            assert false : e;
        }

        final ClassLoader repository = new URLClassLoader(urls.toArray(new URL[urls.size()]));

        try {
            processClasses(repository, classesDirectory, serviceProviderMap, componentMap, componentGroupMap);
        } catch (final IOException e) {
            throw new MojoExecutionException("Error processing service providers", e);
        } catch (final ClassNotFoundException e) {
            throw new MojoExecutionException("Error processing service providers", e);
        }

        for (final Map.Entry<String, Map<String, Collection<String>>> entry : serviceProviderMap.entrySet()) {
            final String type = entry.getKey();
            final Map<String, Collection<String>> providerMap = entry.getValue();
            final File servicesDirectory = new File(new File(classesDirectory, "META-INF"), type);

            for (final Map.Entry<String, Collection<String>> providerEntry : providerMap.entrySet()) {
                final Collection<String> list = providerEntry.getValue();

                if (!list.isEmpty()) {
                    servicesDirectory.mkdirs();

                    if (!servicesDirectory.exists()) {
                        throw new MojoExecutionException("Could not create " + classesDirectory);
                    }
                }
            }

            for (final Map.Entry<String, Collection<String>> providerEntry : providerMap.entrySet()) {
                if (!providerEntry.getValue().isEmpty()) {
                    log.info(String.format("Service provider descriptor META-INF/%s/%s contains:", type, providerEntry.getKey()));
                    final File serviceProviderFile = new File(servicesDirectory, providerEntry.getKey());
                    serviceProviderFile.delete();

                    try {
                        final PrintWriter writer = new PrintWriter(new FileWriter(serviceProviderFile));

                        try {
                            for (final String className : providerEntry.getValue()) {
                                writer.println(className);
                                log.info(String.format("  %s%s", className, componentMap.containsKey(className) ? " (generated)" : ""));
                            }
                        } finally {
                            writer.close();
                        }
                    } catch (final IOException e) {
                        throw new MojoExecutionException("Error opening file" + serviceProviderFile, e);
                    }
                }
            }
        }

        final Map<String, Collection<String>> bindingsMap = serviceProviderMap.get(PackageBindings.SERVICE_TYPE);

        if (bindingsMap != null) {
            for (final Map.Entry<String, Map<String, Set<String>>> entry : componentMap.entrySet()) {
                final String bindingsClassName = entry.getKey();
                final Collection<String> bindings = bindingsMap.get(PACKAGE_BINDINGS);
                assert bindings != null && bindings.contains(bindingsClassName);

                log.info(String.format("Binding %s adds:", bindingsClassName));
                final Map<String, Set<String>> allBindings = entry.getValue();

                printBindings("  ", "Component", allBindings);

                final Map<String, Set<String>> groupBindings = componentGroupMap.remove(bindingsClassName);
                if (groupBindings != null) {
                    printBindings("  ", "Group", groupBindings);
                    allBindings.putAll(groupBindings);
                }

                generateBindingClass(bindingsClassName, allBindings, classesDirectory);
            }

            for (final Map.Entry<String, Map<String, Set<String>>> entry : componentGroupMap.entrySet()) {
                final String bindingsClassName = entry.getKey();

                final Collection<String> bindings = bindingsMap.get(PACKAGE_BINDINGS);
                assert bindings != null && bindings.contains(bindingsClassName);

                final Map<String, Set<String>> groupBindings = entry.getValue();

                log.info(String.format("Binding %s adds:", bindingsClassName));
                printBindings("  ", "Group", groupBindings);

                generateBindingClass(bindingsClassName, groupBindings, classesDirectory);
            }
        }
    }

    private void printBindings(final String indent, final String type, final Map<String, Set<String>> bindings) {
        log.info(String.format("%s%s bindings:", indent, type));
        for (final Map.Entry<String, Set<String>> all : bindings.entrySet()) {
            log.info(String.format("%s%s%s to %s", indent, indent, all.getKey(), all.getValue()));
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void generateBindingClass(final String className, final Map<String, Set<String>> bindings, final File classesDirectory)
            throws MojoExecutionException {
        final ClassWriter generator = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        generator.visit(V1_5, ACC_FINAL | ACC_PUBLIC, ClassReaders.internalName(className), null, EMPTY_BINDINGS_CLASS_NAME, null);

        {
            final MethodVisitor method = generator.visitMethod(ACC_PUBLIC, ClassReaders.CONSTRUCTOR_METHOD_NAME, "()V", null, null);
            method.visitCode();

            method.visitVarInsn(ALOAD, 0);
            method.visitMethodInsn(INVOKESPECIAL, EMPTY_BINDINGS_CLASS_NAME, ClassReaders.CONSTRUCTOR_METHOD_NAME, "()V");
            method.visitInsn(RETURN);

            method.visitMaxs(0, 0);
            method.visitEnd();
        }

        {
            final MethodVisitor method = generator.visitMethod(ACC_PUBLIC,
                                                               "bindComponents",
                                                               String.format("(%s)V", Type.getDescriptor(ComponentContainer.Registry.class)),
                                                               null,
                                                               null);
            method.visitCode();

            for (final Map.Entry<String, Set<String>> entry : bindings.entrySet()) {
                final String implementationName = entry.getKey();

                method.visitVarInsn(ALOAD, 1);
                method.visitLdcInsn(Type.getObjectType(ClassReaders.internalName(implementationName)));
                method.visitMethodInsn(INVOKEINTERFACE, REGISTRY_CLASS_NAME, "bindComponent", String.format("(%s)V", Type.getDescriptor(Class.class)));
            }

            method.visitInsn(RETURN);

            method.visitMaxs(0, 0);
            method.visitEnd();
        }

        generator.visitEnd();

        final File file = new File(classesDirectory, ClassReaders.fileName(className));
        file.getParentFile().mkdirs();

        writeClassContents(file, generator);
    }

    private static class ClassRepositoryImpl implements ClassRepository {

        private final Map<String, ClassReader> readers = new HashMap<String, ClassReader>();
        private final ClassLoader loader;

        public ClassRepositoryImpl(final ClassLoader loader) {
            this.loader = loader;
        }

        public ClassReader reader(final String name) throws IOException {
            if (name == null) {
                return null;
            }

            if (!readers.containsKey(name)) {
                final InputStream stream = loader.getResourceAsStream(ClassReaders.fileName(name));
                readers.put(name, stream == null ? null : new ClassReader(stream));
            }

            return readers.get(name);
        }
    }

    private boolean processClass(final ClassReader classData, final ClassProcessor command) throws MojoExecutionException, IOException {
        try {
            return command.run(classData);
        } catch (final IllegalStateException e) {
            final Throwable cause = e.getCause();

            if (cause != null && cause instanceof MojoExecutionException) {
                throw ((MojoExecutionException) cause);
            } else {
                throw e;
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void processClasses(final ClassLoader loader,
                                final File classesDirectory,
                                final Map<String, Map<String, Collection<String>>> serviceProviderMap,
                                final Map<String, Map<String, Set<String>>> componentMap,
                                final Map<String, Map<String, Set<String>>> componentGroupMap)
            throws IOException, ClassNotFoundException, MojoExecutionException {
        final DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(classesDirectory);
        scanner.setIncludes(new String[] { "**/*.class" });

        scanner.addDefaultExcludes();
        scanner.scan();

        final ClassRepository repository = new ClassRepositoryImpl(loader);

        final Set<String> publicApis = new HashSet<String>();
        final Map<String, Set<String>> serviceProviders = new HashMap<String, Set<String>>();

        for (final String fileName : scanner.getIncludedFiles()) {
            final String className = fileName.substring(0, fileName.length() - ClassLoaders.CLASS_SUFFIX.length()).replace(File.separatorChar, '.');
            final String componentPackage = className.substring(0, className.lastIndexOf(".") + 1);
            final String bindingClassName = componentPackage + GENERATED_PACKAGE_BINDINGS + projectName;

            final boolean isComponent[] = new boolean[1];
            final boolean isComponentGroup[] = new boolean[1];

            if (className.equals(bindingClassName)) {
                new File(classesDirectory, fileName).delete();
            } else {
                final ClassReader classData = repository.reader(className);
                assert classData != null : className;

                final Map<String, Set<String>> serviceProviderApis = new HashMap<String, Set<String>>();
                final Set<String> componentApis = new HashSet<String>();
                final Set<String> componentGroupApis = new HashSet<String>();

                final ClassVisitor annotations = new EmptyVisitor() {
                    private final Type serviceProviderType = Type.getType(ServiceProvider.class);
                    private final Type componentType = Type.getType(Component.class);
                    private final Type componentGroupType = Type.getType(ComponentGroup.class);

                    @Override
                    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
                        final Type type = Type.getType(desc);
                        if (serviceProviderType.equals(type)) {
                            return new ServiceProviderProcessor(repository, classData, new ProcessorCallback<ServiceProviderProcessor>() {
                                public void complete(final ServiceProviderProcessor processor) {
                                    final String type = processor.type();
                                    Set<String> list = serviceProviderApis.get(type);

                                    if (list == null) {
                                        serviceProviderApis.put(type, list = new HashSet<String>());
                                    }

                                    list.addAll(processor.apiSet());

                                    if (processor.isDefaultType()) {
                                        publicApis.addAll(list);
                                    }
                                }
                            });
                        } else if (componentType.equals(type)) {
                            return new ComponentProcessor(new ProcessorCallback<ComponentProcessor>() {
                                public void complete(final ComponentProcessor processor) {
                                    if (processor.isAutomatic()) {
                                        isComponent[0] = !ClassReaders.isAbstract(classData) && !ClassReaders.isInterface(classData);

                                        if (isComponent[0] && componentApis.isEmpty()) {
                                            componentApis.addAll(processor.apiSet());
                                        } else {
                                            log.warn(String.format("Skipping %s as it is not a concrete class", className));
                                        }
                                    }
                                }
                            });
                        } else if (componentGroupType.equals(type)) {
                            return new ComponentProcessor(new ProcessorCallback<ComponentProcessor>() {
                                public void complete(final ComponentProcessor processor) {
                                    isComponentGroup[0] = true;

                                    if (isComponentGroup[0]) {
                                        componentGroupApis.addAll(processor.apiSet());
                                    }
                                }
                            });
                        } else {
                            return null;
                        }
                    }
                };

                final ClassProcessor processor = new ClassProcessor() {
                    public boolean run(final ClassReader classData) throws IOException, MojoExecutionException {
                        try {
                            classData.accept(annotations, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

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

                    if (isComponent[0]) {
                        handleComponent("component", classData, repository, componentApis, componentMap, providerMap, bindingClassName);
                    }

                    if (isComponentGroup[0]) {
                        handleComponent("group", classData, repository, componentGroupApis, componentGroupMap, providerMap, bindingClassName);
                    }

                    for (final Map.Entry<String, Set<String>> entry : serviceProviderApis.entrySet()) {

                        final Set<String> list = entry.getValue();
                        if (!list.isEmpty()) {
                            addServiceProviders(className, list, providerMap(entry.getKey(), serviceProviderMap));
                        }

                        for (final String api : list) {
                            Set<String> providers = serviceProviders.get(api);

                            if (providers == null) {
                                serviceProviders.put(api, providers = new HashSet<String>());
                            }

                            providers.add(className);
                        }
                    }
                }
            }
        }

        for (final String className : publicApis) {
            final Set<String> providers = serviceProviders.get(className);

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
        Map<String, Collection<String>> providerMap = serviceProviderMap.get(type);

        if (providerMap == null) {
            serviceProviderMap.put(type, providerMap = new HashMap<String, Collection<String>>());
        }

        return providerMap;
    }

    private void handleComponent(final String type,
                                 final ClassReader classData,
                                 final ClassRepository repository,
                                 final Set<String> apiList,
                                 final Map<String, Map<String, Set<String>>> componentMap,
                                 final Map<String, Collection<String>> serviceProviderMap,
                                 final String bindingName) throws IOException, MojoExecutionException {
        if (apiList.isEmpty()) {
            apiList.addAll(ClassReaders.findDirectInterfaces(classData, repository));
        }

        if (apiList.isEmpty()) {
            apiList.add(ClassReaders.externalName(classData));
        }

        final String className = ClassReaders.externalName(classData);

        if (!apiList.isEmpty()) {
            addBinding(bindingName, className, apiList, serviceProviderMap, componentMap);
        } else {
            log.warn(String.format("No %s interface could be identified for %s", type, className));
        }
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

        if (!superName.equals(OBJECT_CLASS_NAME)) {
            final ClassReader superClass = repository.reader(superName);
            if (superClass != null) {
                processClass(superClass, processor);
                processAncestry(processor, superClass, repository);
            }
        }

        for (final String api : descendant.getInterfaces()) {
            final ClassReader interfaceClass = repository.reader(api);
            processClass(interfaceClass, processor);
            processAncestry(processor, interfaceClass, repository);
        }
    }

    private void addBinding(final String bindingName,
                            final String componentClass,
                            final Set<String> apiList,
                            final Map<String, Collection<String>> serviceProviderMap,
                            final Map<String, Map<String, Set<String>>> componentMap) throws MojoExecutionException {
        Map<String, Set<String>> packageMap = componentMap.get(bindingName);

        if (packageMap == null) {
            componentMap.put(bindingName, packageMap = new HashMap<String, Set<String>>());
        }

        packageMap.put(componentClass, apiList);

        final Collection<String> bindings = serviceProviderMap.get(PACKAGE_BINDINGS);
        if (bindings == null || !bindings.contains(bindingName)) {
            addServiceProviders(bindingName, Collections.singleton(PACKAGE_BINDINGS), serviceProviderMap);
        }
    }

    private void addServiceProviders(final String className, final Collection<String> providerNames, final Map<String, Collection<String>> serviceProviderMap)
            throws MojoExecutionException {
        for (final String providerName : providerNames) {
            final String key = ClassReaders.externalName(providerName);
            Collection<String> list = serviceProviderMap.get(key);

            if (list == null) {
                serviceProviderMap.put(key, list = new HashSet<String>());
            } else if (list.contains(className)) {
                throw new MojoExecutionException("Duplicate service provider class " + className);
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

    public static interface ClassProcessor {

        boolean run(ClassReader classData) throws IOException, MojoExecutionException;
    }
}
