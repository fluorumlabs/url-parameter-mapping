package org.vaadin.flow.helper;

import java.lang.annotation.*;

/**
 * Define a path/parameter pattern with property mappings. Usage:
 * <code>&#064;UrlParameterMapping("segment/:parameter:regex:[/:optional_parameter]")</code>
 * Pattern can have one or several terms. Patterns are always expected to match entire path.
 * <p>
 * If multiple patterns are matching, the first matched defined pattern is used.
 * <p>
 * The following term types are available:
 * <p>
 * <strong>Static segment: <tt>segment</tt></strong>
 * <p>
 * Example: <tt>"test"</tt> will match only <tt>/test</tt>. It will not match <tt>/testing</tt>.
 * <p>
 * <strong>Parameter mapping: <tt>:parameter</tt></strong>
 * <p>
 * Parameter is field/method annotated with {@link UrlParameter} that will receive the value if matched or <tt>null</tt> otherwise. Only
 * {@link String}, {@link Long}, {@link Integer} and {@link Boolean} properties are supported. It is possible
 * to specify custom regular expressions either with {@link UrlParameter#regEx()} or with extended syntax:
 * <tt>:parameter:regular-expression:</tt>.
 * <p>
 * Example 1: <tt>"entry/:id"</tt> will match <tt>/entry/12345</tt> and will not match
 * <tt>/entry/12345/edit</tt> or <tt>/entry/12345a</tt>. Integer property <tt>id</tt> will receive the value of <tt>12345</tt>.
 * <p>
 * Example 2: <tt>"blog/:title"</tt> will match <tt>/blog/random-title</tt> and will not match
 * <tt>/blog/random-title/with/segments</tt>. String property <tt>title</tt> will receive the value of <tt>"random-title"</tt>.
 * <p>
 * Example 3: <tt>"blog/:title:.*?:"</tt> will match <tt>/blog/random-title</tt> and <tt>/blog/random-title/with/segments</tt>.
 * String property <tt>title</tt> will receive values of <tt>"random-title"</tt> and <tt>"random-title/with/segments"</tt> correspondingly.
 * <p>
 * It is possible to define optional parameters using <tt>[/...]</tt> syntax:
 * <p>
 * Example: <tt>"thread/:id[/:message]"</tt> will match:
 * <ul>
 * <li><tt>/thread/12345</tt>:
 * Integer <tt>id</tt> will receive value of <tt>12345</tt>,
 * Integer <tt>message</tt> will receive value of <tt>null</tt></li>
 * <li><tt>/thread/12345/6789</tt>:
 * <tt>id</tt> will receive value of <tt>12345</tt>,
 * <tt>message</tt> will receive value of <tt>6789</tt></li>
 * </ul>
 * Query parameters also could be matched using extended syntax:
 * <p>
 * <code>&#064;UrlParameterMapping(value = "thread/:id[/:message]", queryParameters = {"mode=:mode:edit|print:"})</code>
 * </p>
 * This will match <tt>/thread/12345?mode=edit</tt> and will set <tt>mode</tt> parameter to corresponding value. If value
 * doesn't match regular expression, it will be <tt>null</tt>. Query parameters are always optional and thus do not trigger
 * redirects when no matches happen. All unknown/unlisted query parameters are ignored. All missing parameters will receive
 * <tt>null</tt> value.
 *
 * @author Artem Godin
 * @see UrlParameter
 * @see HasUrlParameterMapping
 * @see HasAbsoluteUrlParameterMapping
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@Repeatable(UrlParameterMapping.Container.class)
public @interface UrlParameterMapping {
    /**
     * Value for {@link UrlParameterMapping#value()} that matches any path.
     */
    String ANY = ".*?";

    /**
     * Gets the defined pattern of the annotated class. Defaults to empty string.
     *
     * @return the path value of this route
     */
    String value() default "";

    /**
     * Gets the list of expected query parameters.
     *
     * @return
     */
    String[] queryParameters() default {};

    /**
     * Internal annotation to enable use of multiple {@link UrlParameterMapping}
     * annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    @Inherited
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link UrlParameterMapping}
         * annotations.
         *
         * @return an array of the UrlParameterMapping annotations
         */
        UrlParameterMapping[] value();
    }

}
