package org.vaadin.flow.helper;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import org.vaadin.flow.helper.Mapping.Parameter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Helper class for {@link UrlParameterMapping} matching, allowing to perform
 * matching in i.e. http request handlers
 *
 * @author Artem Godin
 */
public class UrlParameterMappingHelper {

    /**
     * Match patterns specified in {@link UrlParameterMapping} annotations to the supplied path
     * and update all associated properties. Path may also contain query parameters which would be
     * matched against {@link UrlParameterMapping#queryParameters()}.
     *
     * @param that instance of class annotated with {@link UrlParameterMapping}
     * @param path path that should be matched.
     * @return <tt>true</tt> if there is a matched pattern, <tt>false</tt> otherwise
     */
    public static boolean match(Object that, String path) {
        Mapping mapping = getMapping(that.getClass());
        return match(mapping, that, path);
    }

    /**
     * Match patterns specified in {@link UrlParameterMapping} annotations to the supplied path
     * and update all associated properties. Path may also contain query parameters which would be
     * matched against {@link UrlParameterMapping#queryParameters()}.
     *
     * @param that    instance of class annotated with {@link UrlParameterMapping}
     * @param request VaadinRequest
     * @return <tt>true</tt> if there is a matched pattern, <tt>false</tt> otherwise
     */
    public static boolean match(Object that, VaadinRequest request) {
        VaadinServletRequest servletRequest = (VaadinServletRequest) request;
        Mapping mapping = getMapping(that.getClass());
        return match(mapping, that, servletRequest.getRequestURI()
                + "?" + ((servletRequest.getQueryString() == null) ? "" : servletRequest.getQueryString()));
    }

    /**
     * Match patterns specified in {@link UrlParameterMapping} annotations to the supplied path
     * and update all associated properties.
     * <p>
     * Automatically reroutes to {@code NotFoundException} or other error specified in {@link
     * RerouteIfNotMatched} annotation if no matches detected, unless {@link IgnoreIfNotMatched} annotation is
     * present.
     *
     * @param event BeforeEvent passed from {@link HasUrlParameterMapping#setParameter(BeforeEvent, String)}
     * @param that  instance of class annotated with {@link UrlParameterMapping}
     * @param path  path that should be matched.
     */
    static boolean matchAndReroute(BeforeEvent event, Object that, String path) {
        Mapping mapping = getMapping(that.getClass());
        boolean hasMatch = match(mapping, that, path);
        if (!hasMatch) {
            if (mapping.rerouteView != RerouteIfNotMatched.NoView.class) {
                event.rerouteTo(mapping.rerouteView);
            } else if (mapping.rerouteException != RerouteIfNotMatched.NoException.class) {
                event.rerouteToError(mapping.rerouteException);
            }
        }
        return hasMatch;
    }

    /**
     * Sets dynamic regular expression for specified parameter. Such parameters must have {@code dynamicRegEx = true} specified
     * in {@link UrlParameter} annotation. This method must be called for such parameters before first call
     * to {@link UrlParameterMappingHelper#match(Object, String)}
     *
     * @param clazz         class annotated with {@link UrlParameterMapping}
     * @param parameter     parameter name
     * @param regexProducer producer of regular expression
     * @param <T>           type of class annotated with {@link UrlParameterMapping}
     */
    @SuppressWarnings("unchecked")
    public static <T> void setDynamicRegex(Class<T> clazz, String parameter, Function<T, String> regexProducer) {
        Mapping mapping = getMapping(clazz);
        if (!mapping.hasDynamicRegex) {
            throw new UrlParameterMappingException("Attempt to set dynamic regex, but no parameters with dynamic regex are defined");
        }
        Parameter p = mapping.parameters.get(parameter);
        if (p == null) {
            throw new UrlParameterMappingException(String.format("Parameter %s was not found in parameter list", parameter));
        }
        p.regexProducer = (Function<Object, String>) regexProducer;
    }

