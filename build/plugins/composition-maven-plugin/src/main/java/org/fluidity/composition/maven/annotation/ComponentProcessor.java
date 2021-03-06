/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.util.Objects;
import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Processes a {@link org.fluidity.composition.Component @Component} annotation.
 *
 * @author Tibor Varga
 */
public final class ComponentProcessor extends AnnotationVisitor {

    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    private static final String ATR_AUTOMATIC = "automatic";
    private static final String ATR_SCOPE = "scope";

    private final Consumer<ComponentProcessor> callback;

    private Type scope = OBJECT_TYPE;
    private boolean automatic = true;

    public ComponentProcessor(final Consumer<ComponentProcessor> callback) {
        super(Opcodes.ASM5);
        this.callback = callback;
    }

    @Override
    public void visit(final String name, final Object value) {
        if (Objects.equals(name, ATR_AUTOMATIC) && !((Boolean) value)) {
            automatic = false;
        } else if (Objects.equals(name, ATR_SCOPE)) {
            scope = (Type) value;
        }
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public Type scope() {
        return Objects.equals(scope, OBJECT_TYPE) ? null : scope;
    }

    @Override
    public void visitEnd() {
        callback.accept(this);
    }
}
