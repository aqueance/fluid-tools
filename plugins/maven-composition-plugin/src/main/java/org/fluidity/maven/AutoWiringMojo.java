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
package org.fluidity.maven;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.AnnotationElementValue;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Annotations;
import org.apache.bcel.classfile.ArrayElementValue;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.ElementValuePair;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.RuntimeVisibleAnnotations;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC_W;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.ClassLoaderRepository;
import org.apache.bcel.util.Repository;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.EmptyPackageBindings;
import org.fluidity.composition.PackageBindings;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.ServiceProviderList;

/**
 * Goal that finds all implementations of a service provider interface and creates a service provider file as per the
 * JAR file specification and finds all components, generates a package bindings class for each and adds these package
 * bindings as service provider descriptor files.
 *
 * @goal process-annotations
 * @phase process-classes
 * @requiresDependencyResolution compile
 */
public class AutoWiringMojo extends AbstractMojo {

    private static final String ATN_COMPONENT = "L" + Component.class.getName().replace('.', '/') + ";";
    private static final String ATN_SERVICE_PROVIDER = "L" + ServiceProvider.class.getName().replace('.', '/') + ";";
    private static final String ATN_SERVICE_PROVIDER_LIST =
        "L" + ServiceProviderList.class.getName().replace('.', '/') + ";";
    private static final String ATR_API = "api";
    private static final String ATR_VALUE = "value";
    private static final String ATR_AUTOMATIC = "automatic";
    private static final String ATTR_FALLBACK = "fallback";
    private static final String PACKAGE_BINDINGS = PackageBindings.class.getName();
    private static final String GENERATED_PACKAGE_BINDINGS =
        PACKAGE_BINDINGS.substring(PACKAGE_BINDINGS.lastIndexOf(".") + 1) + "$";

    /**
     * Reference of the maven project
     *
     * @parameter expression="${project}"
     * @required
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private MavenProject project;

    /**
     * The location of the compiled classes.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private File outputDirectory;

    /**
     * The location of the compiled classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private File classesDirectory;

    private final Log log = getLog();

    private String projectName;

    @SuppressWarnings({ "unchecked" })
    public void execute() throws MojoExecutionException {
        projectName = getProjectNameId();

        if (!classesDirectory.exists()) {
            log.info("No classes to scan");
            return;
        }

        final File servicesDirectory = new File(new File(classesDirectory, "META-INF"), "services");

        final Map<String, Collection<String>> serviceProviderMap = new HashMap<String, Collection<String>>();
        final Map<String, Map<String, String>> componentMap = new HashMap<String, Map<String, String>>();

        final List<URL> urls = new ArrayList<URL>();

        try {
            urls.add(classesDirectory.toURL());
            for (final Artifact artifact : (Set<Artifact>) project.getArtifacts()) {
                urls.add(artifact.getFile().toURL());
            }
        } catch (final MalformedURLException e) {
            assert false : e;
        }

        final Repository repository = new ClassLoaderRepository(new URLClassLoader(urls.toArray(new URL[urls.size()])));

        try {
            processClasses(repository, serviceProviderMap, componentMap);
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

            log.info("Service provider " + bindingsClassName + " binds:");

            final ClassGen cg = new ClassGen(bindingsClassName, EmptyPackageBindings.class.getName(), null,
                Constants.ACC_FINAL + Constants.ACC_PUBLIC, null);
            cg.setMajor(Constants.MAJOR_1_5);
            cg.setMinor(Constants.MINOR_1_5);

            final ConstantPoolGen cp = cg.getConstantPool();
            final InstructionFactory factory = new InstructionFactory(cg);

            InstructionList code = new InstructionList();
            final MethodGen initMethod = new MethodGen(Constants.ACC_PUBLIC, Type.VOID,
                Type.NO_ARGS,
                null, "<init>", bindingsClassName,
                code, cp);

            code.append(InstructionConstants.ALOAD_0);
            code.append(factory.createInvoke(cg.getSuperclassName(), "<init>", Type.VOID, Type.NO_ARGS,
                Constants.INVOKESPECIAL));
            code.append(InstructionConstants.RETURN);
            initMethod.setMaxStack();
            initMethod.setMaxLocals();
            cg.addMethod(initMethod.getMethod());
            code.dispose();

            code = new InstructionList();
            final MethodGen registerMethod = new MethodGen(Constants.ACC_PUBLIC, Type.VOID,
                new Type[] { Type.getType(ComponentContainer.Registry.class) },
                new String[] { "registry" }, "registerComponents", bindingsClassName,
                code, cp);

            for (final Map.Entry<String, String> entry : bindingsEntry.getValue().entrySet()) {
                final String interfaceName = entry.getKey();
                final String implementationName = entry.getValue();

                log.info("  " + interfaceName + " to " + implementationName);

                code.append(InstructionConstants.ALOAD_1);
                code.append(new LDC_W(cp.addClass(interfaceName)));
                code.append(new LDC_W(cp.addClass(implementationName)));
                code.append(factory.createInvoke(ComponentContainer.Registry.class.getName(),
                    "bind", Type.VOID, new Type[] {
                    Type.getType(Class.class),
                    Type.getType(Class.class),
                }, Constants.INVOKEINTERFACE));
            }

            code.append(InstructionConstants.RETURN);
            registerMethod.setMaxStack();
            registerMethod.setMaxLocals();
            cg.addMethod(registerMethod.getMethod());
            code.dispose();

            final File file = new File(classesDirectory, bindingsClassName.replace('.', '/') + ".class");
            file.getParentFile().mkdirs();

            try {
                cg.getJavaClass().dump(file);
            } catch (final IOException e) {
                throw new MojoExecutionException("Could not generate default package bindings class", e);
            }
        }
    }

    private void processClasses(final Repository repository,
                                final Map<String, Collection<String>> serviceProviderMap,
                                final Map<String, Map<String, String>> componentMap)
        throws IOException, ClassNotFoundException, MojoExecutionException {

        final DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(classesDirectory);
        scanner.setIncludes(new String[] { "**/*.class" });

