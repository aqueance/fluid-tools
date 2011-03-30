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

package org.fluidity.composition.maven.annotation;

import java.util.HashSet;
import java.util.Set;

import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.maven.ClassReaders;
import org.fluidity.composition.maven.ClassRepository;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Methods;

import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Processes a {@link org.fluidity.composition.ServiceProvider} annotation.
 *
 * @author Tibor Varga
 */
public final class ServiceProviderProcessor extends EmptyVisitor {

    private static final String ATR_API = "api";
    private static final String ATR_TYPE = "type";

    private final ProcessorCallback<ServiceProviderProcessor> callback;
    private final ClassRepository repository;

    private final ClassReader classData;
    private final Set<String> apiSet = new HashSet<String>();
    private final String defaultType;
    private String type;

    public ServiceProviderProcessor(final ClassRepository repository, final ClassReader classData, final ProcessorCallback<ServiceProviderProcessor> callback) {
        this.repository = repository;
        this.classData = classData;
        this.callback = callback;

        this.defaultType = (String) Methods.get(ServiceProvider.class, new Methods.Invoker<ServiceProvider>() {
            public void invoke(final ServiceProvider dummy) {
                dummy.type();
            }
        }).getDefaultValue();

        this.type = this.defaultType;
        assert type != null;
    }

    @Override
    public void visit(final String name, final Object value) {
        assert ATR_TYPE.equals(name) : name;

        if (ATR_TYPE.equals(name)) {
            type = (String) value;
        }
    }

    @Override
    public AnnotationVisitor visitArray(final String name) {
        assert ATR_API.equals(name) : name;

        return new EmptyVisitor() {

            @Override
            public void visit(final String ignore, final Object value) {
                assert ignore == null : ignore;
                apiSet.add(((Type) value).getClassName());
            }
        };
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return null;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
        return null;
    }

    public Set<String> apiSet() {
        return apiSet;
    }

    public String type() {
        return type;
    }

    @Override
    public void visitEnd() {
        final String className = ClassReaders.externalName(classData);

        Exceptions.wrap(new Exceptions.Command<Void>() {
            public Void run() throws Exception {
                if (apiSet.isEmpty()) {
                    if (ClassReaders.isInterface(classData)) {
                        apiSet.add(className);
                    }
                }

                if (apiSet.isEmpty()) {
                    apiSet.addAll(ClassReaders.findDirectInterfaces(classData, repository));
                }

                if (apiSet.isEmpty() && !ClassReaders.isFinal(classData)) {
                    apiSet.add(className);
                }

                if (apiSet.isEmpty()) {
                    throw new MojoExecutionException(String.format("No service provider interface could be identified for %s", className));
                }

                callback.complete(ServiceProviderProcessor.this);
                return null;
            }
        });
    }

    public boolean isDefaultType() {
        return type.equals(defaultType);
    }

}
