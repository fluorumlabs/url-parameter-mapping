package org.vaadin.flow.helper;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.NotFoundException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Internal representation of mappings
 *
 * @author Artem Godin
 * @see UrlParameterMappingHelper
 */
class Mapping {
    /**
     * Exception class to be used when no matches are detected.
     * No <tt>rerouteToError</tt> will be called when <tt>rerouteException == null</tt>
     */
    Class<? extends Exception> rerouteException = NotFoundException.class;
    Class<? extends Component> rerouteView = RerouteIfNotMatched.NoView.class;

    /**
     * Compiled regular expression for all mappings
     */
    Pattern compiledPattern;

    /**
     * Combined regular expression for all mappings
     */
    String pattern;

    /**
     * <tt>true</tt> if this mapping has parameters with dynamically computed regexp
     */
    boolean hasDynamicRegex = false;

    /**
     * Distinct mapping patterns
     */
    final Set<MappingPattern> mappingPatterns = Collections.synchronizedSet(new HashSet<>());

    /**
     * Fields/Methods annotated with {@link UrlParameter}
     */
    final Map<String, Parameter> parameters = new ConcurrentHashMap<>();

    /**
     * Internal representation of single mapping pattern
     */
    static class MappingPattern {
        /**
         * Mapping pattern identifier (name of capture group)
         */
        String id;

        /**
         * Mapping index, used for natural ordering of matches
         */
        int index;

        /**
         * Set of parameter names/placeholders
         */
        Set<String> parameters;

        /**
         * Original pattern from {@link UrlParameterMapping}
         */
        String pattern;
    }

    /**
     * Internal representatin of {@link UrlParameter} annotated fields/methods
     */
    static class Parameter {
        /**
         * Value type
         */
        private Class<?> type;

        /**
         * Field holding a value
         */
        private Field field;

        /**
         * Setter method used to set a value
         */
        private Method setter;

        /**
         * <tt>true</tt> if parameter regular expression needs to be dynamically computed
         */
        boolean dynamic;

        /**
         * Producer for dynamic regular expressions
         */
        Function<Object, String> regexProducer;

        /**
         * Helper value for calling {@link Method#invoke(Object, Object...)} with <tt>null</tt> argument
         */
        private static final Object[] NULL_ARGUMENT = new Object[]{null};

        /**
         * Construct parameter with specified field
         *
         * @param field field
         * @param dynamic <tt>true</tt> if regexp is dynamically computed
         */
        Parameter(Field field, boolean dynamic) {
            this.type = field.getType();
            this.field = field;
            this.setter = null;
            this.dynamic = dynamic;
        }

        /**
         * Construct parameter with specified setter method
         *
         * @param setter setter method
         * @param dynamic <tt>true</tt> if regexp is dynamically computed
         */
        Parameter(Method setter, boolean dynamic) {
            this.type = setter.getParameterTypes()[0];
            this.field = null;
            this.setter = setter;
            this.dynamic = dynamic;
        }


        /**
         * Get regular expression specified with {@link UrlParameter} annotation or default
         * one based on the field/setter type
         *
         * @param clazz class with {@link UrlParameter} annotated methods/fields
         * @throws UrlParameterMappingException if there was an error
         */
        String getRegex(Class<?> clazz) {
            if (setter != null) {
                UrlParameter annotation = setter.getAnnotation(UrlParameter.class);
                if (annotation != null && !annotation.regEx().isEmpty()) return annotation.regEx();
            }
            if (field != null) {
                UrlParameter annotation = field.getAnnotation(UrlParameter.class);
                if (annotation != null && !annotation.regEx().isEmpty()) return annotation.regEx();
            }

            if (type.isAssignableFrom(String.class)) {
                return "[^/]+";
            } else if (type.isAssignableFrom(Integer.class)) {
                return "-?[0-1]?[0-9]{1,9}"; // -1999999999 to 1999999999
            } else if (type.isAssignableFrom(Long.class)) {
                return "-?[0-8]?[0-9]{1,18}"; // -8999999999999999999 to 8999999999999999999
            } else if (type.isAssignableFrom(Boolean.class)) {
                return "true|false"; // true or false
            } else if (type.isAssignableFrom(UUID.class)) {
                return "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"; // UUID
            } else {
                throw new UrlParameterMappingException(String.format(
                        "Unsupported parameter type '%s' for class %s.",
                        type, clazz.getSimpleName()));
            }
        }

        /**
         * Set specified non-primitive parameter to <tt>null</tt> value
         *
         * @param that instance of class with {@link UrlParameter} annotated methods/fields
         * @throws UrlParameterMappingException if there was an error
         */
        void clear(Object that) {
            try {
                if (type != null && !type.isPrimitive()) {
                    if (setter != null) {
                        setter.invoke(that, NULL_ARGUMENT);
                    } else if (field != null) {
                        field.set(that, NULL_ARGUMENT[0]);
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UrlParameterMappingException(String.format(
                        "Error clearing parameter in class %s.",
                        that.getClass().getSimpleName()), e);
            }
        }

        /**
         * Set specified bean property or field value
         *
         * @param that  instance of class with {@link UrlParameter} annotated methods/fields
         * @param value value to set. It will be converted to the parameter type.
         * @throws UrlParameterMappingException if there was an error
         */
        void set(Object that, String value) {
            try {
                if (setter != null) {
                    if (type.isAssignableFrom(String.class)) {
                        setter.invoke(that, value);
                    } else if (type.isAssignableFrom(Integer.class)) {
                        setter.invoke(that, Integer.valueOf(value));
                    } else if (type.isAssignableFrom(Long.class)) {
                        setter.invoke(that, Long.valueOf(value));
                    } else if (type.isAssignableFrom(Boolean.class)) {
                        setter.invoke(that, Boolean.valueOf(value));
                    } else if (type.isAssignableFrom(UUID.class)) {
                        setter.invoke(that, UUID.fromString(value));
                    } else {
                        throw new UrlParameterMappingException(String.format(
                                "Unsupported parameter type '%s' for class %s.",
                                type, that.getClass().getSimpleName()));
                    }
                } else if (field != null) {
                    if (type.isAssignableFrom(String.class)) {
                        field.set(that, value);
                    } else if (type.isAssignableFrom(Integer.class)) {
                        field.set(that, Integer.valueOf(value));
                    } else if (type.isAssignableFrom(Long.class)) {
                        field.set(that, Long.valueOf(value));
                    } else if (type.isAssignableFrom(Boolean.class)) {
                        field.set(that, Boolean.valueOf(value));
                    } else if (type.isAssignableFrom(UUID.class)) {
                        field.set(that, UUID.fromString(value));
                    } else {
                        throw new UrlParameterMappingException(String.format(
                                "Unsupported parameter type '%s' for class %s.",
                                type, that.getClass().getSimpleName()));
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UrlParameterMappingException(String.format(
                        "Error setting parameter in class %s.",
                        that.getClass().getSimpleName()), e);
            }
        }

    }

}