    /**
     * Format path with parameters. Format syntax is similar to {@link UrlParameterMapping}, with the addition of numbered arguments
     * that can be passed. For example,
     * <pre><code>
     *     UrlParameterMappingHelper.format(this, "order/:orderId[/:orderRawId]/:1", tabId)
     * </code></pre>
     * with <tt>tabId = "edit"</tt> will use <tt>orderId</tt> and <tt>orderRawId</tt> fields annotated with {@link UrlParameter} and
     * produce <tt>"order/12345/edit"</tt> or <tt>"order/12345/45/edit"</tt> depending on whether <tt>orderRawId</tt> was set or not.
     * <p>
     * Note 1: no inline regular expressions are allowed
     * <p>
     * Note 2: only fields may be used
     *
     * @param that      instance of class annotated with {@link UrlParameterMapping}
     * @param format    format string
     * @param arguments optional string arguments. Those can be used with <tt>/:1</tt>..<tt>/:99</tt> notation (one-based indexing).
     * @return formatted string
     */
    public static String format(Object that, String format, String... arguments) {
        Mapping mapping = getMapping(that.getClass());
        String formattedPath = replaceFunctional(PARAMETER_SIMPLE_FORMAT_PATTERN, format, matches -> {
            String parameterName = matches[2];
            String value;
            if (Character.isDigit(parameterName.charAt(0))) {
                int index = Integer.valueOf(parameterName) - 1;
                if (index < 0 || index > arguments.length) {
                    throw new UrlParameterMappingException(String.format("Replacement \":%s\" in \"%s\" is not present in arguments", parameterName, format));
                }
                value = arguments[index];
            } else {
                Parameter parameter = mapping.parameters.get(parameterName);
                if (parameter == null) {
                    throw new UrlParameterMappingException(String.format("Unknown parameter %s in format \"%s\" for class %s", parameterName, format, that.getClass().getSimpleName()));
                }
                value = parameter.get(that);
            }
            if (value == null) return "/\0";
            return "/" + value;
        });
        formattedPath = replaceFunctional(OPTIONAL_FORMAT_PATTERN, formattedPath, matches -> matches[1].equals("\0") ? "" : "/" + matches[1]);
        if (formattedPath.contains("\0")) {
            throw new UrlParameterMappingException(String.format("Required parameter have null value for \"%s\": %s", format, formattedPath.replace("\0", "<null>")));
        }
        return formattedPath;
    }

    /**
     * Match patterns specified in {@link UrlParameterMapping} annotations to the supplied path
     * and update all associated properties.
     *
     * @param mapping mapping
     * @param that    instance of class annotated with {@link UrlParameterMapping}
     * @param path    path to match
     * @return <tt>true</tt> if there are matched patterns
     */
    private static boolean match(Mapping mapping, Object that, String path) {
        boolean result = false;
        // Clean old matching pattern
        Set<String> unsetParameters = new HashSet<>(mapping.parameters.keySet());

        // Add dummy query parameters
        if (!path.contains("?")) path = path + "?";
        if (!path.endsWith("&")) path = path + "&";

        Matcher matcher = getMatcher(mapping, that, path);
        if (matcher.find()) {
            result = true;
            // There is 1+ match. Find a group that has the most properties.
            mapping.mappingPatterns.stream()
                    .filter(pattern -> matcher.group(pattern.id) != null)
                    .min(Comparator.comparing(pattern -> pattern.index))
                    .ifPresent(match -> {
                        Mapping.Parameter matchedPattern = mapping.parameters.get(MATCHED_PATTERN_PARAMETER);
                        if (matchedPattern != null) {
                            unsetParameters.remove(MATCHED_PATTERN_PARAMETER);
                            matchedPattern.set(that, match.pattern);
                        }
                        for (String parameter : match.parameters) {
                            String value = matcher.group(match.id + parameter);
                            if (value != null) {
                                mapping.parameters.get(parameter).set(that, value);
                                unsetParameters.remove(parameter);
                            }
                        }
                    });
        }

        for (String unsetParameter : unsetParameters) {
            mapping.parameters.get(unsetParameter).clear(that);
        }

        return result;
    }

    /**
     * Get {@link Matcher} for mapping.
     *
     * @param mapping mapping
     * @param that    instance of class annotated with {@link UrlParameterMapping}
     * @param path    path to match
     * @return {@link Matcher}
     */
    private static Matcher getMatcher(Mapping mapping, Object that, String path) {
        // Prepend slash
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // Process dynamic regular expressions
        if (mapping.hasDynamicRegex) {
            String newPattern = mapping.pattern;
            for (Map.Entry<String, Mapping.Parameter> stringParameterEntry : mapping.parameters.entrySet()) {
                String parameterName = stringParameterEntry.getKey();
                Parameter parameter = stringParameterEntry.getValue();
                if (parameter.dynamic) {
                    // This parameter has dynamic regex -- compute one
                    if (parameter.regexProducer == null) {
                        throw new UrlParameterMappingException(String.format("No dynamic regexp specified for parameter %s in class %s", parameterName, that.getClass().getSimpleName()));
                    }
                    String regex = parameter.regexProducer.apply(that);
                    if (regex == null) {
                        // Get default regex
                        regex = parameter.getRegex(that.getClass());
                    }
                    // update regular expression
                    newPattern = newPattern.replaceAll("\\(\\?\\<p([0-9]+" + parameterName + ")\\>\\)", "(?<p$1>" + regex + ")");
                }
            }
            return Pattern.compile(newPattern).matcher(path);
        } else {
            return mapping.compiledPattern.matcher(path);
        }
    }

