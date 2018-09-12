package org.vaadin.flow.helper;

import com.vaadin.flow.internal.AnnotationReader;
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
     * @param that instance of class implementing {@link HasUrlParameterMapping} interface.
     * @param path path that should be matched.
     */
    public static void match(HasUrlParameterMapping that, String path) {
        Mapping mapping = getMapping(that);
        // This will hold all un-touched properties
        Set<String> unsetProperties = new HashSet<>(mapping.allProperties);

        Matcher matcher = mapping.pattern.matcher(path.startsWith("/") ? path : "/" + path);
        if (matcher.find()) {
            // There is 1+ match. Find a group that has the most properties.
            Optional<String> longestMatch = mapping.propertyMapping.keySet().stream()
                    .filter(k -> matcher.group(k) != null)
                    .sorted(Comparator.comparing(k -> mapping.propertyMapping.get((String) k).size()).reversed())
                    .findFirst();
            // Go through all properties defined in pattern and call corresponding setters
            longestMatch.ifPresent(patternId -> {
                for (String propertyId : mapping.propertyMapping.get(patternId)) {
                    String value = matcher.group(patternId + propertyId);
                    if (value != null) {
                        setProperty(that, propertyId, value);
                        unsetProperties.remove(propertyId);
                    }
                }
            });
        }

        // Clear all properties which were not updated
        for (String property : unsetProperties) {
            clearProperty(that, property);
        }
    }

    // Complied parameter mappings go there
    private static Map<Class<? extends HasUrlParameterMapping>, Mapping> mappings = new ConcurrentHashMap<>();

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
            Mapping m = new Mapping();

            // Get all pattern
            List<UrlParameterMapping> annotations = AnnotationReader.getAnnotationsFor(that.getClass(), UrlParameterMapping.class);
            StringBuilder patternBuilder = new StringBuilder();
            for (int i = 0; i < annotations.size(); i++) {
                // Each pattern will have unique id, that will be used for properties
                String patternId = String.format("p%d", i);
                String routePattern = annotations.get(i).value();
                // Add leading / for simplicity
                if (!routePattern.startsWith("/")) routePattern = "/" + routePattern;
                // Replace optional segments with proper regex:
                // [/...] will become (/...)?
                Matcher optionalMatcher = OPTIONAL_PATTERN.matcher(routePattern);
                while (optionalMatcher.find()) {
                    routePattern = optionalMatcher.replaceAll("(/$1)?");
                    optionalMatcher = OPTIONAL_PATTERN.matcher(routePattern);
                }
                Set<String> properties = Collections.synchronizedSet(new HashSet<>(5));
                // Expand parameter mappings without regex:
                // /:param will become /:param:<regex> based on property type
                routePattern = replaceFunctional(PARAMETER_SIMPLE_PATTERN, routePattern, groups -> {
                    try {
                        Class<?> parameterType = PropertyUtils.getPropertyType(that, groups[2]);
                        if (parameterType.isAssignableFrom(String.class)) {
                            return groups[1] + groups[2] + ":[^/]+:";
                        } else if (parameterType.isAssignableFrom(Integer.class)) {
                            return groups[1] + groups[2] + ":-?[0-1]?[0-9]{1,9}:"; // -1999999999 to 1999999999
                        } else if (parameterType.isAssignableFrom(Long.class)) {
                            return groups[1] + groups[2] + ":-?[0-8]?[0-9]{1,18}:"; // -8999999999999999999 to 8999999999999999999
                        } else if (parameterType.isAssignableFrom(Boolean.class)) {
                            return groups[1] + groups[2] + ":true|false:"; // true or false
                        } else {
                            throw new UrlParameterMappingException(String.format(
                                    "Unsupported parameter type '%s' for class %s.",
                                    parameterType, that.getClass().getSimpleName()));
                        }

                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new UrlParameterMappingException("Cannot get property type of " + that.getClass() + "." + groups[1], e);
                    }
                });
                // Collect group names (properties)
                Matcher matcher = PARAMETER_FULL_PATTERN.matcher(routePattern);
                while (matcher.find()) {
                    properties.add(matcher.group(2));
                    m.allProperties.add(matcher.group(2));
                }
                // Join all patterns with "|"
                if (patternBuilder.length() > 0) patternBuilder.append("|");
                // Replace parameter mappings with named capture groups:
                // /:param:[0-9]+ will become /(?<p0param>[0-9]+)
                patternBuilder.append("(?<").append(patternId).append(">")
                        .append(matcher.reset().replaceAll("/(?<" + patternId + "$2>$3)"))
                        .append(")");

                m.propertyMapping.put(patternId, properties);
            }

            m.pattern = Pattern.compile("^(" + patternBuilder.toString() + ")$");

            return m;
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
        Pattern pattern;
        final Map<String, Set<String>> propertyMapping = new ConcurrentHashMap<>();
        final Set<String> allProperties = Collections.synchronizedSet(new HashSet<>());
    }

}
