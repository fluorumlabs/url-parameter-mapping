package org.vaadin.flow.helper;

import com.vaadin.flow.router.NotFoundException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a <tt>rerouteToError</tt> exception, which will be used if no matches
 * are detected. Defaults to {@link NotFoundException}
 * <p>
 * Usage:
 * <pre><code>
 * &#064;Route(...)
 * &#064;UrlParameterMapping(SomeView.ORDER_VIEW)
 * &#064;UrlParameterMapping(SomeView.ORDER_EDIT)
 * &#064;RerouteIfNotMatched(NoOrderException.class)
 * </code></pre>
 *
 * @author Artem Godin
 * @see IgnoreIfNotMatched
 * @see UrlParameterMapping
 * @see HasUrlParameterMapping
 * @see HasAbsoluteUrlParameterMapping
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RerouteIfNotMatched {
    /**
     * Gets the defined exception of the annotated class.
     *
     * @return exception with which <tt>event.rerouteToError</tt> should be called
     */
    Class<? extends Exception> value() default NotFoundException.class;
}
