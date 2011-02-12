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

import org.fluidity.composition.maven.ClassReaders;
import org.fluidity.composition.maven.ClassRepository;
import org.fluidity.foundation.Exceptions;

import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Processes a {@link org.fluidity.composition.ComponentGroup} annotation.
 *
 * @author Tibor Varga
 */
public final class ComponentGroupProcessor extends EmptyVisitor {

    private static final String ATR_API = "api";

    private final ProcessorCallback<ComponentGroupProcessor> callback;
    private final ClassRepository repository;

    private final ClassReader classData;
    private final Set<String> apiSet = new HashSet<String>();

    public ComponentGroupProcessor(final ClassRepository repository, final ClassReader classData, final ProcessorCallback<ComponentGroupProcessor> callback) {
        this.repository = repository;
        this.classData = classData;
        this.callback = callback;
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

    public Set<String> apiSet() {
        return apiSet;
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
                    throw new MojoExecutionException(String.format("No component group interface could be identified for %s", className));
                }

                callback.complete(ComponentGroupProcessor.this);
                return null;
            }
        });
    }
}
