/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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