        scanner.addDefaultExcludes();
        scanner.scan();

        FILES:
        for (final String fileName : scanner.getIncludedFiles()) {
            final String className = fileName.substring(0, fileName.length() - ".class".length()).replace(File.separatorChar, '.');
            final String componentPackage = className.substring(0, className.lastIndexOf("."));
            final String generatedBindings = componentPackage + "." + GENERATED_PACKAGE_BINDINGS + projectName;

            if (!className.equals(generatedBindings)) {
                final JavaClass classData = repository.loadClass(className);

                if (!classData.isAbstract()) {
                    for (final Field field : classData.getFields()) {
                        if (field.getName().startsWith("this$")) {

                            // we are looking at a non-static nested class, skip it
                            continue FILES;
                        }
                    }

                    final Set<String> serviceProviderApi = new HashSet<String>();

                    boolean component = false;
                    boolean fallback = false;
                    String componentApi = null;

                    for (final Attribute attribute : classData.getAttributes()) {
                        if (attribute instanceof RuntimeVisibleAnnotations) {
                            for (final AnnotationEntry annotation : ((Annotations) attribute).getAnnotationEntries()) {
                                if (isServiceProvider(annotation)) {
                                    findServiceProviders(classData, serviceProviderApi, annotation);
                                } else if (isComponent(annotation) && isAutomatic(annotation)) {
                                    component = true;
                                    componentApi = getApiClass(null, annotation);
                                    fallback = isFallback(annotation);
                                }
                            }
                        }
                    }

                    // the ServiceProvider annotation is inherited so we must check all implemented interfaces
                    // and superclasses to find them
                    for (final JavaClass cls : classData.getAllInterfaces()) {
                        for (final Attribute attribute : cls.getAttributes()) {
                            if (attribute instanceof RuntimeVisibleAnnotations) {
                                final Annotations annotations = (Annotations) attribute;
                                for (final AnnotationEntry annotation : annotations.getAnnotationEntries()) {
                                    if (isServiceProvider(annotation)) {
                                        findServiceProviders(cls, serviceProviderApi, annotation);
                                    }
                                }
                            }
                        }
                    }

                    for (final JavaClass cls : classData.getSuperClasses()) {
                        for (final Attribute attribute : cls.getAttributes()) {
                            if (attribute instanceof RuntimeVisibleAnnotations) {
                                final Annotations annotations = (Annotations) attribute;
                                for (final AnnotationEntry annotation : annotations.getAnnotationEntries()) {
                                    if (isServiceProvider(annotation)) {
                                        findServiceProviders(cls, serviceProviderApi, annotation);
                                    }
                                }
                            }
                        }
                    }

                    if (!serviceProviderApi.isEmpty()) {
                        foundServiceProvider(serviceProviderMap, serviceProviderApi, className);
                    }

                    if (serviceProviderApi.size() > 1 && !component) {
                        component = true;
                        fallback = true;
                    }

                    if (component) {
                        if (componentApi == null) {
                            componentApi = fallback ? className : findComponentInterface(classData);
                        }

                        if (componentApi != null) {
                            foundComponent(generatedBindings, serviceProviderMap, componentMap, className,
                                componentApi);
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

    private void findServiceProviders(final JavaClass classData,
                                      final Set<String> serviceProviderApi,
                                      final AnnotationEntry annotation) throws ClassNotFoundException {
        final String api = getApiClass(classData, annotation);

        if (api != null) {
            serviceProviderApi.add(api);
        } else {
            final Collection<String> apis = getAnnotationAttribute(ATR_VALUE, ATR_API, annotation);
            if (apis != null && apis.size() > 0) {
                serviceProviderApi.addAll(apis);
            } else {
                log.warn("No service provider interface could be identified for " + classData.getClassName());
            }
        }
    }

    private boolean isFallback(final AnnotationEntry annotation) {
        final String value = getAnnotationAttribute(ATTR_FALLBACK, annotation);
        return value != null && Boolean.parseBoolean(value);
    }

    private boolean isAutomatic(final AnnotationEntry annotation) {
        final String value = getAnnotationAttribute(ATR_AUTOMATIC, annotation);
        return value == null || Boolean.parseBoolean(value);
    }

    private String getApiClass(final JavaClass classData, final AnnotationEntry annotation)
        throws ClassNotFoundException {
        final String api = getAnnotationAttribute(ATR_API, annotation);
        return api == null ? classData == null ? null : findSingleInterface(classData) : api;
    }

    private boolean isComponent(final AnnotationEntry annotation) {
        return ATN_COMPONENT.equals(annotation.getAnnotationType());
    }

    private boolean isServiceProvider(final AnnotationEntry annotation) {
        return ATN_SERVICE_PROVIDER.equals(annotation.getAnnotationType()) || ATN_SERVICE_PROVIDER_LIST
            .equals(annotation.getAnnotationType());
    }

    private String findComponentInterface(final JavaClass classData) throws ClassNotFoundException {
        final String foundInterface = findSingleInterface(classData);
        return foundInterface == null ? classData.getClassName() : foundInterface;
    }

    private String findSingleInterface(final JavaClass classData) throws ClassNotFoundException {
        JavaClass[] interfaces = classData.getInterfaces();

        if (interfaces.length == 1) {
            return interfaces[0].getClassName();
        } else if (classData.isInterface()) {
            return classData.getClassName();
        } else {
            JavaClass[] classes = classData.getSuperClasses();
            for (final JavaClass parent : classes) {
                interfaces = parent.getInterfaces();
                if (interfaces.length == 1) {
                    return interfaces[0].getClassName();
                }
            }
        }

        return null;
    }

    private String getAnnotationAttribute(final String name, final AnnotationEntry annotation) {
        for (final ElementValuePair setting : annotation.getElementValuePairs()) {
            if (name.equals(setting.getNameString())) {
                final ElementValue value = setting.getValue();
                final String answer = String.valueOf(value);

                return value.getElementValueType() == ElementValue.CLASS
                    ? answer.substring(1, answer.length() - 1).replace('/', '.')
                    : answer;
            }
        }

        return null;
    }

    private Collection<String> getAnnotationAttribute(final String name,
                                                      final String nested,
                                                      final AnnotationEntry annotation) {
        final List<String> values = new ArrayList<String>();

        for (final ElementValuePair setting : annotation.getElementValuePairs()) {
            if (name.equals(setting.getNameString())) {
                final ElementValue list = setting.getValue();

                if (list.getElementValueType() == ElementValue.ARRAY) {
                    for (final ElementValue value : ((ArrayElementValue) list).getElementValuesArray()) {
                        if (value.getElementValueType() == ElementValue.ANNOTATION) {
                            values.add(
                                getAnnotationAttribute(nested, ((AnnotationElementValue) value).getAnnotationEntry()));
                        }
                    }
                }
            }
        }

        return values;
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

    private String getProjectNameId() {
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

    private void foundServiceProvider(final Map<String, Collection<String>> serviceProviderMap,
                                      final Collection<String> providerNames,
                                      final String className) throws MojoExecutionException {
        for (final String providerName : providerNames) {
            Collection<String> list = serviceProviderMap.get(providerName);

            if (list == null) {
                serviceProviderMap.put(providerName, list = new HashSet<String>());
            }

            if (list.contains(className)) {
                throw new MojoExecutionException("Duplicate service provider class " + className);
            }

            list.add(className);
        }
    }
}
