package org.vaadin.flow.helper;

import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.NotFoundException;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for {@link UrlParameterMapping} matching. Not indented for direct use.
 *
 * @author Artem Godin
 */
public class UrlParameterMappingHelper {

    /**
     * Match patterns specified in {@link UrlParameterMapping} annotations to the supplied path
     * and update all associated properties.
     *
     * Automatically reroutes to {@code NotFoundException} or other error specified in {@link
     * RerouteIfNotMatched} annotation if no matches detected, unless {@link IgnoreIfNotMatched} annotation is
     * present.
     *
     * @param event BeforeEvent passed from {@link HasUrlParameterMapping#setParameter(BeforeEvent, String)}
     * @param that instance of class implementing {@link HasUrlParameterMapping} interface.
     * @param path path that should be matched.
     */
    public static void match(BeforeEvent event, HasUrlParameterMapping that, String path) {
        // Clean old matching pattern
        matchedPattern.remove(that);

        Mapping mapping = getMapping(that);
        // This will hold all un-touched properties
        Set<String> unsetProperties = new HashSet<>(mapping.properties);

        Matcher matcher = mapping.compiledPattern.matcher(path.startsWith("/") ? path : "/" + path);
        if (matcher.find()) {
            // There is 1+ match. Find a group that has the most properties.
            Optional<String> longestMatch = mapping.mappingPatterns.keySet().stream()
                    .filter(k -> matcher.group(k) != null)
                    .sorted(Comparator.comparing(k -> mapping.mappingPatterns.get(k).index))
                    .findFirst();
            // Go through all properties defined in compiledPattern and call corresponding setters
            longestMatch.ifPresent(patternId -> {
                for (String propertyId : mapping.mappingPatterns.get(patternId).properties) {
                    String value = matcher.group(patternId + propertyId);
                    if (value != null) {
                        setProperty(that, propertyId, value);
                        unsetProperties.remove(propertyId);
                    }
                }
                matchedPattern.put(that, mapping.mappingPatterns.get(patternId).pattern);
            });
        } else {
            if (mapping.rerouteException != null) {
                event.rerouteToError(mapping.rerouteException);
                return; // No need to clear properties then
            }
        }

        // Clear all properties which were not updated
        for (String property : unsetProperties) {
            clearProperty(that, property);
        }
    }

    public static String getMatchedPattern(HasUrlParameterMapping that) {
        return matchedPattern.get(that);
    }

    // Complied parameter mappingPatterns go there
    private static Map<Class<? extends HasUrlParameterMapping>, Mapping> mappings = new ConcurrentHashMap<>();

    // Store patterns that were matched per instance
    private static Map<HasUrlParameterMapping, String> matchedPattern = Collections.synchronizedMap(new WeakHashMap<>());

    private static Pattern OPTIONAL_PATTERN = Pattern.compile("\\[/([^/]+)\\]");
    private static Pattern PARAMETER_SIMPLE_PATTERN = Pattern.compile("(/:)([\\w]+)(?![\\w:])");
    private static Pattern PARAMETER_FULL_PATTERN = Pattern.compile("(/:)([\\w]+):([^:]+):");

