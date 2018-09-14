package org.vaadin.flow.helper;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.WildcardParameter;

/**
 * Helper interface that enables use of {@link UrlParameterMapping} annotation. This interface
 * uses path for matching. See {@link HasUrlParameterMapping} if you need to perform matching
 * of {@link WildcardParameter} parameter.
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
public interface HasAbsoluteUrlParameterMapping extends HasUrlParameterMapping {

    /**
     * Method that is called automatically when navigating to the target that
     * implements this interface.
     *
     * @param event     the navigation event that caused the call to this method
     * @param parameter
     */
    @Override
    default void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        UrlParameterMappingHelper.matchAndReroute(event, this, event.getLocation().getPath());
    }

}
