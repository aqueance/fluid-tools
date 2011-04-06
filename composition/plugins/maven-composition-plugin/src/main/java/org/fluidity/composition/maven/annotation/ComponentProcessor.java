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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Processes a {@link org.fluidity.composition.Component} annotation.
 *
 * @author Tibor Varga
 */
public final class ComponentProcessor extends EmptyVisitor {

    private static final String ATR_AUTOMATIC = "automatic";

    private final ProcessorCallback<ComponentProcessor> callback;

    private boolean automatic = true;

    public ComponentProcessor(final ProcessorCallback<ComponentProcessor> callback) {
        this.callback = callback;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return null;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
        return null;
    }

    @Override
    public AnnotationVisitor visitArray(final String desc) {
        return null;
    }

    @Override
    public void visit(final String name, final Object value) {
        if (ATR_AUTOMATIC.equals(name) && !((Boolean) value)) {
            automatic = false;
        }
    }

    public boolean isAutomatic() {
        return automatic;
    }

    @Override
    public void visitEnd() {
        callback.complete(this);
    }
}