    // Complied parameter mappingPatterns go there
    private static Map<Class<?>, Mapping> mappings = new ConcurrentHashMap<>();

    private static final Pattern OPTIONAL_PATTERN = Pattern.compile("\\[/([^/^\\[]+)\\]");
    private static final Pattern OPTIONAL_FORMAT_PATTERN = Pattern.compile("\\[\\/([^\\]]+)\\]");
    private static final Pattern PARAMETER_SIMPLE_FORMAT_PATTERN = Pattern.compile("(/:)([\\d]+|[\\w]+)(?![\\w\\d:])");
    private static final Pattern PARAMETER_SIMPLE_PATTERN = Pattern.compile("(/:|=:)([\\w]+)(?![\\w:])");
    private static final Pattern PARAMETER_FULL_PATTERN = Pattern.compile("(/:|=:)([\\w]+):([^:]+):");

    private static final String MATCHED_PATTERN_PARAMETER = "[pattern]";

    /**
     * Get or compute property mapping for specified class
     *
     * @param clazz class with {@link UrlParameterMapping} annotations
     * @return static {@link Mapping}
     */
    private static Mapping getMapping(Class<?> clazz) {
        return mappings.computeIfAbsent(clazz, c -> {
            Mapping mapping = new Mapping();

            Optional.ofNullable(clazz.getAnnotation(RerouteIfNotMatched.class))
                    .ifPresent(rerouteIfNotMatched -> {
                        mapping.rerouteException = rerouteIfNotMatched.exception();
                        mapping.rerouteView = rerouteIfNotMatched.view();
                    });

            Optional.ofNullable(clazz.getAnnotation(IgnoreIfNotMatched.class))
                    .ifPresent(rerouteIfNotMatched -> {
                        mapping.rerouteException = RerouteIfNotMatched.NoException.class;
                        mapping.rerouteView = RerouteIfNotMatched.NoView.class;
                    });

            collectUrlParameters(mapping, clazz);

            // Get all compiledPattern
            UrlParameterMapping[] annotations = clazz.getAnnotationsByType(UrlParameterMapping.class);
            StringBuilder patternBuilder = new StringBuilder();
            for (int i = 0; i < annotations.length; i++) {
                // Each pattern will have unique id, that will be used for properties
                computeMappingPattern(mapping, patternBuilder, clazz, i, annotations[i].value(), annotations[i].queryParameters());
            }

            mapping.pattern = "^(" + patternBuilder.toString() + ")$";
            if (!mapping.hasDynamicRegex) {
                mapping.compiledPattern = Pattern.compile(mapping.pattern);
            }

            return mapping;
        });
    }

    /**
     * Collect parameters annotated with {@link UrlParameter}
     *
     * @param mapping mapping
     * @param clazz   class
     */
    private static void collectUrlParameters(Mapping mapping, Class<?> clazz) {
        // Find field/method for UrlMatchedPatternParameter
        Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.getAnnotation(UrlMatchedPatternParameter.class) != null)
                .findFirst()
                .ifPresent(field -> mapping.parameters.putIfAbsent(MATCHED_PATTERN_PARAMETER, new Parameter(field, false)));

        Stream.of(clazz.getDeclaredMethods())
                .filter(method -> method.getAnnotation(UrlMatchedPatternParameter.class) != null)
                .findFirst()
                .ifPresent(method -> mapping.parameters.putIfAbsent(MATCHED_PATTERN_PARAMETER, new Parameter(method, false)));

