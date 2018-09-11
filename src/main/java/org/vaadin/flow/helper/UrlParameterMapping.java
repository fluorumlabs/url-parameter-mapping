package org.vaadin.flow.helper;

import java.lang.annotation.*;

/**
 * Define a path/parameter pattern with property mappings. Usage:
 * <pre><code>@UrlParameterMapping("segment/:parameter:regex:[/:optional_parameter]")</code></pre>
 * Pattern can have one or several terms. Patterns are always expected to match entire path.
 *
 * If multiple patterns are matching, the one with the maximum number of properties is used.
 *
 * The following term types are available:
 *
 * <strong>Static segment: <pre>segment</pre></strong>
 *
 * Example: <pre>"test"</pre> will match only <pre>/test</pre>. It will not match <pre>/testing</pre>.
 *
 * <strong>Parameter mapping: <pre>:parameter:regex:</pre></strong>
 * Parameter is the propery name that will receive the value if matched or {@code null} otherwise. Only
 * {@code String}, {@code Long}, {@code Integer} and {@code Boolean} properties are supported. Regex is
 * a normal Java {@link java.util.regex.Pattern} regular expression, that can be used to specify
 * the expected parameter format.
 *
 * Example 1: <pre>"entry/:id"</pre> will match <pre>/entry/12345</pre> and will not match
 * <pre>/entry/12345/edit</pre> or <pre>/entry/12345a</pre>. Integer property {@code id} will receive the value of {@code "12345"}.
 *
 * Example 2: <pre>"blog/:title</pre> will match <pre>/blog/random-title</pre> and will not match
 * <pre>/blog/random-title/with/segments</pre>. String property {@code title} will receive the value of {@code "random-title"}.
 *
 * Example 3: <pre>"blog/:title:.*:</pre> will match <pre>/blog/random-title</pre> and <pre>/blog/random-title/with/segments</pre>.
 * String property {@code title} will receive values of {@code "random-title"} and {@code "random-title/with/segments"} correspondingly.
 *
 * It is possible to define optional parameters using <pre>[/...]</pre> syntax:
 *
 * Example: <pre>"thread/:id[/:message]"</pre> will match:
 * <ul>
 * <li><pre>/thread/12345</pre>:
 * Integer {@code id} will receive value of {@code "12345"},
 * Integer {@code message} will receive value of {@code null}</li>
 * <li><pre>/thread/12345/6789</pre>:
 * {@code id} will receive value of {@code "12345"},
 * {@code message} will receive value of {@code "6789"}</li>
 * </ul>
 *
 * @author Artem Godin
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
     * Gets the defined pattern of the annotated class.
     *
     * @return the path value of this route
     */
    String value();

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
