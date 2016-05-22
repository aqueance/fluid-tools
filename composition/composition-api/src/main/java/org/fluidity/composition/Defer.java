package org.fluidity.composition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a dependency for lazy instantiation. If the dependency reference is not to an interface, an {@link IllegalArgumentException} is thrown.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * public final class MyComponent {
 *
 *   MyComponent(final <span class="hl1">&#64;Defer({@linkplain #shared() shared} = false)</span> <span class="hl2">SomeDependency</span> dependency) {
 *     &hellip;
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 * <pre>
 * public <b>interface</b> <span class="hl2">SomeDependency</span> {
 *   &hellip;
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD})
@Component.Qualifiers(compose = Component.Qualifiers.Composition.IMMEDIATE)
public @interface Defer {

    /**
     * Specifies whether the deferred reference should be thread safe or not.
     *
     * @return <code>true</code> if the deferred reference should be thread safe; <code>false</code> otherwise.
     */
    boolean shared() default true;
}