        // Collect all declared UrlParameters
        Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.getAnnotation(UrlParameter.class) != null)
                .forEach(field -> {
                    UrlParameter annotation = field.getAnnotation(UrlParameter.class);
                    String name = field.getName();
                    if (!annotation.name().isEmpty()) {
                        name = annotation.name();
                    }
                    mapping.hasDynamicRegex = mapping.hasDynamicRegex || annotation.dynamicRegEx();
                    if (mapping.parameters.put(name, new Parameter(field, annotation.dynamicRegEx())) != null) {
                        throw new UrlParameterMappingException(String.format("Duplicate parameter %s in class %s", name, clazz.getSimpleName()));
                    }
                });

        Stream.of(clazz.getDeclaredMethods())
                .filter(method -> method.getAnnotation(UrlParameter.class) != null && method.getParameterCount() == 1)
                .forEach(method -> {
                    UrlParameter annotation = method.getAnnotation(UrlParameter.class);
                    String name = method.getName();
                    if (name.length() > 3 && name.startsWith("set")) {
                        name = name.substring(3, 4).toLowerCase() + name.substring(4);
                    }
                    if (!annotation.name().isEmpty()) {
                        name = annotation.name();
                    }
                    mapping.hasDynamicRegex = mapping.hasDynamicRegex || annotation.dynamicRegEx();
                    if (mapping.parameters.put(name, new Parameter(method, annotation.dynamicRegEx())) != null) {
                        throw new UrlParameterMappingException(String.format("Duplicate parameter %s in class %s", name, clazz.getSimpleName()));
                    }
                });
    }

    /**
     * Compute and build mapping pattern for specified {@link UrlParameterMapping}
     *
     * @param mapping        mapping
     * @param patternBuilder {@link StringBuilder} used to build regular expression
     * @param clazz          class
     * @param index          index of {@link UrlParameterMapping} annotation, used for natural ordering
     * @param routePattern   pattern
     */
    private static void computeMappingPattern(Mapping mapping, StringBuilder patternBuilder, Class<?> clazz, int index, String routePattern, String[] queryParameters) {
        // Each pattern will have unique id, that will be used for properties
        String patternId = String.format("p%d", index);

        Map<String, String> parameterRegEx = new HashMap<>();

        Mapping.MappingPattern mappingPattern = new Mapping.MappingPattern();
        mappingPattern.id = patternId;
        mappingPattern.pattern = routePattern;
        mappingPattern.parameters = new HashSet<>();
        mappingPattern.index = index;

        // Add leading / for simplicity
        if (!routePattern.startsWith("/") && !routePattern.startsWith("[/")) routePattern = "/" + routePattern;

        StringBuilder queryParameterGroup = new StringBuilder("\\?((");
        // Append query parameters
        for (String queryParameter : queryParameters) {
            queryParameterGroup.append("(").append(queryParameter).append(")|");
        }
        queryParameterGroup.append("(.*?))&)*");
        routePattern = routePattern + queryParameterGroup;

        // Expand parameter mapping without regex:
        // /:param will become /:param:<regex> based on property type
        Matcher matcher = PARAMETER_SIMPLE_PATTERN.matcher(routePattern);
        while (matcher.find()) {
            String parameterName = matcher.group(2);
            mappingPattern.parameters.add(parameterName);
        }

        // Extract custom regular expressions
        matcher = PARAMETER_FULL_PATTERN.matcher(routePattern);
        while (matcher.find()) {
            String parameterName = matcher.group(2);
            parameterRegEx.put(parameterName, matcher.group(3));
            mappingPattern.parameters.add(parameterName);
        }
        routePattern = matcher.reset().replaceAll("$1$2");

        // Replace optional segments with proper regex:
        // [/...] will become (/...)?
        routePattern = OPTIONAL_PATTERN.matcher(routePattern).replaceAll("(/$1)?");

        // Join all patterns with "|"
        if (patternBuilder.length() > 0) patternBuilder.append("|");
        // Replace parameter mapping with named capture groups:
        // /:param will become /(?<p0param>...)
        patternBuilder.append("(?<").append(patternId).append(">")
                .append(replaceFunctional
                        (
                                PARAMETER_SIMPLE_PATTERN,
                                routePattern,
                                matches -> {
                                    Mapping.Parameter parameter = mapping.parameters.get(matches[2]);
                                    if (parameter == null) {
                                        throw new UrlParameterMappingException(String.format(
                                                "Unknown parameter '%s' in class %s.",
                                                matches[2], clazz.getSimpleName()));
                                    }
                                    return matches[1].substring(0, 1) + "(?<" + patternId + matches[2] + ">"
                                            + parameterRegEx.getOrDefault(matches[2], parameter.dynamic ? "" : parameter.getRegex(clazz))
                                            + ")";
                                }
                        ))
                .append(")");

        mapping.mappingPatterns.add(mappingPattern);
    }

    /**
     * Equivalent of Mather.replaceAll with computed replacements
     *
     * @param pattern     pattern
     * @param input       input string
     * @param replacement replacement function converting <tt>String[]</tt> of groups to String. If function returns null, the
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

}
