package org.vaadin.flow.helper;

import java.lang.annotation.*;

/**
 * Do not reroute to error view if no matches were found. This effectively cancels
 * the effect of {@link RerouteIfNotMatched}. It is possible to check if any of
 * patterns matched the request using {@link HasUrlParameterMapping#isPatternMatched()}
 *
 * @author Artem Godin
 * @see RerouteIfNotMatched
 * @see HasUrlParameterMapping
 * @see HasAbsoluteUrlParameterMapping
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface IgnoreIfNotMatched {
}
