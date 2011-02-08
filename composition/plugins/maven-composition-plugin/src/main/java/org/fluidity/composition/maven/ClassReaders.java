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
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.foundation.ClassLoaders;

import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Class related convenience methods.
 */
public final class ClassReaders {

    public static final String CONSTRUCTOR_METHOD_NAME = "<init>";

    public ClassReaders() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    public static boolean isInterface(final ClassReader data) {
        return (data.getAccess() & Opcodes.ACC_INTERFACE) != 0;
    }

    public static boolean isAbstract(final ClassReader data) {
        return (data.getAccess() & Opcodes.ACC_ABSTRACT) != 0;
    }

    public static boolean isFinal(final ClassReader data) {
        return (data.getAccess() & Opcodes.ACC_FINAL) != 0;
    }

    public static ClassWriter makePublic(String className, ClassReader reader) throws MojoExecutionException {
        assert reader != null : className;
        final ClassWriter writer = new ClassWriter(0);

        final AtomicBoolean constructorFound = new AtomicBoolean(false);

        // make class and its default constructor public
        reader.accept(new ClassAdapter(writer) {

            @Override
            public void visit(final int version,
                              final int access,
                              final String name,
                              final String signature,
                              final String superName,
                              final String[] interfaces) {
                super.visit(version, access & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                final boolean defaultConstructor = name.equals(CONSTRUCTOR_METHOD_NAME) && Type.getArgumentTypes(desc).length == 0;

                if (defaultConstructor) {
                    constructorFound.set(true);
                }

                return super.visitMethod(defaultConstructor ? access & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC : access,
                                         name,
                                         desc,
                                         signature,
                                         exceptions);
            }
        }, 0);

        if (!constructorFound.get()) {
            throw new MojoExecutionException(String.format("Class %s does not have a default constructor", className));
        }

        return writer;
    }

    @SuppressWarnings("unchecked")
    public static Set<String> findDirectInterfaces(final ClassReader classData, final ClassRepository repository) throws IOException {
        if (classData == null) {
            return Collections.EMPTY_SET;
        }

        final String[] interfaces = classData.getInterfaces();

        if (interfaces.length > 0) {
            final Set<String> names = new HashSet<String>();

            for (final String type : interfaces) {
                names.add(ClassReaders.externalName(type));
            }

            return names;
        } else {
            return findDirectInterfaces(repository.reader(classData.getSuperName()), repository);
        }
    }

    public static String externalName(final ClassReader reader) {
        return reader.getClassName().replace('/', '.');
    }

    public static String externalName(final String className) {
        return className.replace('/', '.');
    }

    public static String internalName(final String className) {
        return className.replace('.', '/');
    }

    public static String fileName(final String className) {
        return className.replace('.', File.separatorChar).concat(ClassLoaders.CLASS_SUFFIX);
    }
}
