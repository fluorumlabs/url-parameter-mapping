package org.vaadin.flow.helper;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.WildcardParameter;

/**
 * Helper interface that enables use of {@link UrlParameterMapping} annotation. This interface
 * uses {@link WildcardParameter} parameter for matching. See {@link HasAbsoluteUrlParameterMapping}
 * if you need to perform matching of absolute path.
 * <p>
 * Interface expects you to have implemented setters for all mapped properties. The implementation
 * first sets all matched properties to their corresponding values and then clears all not matched
 * to {@code null}. The order in which those properties are set and cleared is undefined.
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
        UrlParameterMappingHelper.match(this, parameter);
    }

    /**
     * Retrieve matched pattern (if any). This allows for pattern-based branching in
     * BeforeEnter observer:
     * <p>
     * <pre><code>
     *     @Route(...)
     *     @UrlParameterMapping(SomeView.ORDER_VIEW)
     *     @UrlParameterMapping(SomeView.ORDER_EDIT)
     *     class SomeView extends Div implements HasUrlParameterMapping {
     *         final static String ORDER_VIEW = ":orderId[/view]";
     *         final static String ORDER_EDIT = ":orderId/edit";
     *
     *         public void setOrderId(Integer orderId) { ... }
     *
     *         @Override
     *         public void beforeEnter(BeforeEnterEvent event) {
     *             if ( ORDER_EDIT.equals(getMatchedPattern() ) {
     *                 ...
     *             } else {
     *                 ...
     *             }
     *         }
     *
     *         ...
     *     }
     * </code></pre>
     *
     * @return matched pattern (as specified in {@link UrlParameterMapping} annotation) or {@code null} if nothing matches
     */
    default String getMatchedPattern() {
        return UrlParameterMappingHelper.getMatchedPattern(this);
    }
}
