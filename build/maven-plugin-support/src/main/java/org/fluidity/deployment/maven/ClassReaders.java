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

package org.fluidity.deployment.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Class related convenience methods.
 *
 * @author Tibor Varga
 */
public final class ClassReaders extends Utility implements Opcodes {

    private ClassReaders() { }

    public static final String CONSTRUCTOR_METHOD_NAME = "<init>";

    public static boolean isInterface(final ClassReader data) {
        return (data.getAccess() & ACC_INTERFACE) != 0;
    }

    public static boolean isAbstract(final ClassReader data) {
        return (data.getAccess() & ACC_ABSTRACT) != 0;
    }

    public static boolean isFinal(final ClassReader data) {
        return (data.getAccess() & ACC_FINAL) != 0;
    }

    public static ClassWriter makePublic(final String className, final ClassReader reader) throws MojoExecutionException {
        assert reader != null : className;
        final ClassWriter writer = new ClassWriter(0);

        final AtomicBoolean constructorFound = new AtomicBoolean(false);

        // make class and its default constructor public
        reader.accept(new ClassVisitor(ASM5, writer) {

            @Override
            public void visit(final int version,
                              final int access,
                              final String name,
                              final String signature,
                              final String superName,
                              final String[] interfaces) {
                super.visit(version, access & ~ACC_PRIVATE & ~ACC_PROTECTED | ACC_PUBLIC, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
                final boolean defaultConstructor = Objects.equals(name, CONSTRUCTOR_METHOD_NAME) && Type.getArgumentTypes(desc).length == 0;

                if (defaultConstructor) {
                    constructorFound.set(true);
                }

                return super.visitMethod(defaultConstructor ? access & ~ACC_PRIVATE & ~ACC_PROTECTED | ACC_PUBLIC : access, name, desc, signature, exceptions);
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
            return Collections.emptySet();
        }

        final String[] interfaces = classData.getInterfaces();

        if (interfaces.length > 0) {
            final Set<String> names = new HashSet<>();

            for (final String type : interfaces) {
                names.add(ClassReaders.externalName(type));
            }

            return names;
        } else {
            return findDirectInterfaces(repository.reader(classData.getSuperName()), repository);
        }
    }

    public static Set<String> findInterfaces(final ClassReader classData, final ClassRepository repository) throws IOException {
        return findInterfaces(classData, repository, new HashSet<>());
    }

    private static Set<String> findInterfaces(final ClassReader classData, final ClassRepository repository, final Set<String> list) throws IOException {
        if (classData != null) {
            final Set<String> interfaces = findDirectInterfaces(classData, repository);

            list.addAll(interfaces);

            for (final String api : interfaces) {
                findInterfaces(repository.reader(api), repository, list);
            }

            findInterfaces(repository.reader(classData.getSuperName()), repository, list);
        }

        return list;
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