    /**
     * Get or compute property mapping for specified class
     *
     * @param that class implementing {@link HasUrlParameterMapping}
     * @return static {@link Mapping}
     */
    private static Mapping getMapping(HasUrlParameterMapping that) {
        return mappings.computeIfAbsent(that.getClass(), c -> {
            Mapping mapping = new Mapping();

            AnnotationReader.getAnnotationFor(that.getClass(), RerouteIfNotMatched.class)
                    .ifPresent(rerouteIfNotMatched -> mapping.rerouteException = rerouteIfNotMatched.value());

            AnnotationReader.getAnnotationFor(that.getClass(), IgnoreIfNotMatched.class)
                    .ifPresent(ignoreIfNotMatched -> mapping.rerouteException = null);

            // Get all compiledPattern
            List<UrlParameterMapping> annotations = AnnotationReader.getAnnotationsFor(that.getClass(), UrlParameterMapping.class);
            StringBuilder patternBuilder = new StringBuilder();
            for (int i = 0; i < annotations.size(); i++) {
                // Each pattern will have unique id, that will be used for properties
                String patternId = String.format("p%d", i);
                String routePattern = annotations.get(i).value();

                Mapping.MappingPattern mappingPattern = new Mapping.MappingPattern();
                mappingPattern.pattern = routePattern;
                mappingPattern.properties = Collections.synchronizedSet(new HashSet<>());
                mappingPattern.index = i;

                // Add leading / for simplicity
                if (!routePattern.startsWith("/")) routePattern = "/" + routePattern;

                Map<String, String> propertyPatterns = new HashMap<>();

                // Expand parameter mapping without regex:
                // /:param will become /:param:<regex> based on property type
                Matcher matcher = PARAMETER_SIMPLE_PATTERN.matcher(routePattern);
                while (matcher.find()) {
                    String property = matcher.group(2);
                    try {
                        Class<?> parameterType = PropertyUtils.getPropertyType(that, property);
                        if (parameterType == null) {
                            throw new UrlParameterMappingException(String.format(
                                    "Unknown property '%s' in class %s.",
                                    property, that.getClass().getSimpleName()));
                        } else if (parameterType.isAssignableFrom(String.class)) {
                            propertyPatterns.put(property, "[^/]+");
                        } else if (parameterType.isAssignableFrom(Integer.class)) {
                            propertyPatterns.put(property, "-?[0-1]?[0-9]{1,9}"); // -1999999999 to 1999999999
                        } else if (parameterType.isAssignableFrom(Long.class)) {
                            propertyPatterns.put(property, "-?[0-8]?[0-9]{1,18}"); // -8999999999999999999 to 8999999999999999999
                        } else if (parameterType.isAssignableFrom(Boolean.class)) {
                            propertyPatterns.put(property, "true|false"); // true or false
                        } else if (parameterType.isAssignableFrom(UUID.class)) {
                            propertyPatterns.put(property, "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"); // UUID
                        } else {
                            throw new UrlParameterMappingException(String.format(
                                    "Unsupported parameter type '%s' for class %s.",
                                    parameterType, that.getClass().getSimpleName()));
                        }
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new UrlParameterMappingException("Cannot get property type of " + that.getClass() + "." + property, e);
                    }
                }

                // Extract custom regular expressions
                matcher = PARAMETER_FULL_PATTERN.matcher(routePattern);
                while (matcher.find()) {
                    propertyPatterns.put(matcher.group(2), matcher.group(3));
                }
                routePattern = matcher.reset().replaceAll("/:$2");

                // Replace optional segments with proper regex:
                // [/...] will become (/...)?
                routePattern = OPTIONAL_PATTERN.matcher(routePattern).replaceAll("(/$1)?");

                // Collect group names (properties)
                for (String property : propertyPatterns.keySet()) {
                    mappingPattern.properties.add(property);
                    mapping.properties.add(property);
                }
                // Join all patterns with "|"
                if (patternBuilder.length() > 0) patternBuilder.append("|");
                // Replace parameter mapping with named capture groups:
                // /:param will become /(?<p0param>...)
                patternBuilder.append("(?<").append(patternId).append(">")
                        .append(replaceFunctional
                                (
                                        PARAMETER_SIMPLE_PATTERN,
                                        routePattern,
                                        matches -> "/(?<" + patternId + matches[2] + ">" + propertyPatterns.get(matches[2]) + ")"
                                ))
                        .append(")");

                mapping.mappingPatterns.put(patternId, mappingPattern);
            }

            mapping.compiledPattern = Pattern.compile("^(" + patternBuilder.toString() + ")$");

            return mapping;
        });
    }

    /**
     * Set specified bean property to {@code null} value
     *
     * @param that class implementing {@link HasUrlParameterMapping}
     * @param name name of bean property
     * @throws UrlParameterMappingException if there was an error
     */
    private static void clearProperty(HasUrlParameterMapping that, String name) {
        try {
            PropertyUtils.setProperty(that, name, null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new UrlParameterMappingException("Cannot clear property: " + name, e);
        }
    }

    /**
     * Set specified bean property
     *
     * @param that  class implementing {@link HasUrlParameterMapping}
     * @param name  name of bean property
     * @param value value to set. It will be converted to the property type.
     * @throws UrlParameterMappingException if there was an error
     */
    private static void setProperty(HasUrlParameterMapping that, String name, String value) {
        try {
            Class<?> parameterType = PropertyUtils.getPropertyDescriptor(that, name).getPropertyType();

            if (parameterType.isAssignableFrom(String.class)) {
                PropertyUtils.setProperty(that, name, value);
            } else if (parameterType.isAssignableFrom(Integer.class)) {
                PropertyUtils.setProperty(that, name, Integer.valueOf(value));
            } else if (parameterType.isAssignableFrom(Long.class)) {
                PropertyUtils.setProperty(that, name, Long.valueOf(value));
            } else if (parameterType.isAssignableFrom(Boolean.class)) {
                PropertyUtils.setProperty(that, name, Boolean.valueOf(value));
            } else if (parameterType.isAssignableFrom(UUID.class)) {
                PropertyUtils.setProperty(that, name, UUID.fromString(value));
            } else {
                throw new UrlParameterMappingException(String.format(
                        "Unsupported parameter type '%s' for class %s.",
                        parameterType, that.getClass().getSimpleName()));
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new UrlParameterMappingException("Cannot set property: " + name + " to value \"" + value + "\"", e);
        }
    }

    /**
     * Equivalent of Mather.replaceAll with computed replacements
     *
     * @param pattern     pattern
     * @param input       input string
     * @param replacement replacement function converting String[] of groups to String. If function returns null, the
     *                    match is simply removed from the result
     * @return input string with all replacements applied
     */
    private static String replaceFunctional(Pattern pattern, String input, Function<String[], String> replacement) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int groupCount = matcher.groupCount() + 1;
            String[] groups = new String[groupCount];
            for (int i = 0; i < groupCount; i++) {
                groups[i] = matcher.group(i);
            }
            String result = replacement.apply(groups);
            if (result != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(result));
            }
        }
        return matcher.appendTail(sb).toString();
    }

    private UrlParameterMappingHelper() {
    }

    static class Mapping {
        static class MappingPattern {
            int index;
            Set<String> properties;
            String pattern;
        }

        Class<? extends Exception> rerouteException = NotFoundException.class;
        Pattern compiledPattern;
        final Map<String, MappingPattern> mappingPatterns = new ConcurrentHashMap<>();
        final Set<String> properties = Collections.synchronizedSet(new HashSet<>());
    }

}
