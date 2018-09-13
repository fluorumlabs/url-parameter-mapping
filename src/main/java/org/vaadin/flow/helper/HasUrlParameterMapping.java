package org.vaadin.flow.helper;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.WildcardParameter;
import org.vaadin.flow.helper.internal.UrlParameterMappingHelper;

/**
 * Helper interface that enables use of {@link UrlParameterMapping} annotation. This interface
 * uses {@link WildcardParameter} parameter for matching. See {@link HasAbsoluteUrlParameterMapping}
 * if you need to perform matching of absolute path.
 * <p>
 * Interface expects you to have implemented setters for all mapped properties. The implementation
 * first sets all matched properties to their corresponding values and then clears all not matched
 * to <tt>null</tt>. The order in which those properties are set and cleared is undefined.
 * <p>
 * You don't need to override {@link #setParameter(BeforeEvent, String)} method, the default
 * implementation takes care of everything.
 *
 * @author Artem Godin
 * @see UrlParameterMapping
 */
public interface HasUrlParameterMapping extends HasUrlParameter<String> {

    /**
     * Method that is called automatically when navigating to the target that
     * implements this interface.
     *
     * @param event     the navigation event that caused the call to this method
     * @param parameter
     */
    @Override
    default void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        UrlParameterMappingHelper.match(event,this, parameter);
    }

    /**
     * Retrieve matched pattern (if any). This allows for pattern-based branching in
     * BeforeEnter observer:
     * <p>
     * <pre><code>
     * &#064;Route(...)
     * &#064;UrlParameterMapping(SomeView.ORDER_VIEW)
     * &#064;UrlParameterMapping(SomeView.ORDER_EDIT)
     * class SomeView extends Div implements HasUrlParameterMapping {
     *     final static String ORDER_VIEW = ":orderId[/view]";
     *     final static String ORDER_EDIT = ":orderId/edit";
     *
     *     public void setOrderId(Integer orderId) { ... }
     *
     *     &#064;Override
     *     public void beforeEnter(BeforeEnterEvent event) {
     *         if ( ORDER_EDIT.equals(getMatchedPattern()) ) {
     *             ...
     *         } else {
     *             ...
     *         }
     *     }
     *
     *     ...
     * }
     * </code></pre>
     *
     * @return matched pattern (as specified in {@link UrlParameterMapping} annotation) or <tt>null</tt> if nothing matches
     */
    default String getMatchedPattern() {
        return UrlParameterMappingHelper.getMatchedPattern(this);
    }

    /**
     * Check if specified pattern matched request.
     * <pre><code>
     * &#064;Route(...)
     * &#064;UrlParameterMapping(SomeView.ORDER_VIEW)
     * &#064;UrlParameterMapping(SomeView.ORDER_EDIT)
     * class SomeView extends Div implements HasUrlParameterMapping {
     *     final static String ORDER_VIEW = ":orderId[/view]";
     *     final static String ORDER_EDIT = ":orderId/edit";
     *
     *     public void setOrderId(Integer orderId) { ... }
     *
     *     &#064;Override
     *     public void beforeEnter(BeforeEnterEvent event) {
     *         if ( isPatternMatched(ORDER_EDIT) ) {
     *             ...
     *         } else {
     *             ...
     *         }
     *     }
     *
     *     ...
     * }
     * </code></pre>
     *
     * @param pattern pattern, as specified in {@link UrlParameterMapping}
     * @return <tt>true</tt> if pattern match request, <tt>false</tt> otherwise
     */
    default boolean isPatternMatched(String pattern) {
        return pattern.equals(getMatchedPattern());
    }

    /**
     * Check if any {@link UrlParameterMapping} pattern matched request.
     * <p>
     * <pre><code>
     * &#064;Route(...)
     * &#064;UrlParameterMapping(":orderId")
     * class SomeView extends Div implements HasUrlParameterMapping {
     *     public void setOrderId(Integer orderId) { ... }
     *
     *     &#064;Override
     *     public void beforeEnter(BeforeEnterEvent event) {
     *         if ( !isPatternMatched() ) {
     *             event.rerouteToError(NotFoundException.class);
     *             return;
     *         }
     *         ...
     *     }
     *
     *     ...
     * }
     * </code></pre>
     *
     * @return <tt>true</tt> if there was match, <tt>false</tt> otherwise
     */
    default boolean isPatternMatched() {
        return getMatchedPattern() != null;
    }
}
