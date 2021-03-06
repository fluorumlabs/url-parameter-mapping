package org.vaadin.flow.helper;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * Mark field or setter method for use in {@link UrlParameterMapping}.
 * <p>
 * Example use:
 * <pre><code>
 * &#064;UrlParameter(name = "user", regEx = "[0-9]{6}")
 * public Integer userId;
 *
 * &#064;UrlParameter
 * public setProduct(String product) { ... }
 * </code></pre>
 * <p>
 * If <tt>name</tt> is not specified, the field name is used, or, in case of setter method,
 * the corresponding property name (i.e. <tt>"product"</tt> for <tt>"setProduct"</tt>).
 * <p>
 * Optional <tt>regEx</tt> allows to specify custom regular expressions to be used for capturing
 * parameters. If <tt>regEx</tt> is not specified, the default regular expression based on parameter
 * type is used.
 * <p>
 * Fields and methods to be used as parameters should be declared public.
 *
 * Special construct can be used to store the matched pattern:
 * <pre><code>
 * &#064;UrlParameter(type = UrlParameter.Type.MATCHED_PATTERN)
 * String matchedPattern;
 * </code></pre>
 *
 * @author Artem Godin
 * @see UrlParameterMapping
 * @see HasUrlParameterMapping
 * @see HasAbsoluteUrlParameterMapping
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Inherited
@Documented
public @interface UrlParameter {
    /**
     * Name of the parameter (placeholder) in {@link UrlParameterMapping}
     *
     * @return parameter name. If not specified, the parameter name will be based on field or method name.
     */
    String name() default "";

    /**
     * Regular expression used to capture parameter value in {@link UrlParameterMapping}
     *
     * @return regular expression. If not specified, type-based regular expression will be used.
     */
    String regEx() default "";

    /**
     * Indicate that the regular expression for this parameter is computed dynamically. Use
     * {@link UrlParameterMappingHelper#setDynamicRegex(Class, String, Function)} to set the actual producer
     *
     * @return <tt>true</tt> if parameter's regex is dynamically computed
     */
    boolean dynamicRegEx() default false;
}
