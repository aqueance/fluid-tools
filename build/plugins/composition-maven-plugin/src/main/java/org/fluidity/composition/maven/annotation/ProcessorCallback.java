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

import org.objectweb.asm.AnnotationVisitor;

/**
 * Called when an annotation has been fully processed, to extract data collected during the annotation processing.
 *
 * @param <T> the type of the annotation visitor to call.
 *
 * @author Tibor Varga
 */
@FunctionalInterface
@SuppressWarnings("WeakerAccess")
public interface ProcessorCallback<T extends AnnotationVisitor> {

    void complete(T visitor);
}
