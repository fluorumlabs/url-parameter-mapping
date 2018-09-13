package org.vaadin.flow.helper;

import java.lang.annotation.*;

/**
 * Mark field or setter method for use in {@link UrlParameterMapping}.
 * <p>
 * Example use:
 * <pre><code>
 *     @UrlParameter(name = "user", regEx = "[0-9]{6}")
 *     public Integer userId;
 *
 *     @UrlParameter
 *     public setProduct(String product) { ... }
 * </code></pre>
 * <p>
 * If {@code name} is not specified, the field name is used, or, in case of setter method,
 * the corresponding property name (i.e. "product" for "setProduct").
 * <p>
 * Optional {@code regEx} allows to specify custom regular expressions to be used for capturing
 * parameters. If {@code regEx} is not specified, the default regular expression based on parameter
 * type is used.
 * <p>
 * Fields and methods to be used as parameters should be declared public.
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
}
