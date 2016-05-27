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

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * Specifies the <a href="https://github.com/aqueance/fluid-tools/wiki/User-Guide---Introduction#component-interface">component interface(s)</a> that a class implements, which should
 * resolve to the annotated class at run time by a dependency injection container.
 * <p>
 * This annotation is also a marker for the <code>org.fluidity.maven:composition-maven-plugin</code> Maven plugin to make configuration-free dependency
 * injection possible.
 * <p>
 * For any class marked with this annotation and without having automatic processing disabled, i.e., without {@link #automatic() @Component(automatic =
 * false)}, the <code>composition-maven-plugin</code> Maven plugin will generate at build time the necessary metadata that the dependency injection container
 * will need to pick up and process the annotated class at run time.
 * <h3>Usage</h3>
 * <pre>
 * <span class="hl1">&#64;Component</span>
 * public final class MyComponent {
 *
 *   MyComponent(final <span class="hl2">MyDependency</span> dependency) {
 *     &hellip;
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 * <pre>
 * <span class="hl1">&#64;Component</span>
 * final class MyDependencyImpl implements <span class="hl2">MyDependency</span> {
 *   &hellip;
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Qualifier(Qualifier.Composition.NONE)
public @interface Component {

    /**
     * The service provider type for scoped components. Used internally.
     */
    String SCOPE = "scope";

    /**
     * Specifies the interfaces or classes that should resolve to the annotated class at run time.
     * <p>
     * In case of {@link org.fluidity.composition.spi.ComponentFactory ComponentFactory}, the value applies to the component the factory creates, not the
     * factory itself.
     * <p>
     * When omitted, the property defaults to the list of interfaces determined by the algorithm documented at {@link org.fluidity.composition.Components}.
     *
     * @return an array of class objects.
     */
    @SuppressWarnings("JavadocReference")
    Class<?>[] api() default { };

    /**
     * When not specified or set to <code>true</code>, this parameter specifies that the annotated class can be automatically added to a global container.
     * <p>
     * Setting this property to <code>false</code> allows the class to be added to a container programmatically, or to merely to define a list of interfaces
     * for another class to pick up, such as a {@link org.fluidity.composition.spi.ComponentFactory ComponentFactory} implementation.
     * <p>
     * Programmatic handling of the component entails either:<ul>
     *     <li>explicitly {@link ComponentContainer.Registry#bindComponent(Class, Class[]) binding} the class to some
     *     {@link ComponentContainer#makeChildContainer(ComponentContainer.Bindings...) local container},</li>
     *     <li>explicitly {@link ComponentContainer#instantiate(Class, ComponentContainer.Bindings...) instantiating} the component,</li>
     *     <li>having the annotation's {@link #scope() scope} parameter set to a component interface to which the foregoing applies,</li>
     *     <li>having another component's {@link #api() api} parameter refer to the annotated class, or</li>
     *     <li>provision of a suitable {@link org.fluidity.composition.spi.PackageBindings PackageBindings} object that adds the component to a global
     *     container.</li>
     * </ul>
     *
     * @return <code>true</code> if this component should be automatically processed.
     *
     * @see org.fluidity.composition.spi.EmptyPackageBindings
     */
    boolean automatic() default true;

    /**
     * Tells whether the annotated class is a primary implementation of its component interface(s) or just a fallback or default implementation. As a fallback
     * it will be used when no other class has been marked for the same component interface as a primary. This parameter defaults to <code>true</code>, which
     * means the annotated component is the primary implementation of its component interfaces.
     *
     * @return <code>true</code> if this component should be bound as a primary implementation of its component interfaces.
     */
    boolean primary() default true;

    /**
     * Tells whether the annotated component should be singleton or a new instance must be created for every query or reference. This parameter defaults to
     * <code>false</code>, which means that the annotated component is a singleton (or semi-singleton in case of context dependent components).
     *
     * @return <code>true</code> if a new instance should be created for every query or dependency reference.
     */
    boolean stateful() default false;

    /**
     * Specifies that this component is scoped to a component with the given component interface, as opposed to being globally accessible. Setting
     * this parameter implies <code>{@link #automatic automatic} = false</code>.
     * <p>
     * Unless it itself implements the component interface it is scoped to, the annotated component is <em>not</em> supposed to be bound in any way by the
     * developer; it is automatically bound when the given component interface is either
     * {@link ComponentContainer.Registry#bindComponent(Class, Class[]) bound} in a
     * {@link ComponentContainer#makeChildContainer(ComponentContainer.Bindings...) local container} or directly
     * {@link ComponentContainer#instantiate(Class, ComponentContainer.Bindings...) instantiated}.
     * <p>
     * When any of the foregoing occurs, Fluid Tools automatically binds every component that has this parameter set to the given component interface.
     * This makes it possible to have an isolated sub-graph of dependency injected components for every binding / instance of the given component interface.
     *
     * @return a component interface class.
     */
    Class<?> scope() default Object.class;

    /**
     * Lists the <a href="https://github.com/aqueance/fluid-tools/wiki/User-Guide---Overview#component-context">context</a> {@link Qualifier qualifiers}
     * accepted by the annotated component class. These annotations distinguish instances of the same component class from one another. Such a annotation could,
     * for instance, specify a database identifier for a database access component, etc. The component receives, as a {@link ComponentContext} argument of its
     * constructor, the instances of its accepted annotations prevalent at the point of reference to the component.
     * <p>
     * A special context qualifier is the parameterized type of the dependency reference to the context dependent component, {@link
     * Component.Reference @Component.Reference}.
     * <p>
     * When the {@link #ignore()} parameter is present, it causes all definitions, up to but not including the annotated entity, of the specified context
     * qualifiers to be ignored by the annotated entity.
     * <h3>Usage</h3>
     * <pre>
     * {@linkplain Component @Component}
     * <span class="hl2">&#64;MyQualifier</span>("1")
     * public final class MyComponent {
     *
     *   MyComponent(final <span class="hl2">&#64;MyQualifier</span>("2") <span class="hl3">MyDependency</span> dependency) {
     *     &hellip;
     *   }
     *
     *   &hellip;
     * }
     * </pre>
     * <pre>
     * {@linkplain Component @Component}
     * <span class="hl1">&#64;Component.Qualifiers</span>(<span class="hl2">MyQualifier</span>.class)
     * final class MyDependencyImpl implements <span class="hl3">MyDependency</span> {
     *
     *   MyDependencyImpl(final {@linkplain ComponentContext} context) {
     *     final <span class="hl2">MyQualifier</span>[] annotations = context.qualifiers(<span class="hl2">MyQualifier</span>.class);
     *     &hellip;
     *   }
     *
     *   &hellip;
     * }
     * </pre>
     *
     * @author Tibor Varga
     */
    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    @Qualifier(Qualifier.Composition.NONE)
    @interface Qualifiers {

        /**
         * Lists the qualifiers that configure the annotated class.
         * <p>
         * This parameter is used only at class level.
         *
         * @return a list of annotation classes.
         */
        Class<? extends Annotation>[] value() default { };

        /**
         * Specifies what qualifiers should be ignored up to this point in the instantiation path. This parameter can be used when annotating fields
         * and constructor parameters as well as classes.
         *
         * @return an array of annotation classes to ignore.
         */
        Class<? extends Annotation>[] ignore() default {};
    }

    /**
     * {@link Qualifier Qualifier} <a
     * href="https://github.com/aqueance/fluid-tools/wiki/User-Guide---Composition#working-with-component-contexts">annotation</a> that captures the
     * parameterized type of a component reference. This is not an actual annotation from the Java syntax point of view &ndash; a Java annotation would not
     * allow <code>Type</code> as the type of a parameter &ndash; but simply a run-time representation of an annotation to convey type information.
     * <h3>Usage</h3>
     * <pre>
     * {@linkplain Component @Component}
     * {@linkplain Qualifiers @Component.Qualifiers}(<span class="hl1">Component.Reference</span>.class)
     * final class <span class="hl2">MyComponent</span>&lt;T&gt; {
     *
     *   &#47;**
     *    * The raw value of the type parameter T.
     *    *&#47;
     *   private final Class&lt;?&gt; type;
     *
     *   MyComponent(final {@linkplain ComponentContext} context) {
     *     final <span class="hl1">Component.Reference</span> reference = context.qualifier(<span class="hl1">Component.Reference</span>.class, null);
     *     this.type = reference.parameter(0);
     *     &hellip;
     *   }
     *
     *   public Class&lt;?&gt; <span class="hl2">type()</span> {
     *     return type;
     *   }
     * }
     * </pre>
     * <pre>
     * {@linkplain Component @Component}
     * final class MyReferrer {
     *
     *   MyReferrer(final <span class="hl2">MyComponent</span><span class="hl3">&lt;MyType&gt;</span> dependency) {
     *     assert dependency.<span class="hl2">type()</span> == <span class="hl3">MyType</span>.class : <span class="hl2">MyComponent</span>.class;
     *     &hellip;
     *   }
     * }
     * </pre>
     *
     * @author Tibor Varga
     */
    @Qualifier(Qualifier.Composition.IMMEDIATE)
    interface Reference extends Annotation {

        /**
         * Returns the parameterized type of a component reference.
         *
         * @return a type object.
         */
        Type type();

        /**
         * Convenience method to return the raw type of the type parameter at the given index, of the component reference.
         *
         * @param index the index of the type parameter.
         *
         * @return the raw type of the type parameter at the given index or <code>null</code> if no such parameter is present.
         */
        Class<?> parameter(int index);
    }
}
