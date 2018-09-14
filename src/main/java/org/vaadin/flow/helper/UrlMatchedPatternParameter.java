package org.vaadin.flow.helper;

import java.lang.annotation.*;

/**
 * Mark field or setter method to receive matching {@link UrlParameterMapping} pattern.
 *
 * @author Artem Godin
 * @see IgnoreIfNotMatched
 * @see UrlParameter
 * @see UrlParameterMapping
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Inherited
@Documented
public @interface UrlMatchedPatternParameter {
}
