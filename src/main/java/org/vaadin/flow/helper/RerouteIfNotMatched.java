package org.vaadin.flow.helper;

import com.vaadin.flow.router.NotFoundException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a {@code rerouteToError} exception, which will be used if no matches
 * are detected. Defaults to {@link NotFoundException}
 * <p>
 * Usage:
 * <pre><code>
 *     @Route(...)
 *     @UrlParameterMapping(SomeView.ORDER_VIEW)
 *     @UrlParameterMapping(SomeView.ORDER_EDIT)
 *     @RerouteIfNotMatched
 * </code></pre>
 *
 * @author Artem Godin
 * @see UrlParameterMapping
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RerouteIfNotMatched {
    /**
     * Gets the defined exception of the annotated class.
     *
     * @return exception with which {@code event.rerouteToError} should be called
     */
    Class<? extends Exception> value() default NotFoundException.class;
}
