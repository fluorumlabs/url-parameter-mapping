package org.vaadin.flow.helper;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.NotFoundException;

import java.lang.annotation.*;

/**
 * Define a <tt>rerouteTo</tt> view/<tt>rerouteToError</tt> exception, which will be used if no matches
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
@Inherited
public @interface RerouteIfNotMatched {
    /**
     * Gets the redirection view of the annotated class.
     *
     * @return view to which <tt>event.rerouteTo</tt> should be called
     */
    Class<? extends Component> view() default NoView.class;

    /**
     * Gets the defined exception of the annotated class.
     *
     * @return exception with which <tt>event.rerouteToError</tt> should be called
     */
    Class<? extends Exception> exception() default NotFoundException.class;

    /**
     * Dummy component class for use as {@link RerouteIfNotMatched#view()} value.
     */
    abstract class NoView extends Component {
    }

    /**
     * Dummy exception class for use as {@link RerouteIfNotMatched#exception()} value.
     */
    abstract class NoException extends Exception {
    }
}
