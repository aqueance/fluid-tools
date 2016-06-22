/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.function.Consumer;

import org.fluidity.deployment.maven.ClassReaders;
import org.fluidity.deployment.maven.ClassRepository;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.ServiceProviders;

import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Processes a {@link org.fluidity.composition.ServiceProvider @ServiceProvider} annotation.
 *
 * @author Tibor Varga
 */
public final class ServiceProviderProcessor extends AnnotationVisitor {

    private static final String ATR_API = "api";
    private static final String ATR_TYPE = "type";

    private final Consumer<ServiceProviderProcessor> callback;
    private final ClassRepository repository;

    private final ClassReader classData;
    private final Set<String> apiSet = new HashSet<>();
    private String type;

    public ServiceProviderProcessor(final ClassRepository repository, final ClassReader classData, final Consumer<ServiceProviderProcessor> callback) {
        super(Opcodes.ASM5);
        this.repository = repository;
        this.classData = classData;
        this.callback = callback;
        this.type = ServiceProviders.TYPE;
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

        return new AnnotationVisitor(api) {
            @Override
            public void visit(final String ignore, final Object value) {
                assert ignore == null : ignore;
                apiSet.add(((Type) value).getClassName());
            }
        };
    }

    public Set<String> apiSet() {
        return apiSet;
    }

    public String type() {
        return type;
    }

    @Override
    public void visitEnd() {
        Exceptions.wrap(() -> {
            final String className = ClassReaders.externalName(classData);

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

            callback.accept(ServiceProviderProcessor.this);

            return null;
        });
    }
}
