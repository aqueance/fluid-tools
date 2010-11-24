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

package org.fluidity.maven;

import java.io.File;
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
import org.fluidity.composition.EmptyPackageBindings;
import org.fluidity.composition.PackageBindings;
import org.fluidity.composition.ServiceProvider;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Mojos that find in a bunch of class files all implementations of a service provider interface, create a service provider file as per the JAR file
 * specification, find all components, generate a package bindings class for each and add these package bindings as service provider descriptor files.
 * <p/>
 * Subclasses should call {@link AbstractAnnotationProcessorMojo#processDirectory(java.io.File)} with the directory containing the classes to process. The Maven
 * build object can be obtained by calling {@link org.fluidity.maven.AbstractAnnotationProcessorMojo#build()}.
 */
public abstract class AbstractAnnotationProcessorMojo extends AbstractMojo implements Opcodes {

    private static final String CLASS_FILE_SUFFIX = ".class";
    private static final String OBJECT_CLASS_NAME = Type.getInternalName(Object.class);
    private static final String EMPTY_BINDINGS_CLASS_NAME = Type.getInternalName(EmptyPackageBindings.class);
    private static final String REGISTRY_CLASS_NAME = Type.getInternalName(ComponentContainer.Registry.class);

    private static final String ATR_API = "api";
    private static final String ATR_AUTOMATIC = "automatic";
    private static final String ATR_FALLBACK = "fallback";
    private static final String PACKAGE_BINDINGS = PackageBindings.class.getName();
    private static final String GENERATED_PACKAGE_BINDINGS = PACKAGE_BINDINGS.substring(PACKAGE_BINDINGS.lastIndexOf(".") + 1) + "$";

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

    @SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
    protected final void processDirectory(File classesDirectory) throws MojoExecutionException {
        projectName = getProjectNameId();

        if (!classesDirectory.exists()) {
            return;
        }

        final File servicesDirectory = new File(new File(classesDirectory, "META-INF"), "services");

        final Map<String, Collection<String>> serviceProviderMap = new HashMap<String, Collection<String>>();
        final Map<String, Map<String, String>> componentMap = new HashMap<String, Map<String, String>>();

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
            processClasses(repository, classesDirectory, serviceProviderMap, componentMap);
        } catch (final IOException e) {
            throw new MojoExecutionException("Error processing service providers", e);
        } catch (final ClassNotFoundException e) {
            throw new MojoExecutionException("Error processing service providers", e);
        }

        for (final Map.Entry<String, Collection<String>> entry : serviceProviderMap.entrySet()) {
            final Collection<String> list = entry.getValue();

            if (!list.isEmpty()) {
                servicesDirectory.mkdirs();

                if (!servicesDirectory.exists()) {
                    throw new MojoExecutionException("Could not create " + classesDirectory);
                }
            }
        }

        for (final Map.Entry<String, Collection<String>> entry : serviceProviderMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                log.info("Service provider descriptor META-INF/services/" + entry.getKey() + " contains:");
                final File serviceProviderFile = new File(servicesDirectory, entry.getKey());
                serviceProviderFile.delete();

                try {
                    final PrintWriter writer = new PrintWriter(new FileWriter(serviceProviderFile));

                    try {
                        for (final String className : entry.getValue()) {
                            writer.println(className);
                            log.info("  " + className + (componentMap.containsKey(className) ? " (generated)" : ""));
                        }
                    } finally {
                        writer.close();
                    }
                } catch (final IOException e) {
                    throw new MojoExecutionException("Error opening file" + serviceProviderFile, e);
                }
            }
        }

        for (final Map.Entry<String, Map<String, String>> bindingsEntry : componentMap.entrySet()) {
            final String bindingsClassName = bindingsEntry.getKey();
            final Collection<String> bindings = serviceProviderMap.get(PACKAGE_BINDINGS);
            assert bindings != null && bindings.contains(bindingsClassName);

            generateBindingClass(bindingsClassName, bindingsEntry, classesDirectory);
        }
    }

    @SuppressWarnings({ "ResultOfMethodCallIgnored" })
    private void generateBindingClass(final String className, final Map.Entry<String, Map<String, String>> bindings, final File classesDirectory)
            throws MojoExecutionException {
        log.info("Service provider " + className + " binds:");

        final ClassWriter generator = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        generator.visit(V1_5, ACC_FINAL | ACC_PUBLIC, className.replace(".", "/"), null, EMPTY_BINDINGS_CLASS_NAME, null);

        {
            final MethodVisitor method = generator.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            method.visitCode();

            method.visitVarInsn(ALOAD, 0);
            method.visitMethodInsn(INVOKESPECIAL, EMPTY_BINDINGS_CLASS_NAME, "<init>", "()V");
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

            for (final Map.Entry<String, String> entry : bindings.getValue().entrySet()) {
                final String interfaceName = entry.getKey();
                final String implementationName = entry.getValue();

                log.info("  " + interfaceName + " to " + implementationName);

                method.visitVarInsn(ALOAD, 1);
                method.visitLdcInsn(Type.getObjectType(interfaceName.replace('.', '/')));
                method.visitLdcInsn(Type.getObjectType(implementationName.replace('.', '/')));
                method.visitMethodInsn(INVOKEINTERFACE,
                                       REGISTRY_CLASS_NAME,
                                       "bindComponent",
                                       String.format("(%s%s)V", Type.getDescriptor(Class.class), Type.getDescriptor(Class.class)));
            }

            method.visitInsn(RETURN);

            method.visitMaxs(0, 0);
            method.visitEnd();
        }

        generator.visitEnd();

        final File file = new File(classesDirectory, className.replace('.', '/') + CLASS_FILE_SUFFIX);
        file.getParentFile().mkdirs();

        try {
            final OutputStream stateOutput = new FileOutputStream(file);
            stateOutput.write(generator.toByteArray());
            stateOutput.close();
        } catch (final IOException e) {
            throw new MojoExecutionException("Could not generate default package bindings class", e);
        }
    }

    private ClassReader reader(final String className, final ClassLoader repository, final Map<String, ClassReader> readers) throws IOException {
        if (className == null) {
            return null;
        }

        if (!readers.containsKey(className)) {
            final InputStream stream = repository.getResourceAsStream(className.replace('.', '/') + CLASS_FILE_SUFFIX);
            readers.put(className, stream == null ? null : new ClassReader(stream));
        }

        return readers.get(className);
    }

    interface Query {

        boolean run(ClassReader argument) throws IOException, MojoExecutionException;
    }

    private boolean processClass(final Query command, ClassReader argument) throws MojoExecutionException, IOException {
        try {
            return command.run(argument);
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
    private void processClasses(final ClassLoader repository,
                                final File classesDirectory,
                                final Map<String, Collection<String>> serviceProviderMap,
                                final Map<String, Map<String, String>> componentMap) throws IOException, ClassNotFoundException, MojoExecutionException {
        final DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(classesDirectory);
        scanner.setIncludes(new String[] { "**/*.class" });

        scanner.addDefaultExcludes();
        scanner.scan();

        final Map<String, ClassReader> readers = new HashMap<String, ClassReader>();
        final Type serviceProviderAnnotationType = Type.getType(ServiceProvider.class);
        final Type componentAnnotationType = Type.getType(Component.class);

        for (final String fileName : scanner.getIncludedFiles()) {
            final String className = fileName.substring(0, fileName.length() - ".class".length()).replace(File.separatorChar, '.');
            final String componentPackage = className.substring(0, className.lastIndexOf(".") + 1);
            final String generatedBindings = componentPackage + GENERATED_PACKAGE_BINDINGS + projectName;

            if (!className.equals(generatedBindings)) {
                final ClassReader classData = reader(className, repository, readers);
                assert classData != null : className;

                final Set<String> serviceProviderApis = new HashSet<String>();
                final Set<String> componentApis = new HashSet<String>();

                final class ComponentAnnotationVisitor extends EmptyVisitor {

                    final Set<String> componentApis;

                    private String api = null;
                    private boolean fallback = false;
                    private boolean automatic = true;

                    ComponentAnnotationVisitor(final Set<String> componentApis) {
                        this.componentApis = componentApis;
                    }

                    @Override
                    public void visit(final String name, final Object value) {
                        if (ATR_FALLBACK.equals(name) && ((Boolean) value)) {
                            fallback = true;
                        } else if (ATR_API.equals(name)) {
                            api = ((Type) value).getClassName();
                        } else if (ATR_AUTOMATIC.equals(name) && !((Boolean) value)) {
                            automatic = false;
                        }
                    }

                    @Override
                    public void visitEnd() {
                        if (automatic) {
                            componentApis.add(fallback ? className : api);
                        }
                    }
                }

                final class ServiceProviderAnnotationVisitor extends EmptyVisitor {

                    final ClassReader classData;
                    final boolean abstractClass;
                    final Set<String> serviceProviderApis;

                    ServiceProviderAnnotationVisitor(final ClassReader classData, boolean abstractClass, final Set<String> serviceProviderApis) {
                        this.classData = classData;
                        this.abstractClass = abstractClass;
                        this.serviceProviderApis = serviceProviderApis;
                    }

                    @Override
                    public AnnotationVisitor visitArray(final String name) {
                        assert ATR_API.equals(name) : name;
                        return new EmptyVisitor() {

                            @Override
                            public void visit(final String ignore, final Object value) {
                                assert ignore == null : ignore;
                                serviceProviderApis.add(((Type) value).getClassName());
                            }
                        };
                    }

                    @Override
                    public void visitEnd() {
                        if (serviceProviderApis.isEmpty()) {
                            try {
                                findServiceProviders(abstractClass, classData, serviceProviderApis, repository, readers);
                            } catch (final Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }

                final class AnnotationsVisitor extends EmptyVisitor {

                    private final ClassReader classData;

                    private final Set<String> serviceProviders = new HashSet<String>();
                    private final Set<String> components = new HashSet<String>();

                    private boolean abstractClass;
                    private boolean innerClass;

                    AnnotationsVisitor(final ClassReader classData) {
                        this.classData = classData;
                    }

                    @Override
                    public void visit(final int version,
                                      final int access,
                                      final String name,
                                      final String signature,
                                      final String superName,
                                      final String[] interfaces) {
                        serviceProviders.clear();
                        components.clear();
                        abstractClass = (access & ACC_ABSTRACT) != 0;
                    }

                    @Override
                    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
                        final Type type = Type.getType(desc);

                        if (serviceProviderAnnotationType.equals(type)) {
                            return new ServiceProviderAnnotationVisitor(classData, abstractClass, serviceProviders);
                        } else if (componentAnnotationType.equals(type)) {
                            return new ComponentAnnotationVisitor(components);
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
                        if ((access & ACC_SYNTHETIC) != 0 && name.startsWith("this$")) {
                            innerClass = true;
                        }

                        return null;
                    }

                    @Override
                    public void visitEnd() {
                        if (componentApis.size() > 1) {
                            throw new IllegalStateException(String.format("Component class %s defines multiple component APIs", classData.getClassName()));
                        }

                        if (!innerClass) {
                            serviceProviderApis.addAll(serviceProviders);

                            if (!abstractClass) {
                                if (componentApis.isEmpty()) {
                                    componentApis.addAll(components);
                                }
                            }
                        }
                    }
                }

                final Query annotationsQuery = new Query() {
                    public boolean run(final ClassReader classData) throws IOException, MojoExecutionException {
                        final AnnotationsVisitor visitor = new AnnotationsVisitor(classData);

                        classData.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

                        // components and service providers are always concrete classes that can be instantiated on their own
                        final boolean instantiableClass = !visitor.abstractClass && !visitor.innerClass;

                        if (instantiableClass) {
                            processAncestry(this, classData, repository, readers);
                        }

                        return instantiableClass;
                    }
                };

                if (processClass(annotationsQuery, classData)) {
                    if (!serviceProviderApis.isEmpty()) {
                        foundServiceProvider(serviceProviderMap, serviceProviderApis, className);
                    }

                    if (serviceProviderApis.size() > 1 && componentApis.isEmpty()) {
                        componentApis.add(className);
                    }

                    if (!componentApis.isEmpty()) {
                        assert componentApis.size() == 1 : componentApis;

                        String componentApi = componentApis.iterator().next();

                        if (componentApi == null) {
                            componentApi = findComponentInterface(classData, repository, readers).replace('/', '.');
                        }

                        if (componentApi != null) {
                            foundComponent(generatedBindings, serviceProviderMap, componentMap, className, componentApi);
                        } else {
                            log.warn("No component interface could be identified for " + className);
                        }
                    }
                }
            } else {
                new File(classesDirectory, fileName).delete();
            }
        }
    }

    private void processAncestry(final Query annotationsQuery,
                                 final ClassReader descendant,
                                 final ClassLoader repository,
                                 final Map<String, ClassReader> readers) throws IOException, MojoExecutionException {
        final String superName = descendant.getSuperName();

        if (!superName.equals(OBJECT_CLASS_NAME)) {
            final ClassReader superClass = reader(superName, repository, readers);
            if (superClass != null) {
                processClass(annotationsQuery, superClass);
                processAncestry(annotationsQuery, superClass, repository, readers);
            }
        }

        for (final String api : descendant.getInterfaces()) {
            final ClassReader interfaceClass = reader(api, repository, readers);
            processClass(annotationsQuery, interfaceClass);
            processAncestry(annotationsQuery, interfaceClass, repository, readers);
        }
    }

    private void findServiceProviders(final boolean abstractClass,
                                      final ClassReader classData,
                                      final Set<String> serviceProviderApis,
                                      final ClassLoader repository,
                                      final Map<String, ClassReader> readers) throws IOException, ClassNotFoundException {
        final String api = findSingleInterface(classData, repository, readers);

        if (api != null) {
            serviceProviderApis.add(api);
        } else if (abstractClass) {
            serviceProviderApis.add(classData.getClassName());
        } else {
            log.warn("No service provider interface could be identified for " + classData.getClassName().replace('/', '.'));
        }
    }

    private String findComponentInterface(final ClassReader classData, final ClassLoader repository, final Map<String, ClassReader> readers)
            throws ClassNotFoundException, IOException {
        final String foundInterface = findSingleInterface(classData, repository, readers);
        return foundInterface == null ? classData.getClassName() : foundInterface;
    }

    private String findSingleInterface(final ClassReader classData, final ClassLoader repository, final Map<String, ClassReader> readers)
            throws ClassNotFoundException, IOException {
        if (classData == null) {
            return null;
        }

        final String[] interfaces = classData.getInterfaces();

        if (interfaces.length == 1) {
            return interfaces[0];
        } else if (interfaces.length == 0) {
            return findSingleInterface(reader(classData.getSuperName(), repository, readers), repository, readers);
        }

        return null;
    }

    private void foundComponent(final String generatedBindings,
                                final Map<String, Collection<String>> serviceProviderMap,
                                final Map<String, Map<String, String>> componentMap,
                                final String componentClass,
                                final String componentApi) throws MojoExecutionException {
        Map<String, String> packageMap = componentMap.get(generatedBindings);

        if (packageMap == null) {
            componentMap.put(generatedBindings, packageMap = new HashMap<String, String>());
        }

        packageMap.put(componentApi, componentClass);

        final Collection<String> bindings = serviceProviderMap.get(PACKAGE_BINDINGS);
        if (bindings == null || !bindings.contains(generatedBindings)) {
            foundServiceProvider(serviceProviderMap, Collections.singleton(PACKAGE_BINDINGS), generatedBindings);
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

    private void foundServiceProvider(final Map<String, Collection<String>> serviceProviderMap, final Collection<String> providerNames, final String className)
            throws MojoExecutionException {
        for (final String providerName : providerNames) {
            final String key = providerName.replace('/', '.');
            Collection<String> list = serviceProviderMap.get(key);

            if (list == null) {
                serviceProviderMap.put(key, list = new HashSet<String>());
            } else if (list.contains(className)) {
                throw new MojoExecutionException("Duplicate service provider class " + className);
            }

            list.add(className);
        }
    }
}
