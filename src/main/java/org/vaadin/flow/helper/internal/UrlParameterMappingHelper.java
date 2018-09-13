package org.vaadin.flow.helper.internal;

import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.BeforeEvent;
import org.vaadin.flow.helper.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Helper class for {@link UrlParameterMapping} matching. Not indented for direct use.
 *
 * @author Artem Godin
 */
public class UrlParameterMappingHelper {

    /**
     * Match patterns specified in {@link UrlParameterMapping} annotations to the supplied path
     * and update all associated properties.
     * <p>
     * Automatically reroutes to {@code NotFoundException} or other error specified in {@link
     * RerouteIfNotMatched} annotation if no matches detected, unless {@link IgnoreIfNotMatched} annotation is
     * present.
     *
     * @param event BeforeEvent passed from {@link HasUrlParameterMapping#setParameter(BeforeEvent, String)}
     * @param that  instance of class implementing {@link HasUrlParameterMapping} interface.
     * @param path  path that should be matched.
     */
    public static void match(BeforeEvent event, HasUrlParameterMapping that, String path) {
        // Clean old matching pattern
        matchedPattern.remove(that);

        Mapping mapping = getMapping(that);
        Set<String> unsetParameters = new HashSet<>(mapping.parameters.keySet());

        Matcher matcher = mapping.compiledPattern.matcher(path.startsWith("/") ? path : "/" + path);
        if (matcher.find()) {
            // There is 1+ match. Find a group that has the most properties.
            Optional<Mapping.MappingPattern> longestMatch = mapping.mappingPatterns.stream()
                    .filter(pattern -> matcher.group(pattern.id) != null)
                    .sorted(Comparator.comparing(pattern -> pattern.index))
                    .findFirst();
            // Go through all properties defined in compiledPattern and call corresponding setters
            longestMatch.ifPresent(match -> {
                for (String parameter : match.parameters) {
                    String value = matcher.group(match.id + parameter);
                    if (value != null) {
                        mapping.parameters.get(parameter).set(that, value);
                        unsetParameters.remove(parameter);
                    }
                }
                matchedPattern.put(that, match.pattern);
            });
        } else {
            if (mapping.rerouteException != null) {
                event.rerouteToError(mapping.rerouteException);
                return; // No need to clear properties then
            }
        }

        for (String unsetParameter : unsetParameters) {
            mapping.parameters.get(unsetParameter).clear(that);
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

            // Collect all declared UrlParameters
            Stream.of(that.getClass().getDeclaredFields())
                    .filter(field -> field.getAnnotation(UrlParameter.class) != null)
                    .forEach(field -> {
                        UrlParameter annotation = field.getAnnotation(UrlParameter.class);
                        String name = annotation.name().isEmpty() ? field.getName() : annotation.name();
                        mapping.parameters.put(name, new Mapping.Parameter(field));
                    });

            Stream.of(that.getClass().getDeclaredMethods())
                    .filter(method -> method.getAnnotation(UrlParameter.class) != null && method.getParameterCount() == 1)
                    .forEach(method -> {
                        UrlParameter annotation = method.getAnnotation(UrlParameter.class);
                        String computedName = method.getName();
                        if (computedName.length() > 3 && computedName.startsWith("set")) {
                            computedName = computedName.substring(3, 4).toLowerCase() + computedName.substring(4);
                        }
                        String name = annotation.name().isEmpty() ? computedName : annotation.name();
                        mapping.parameters.put(name, new Mapping.Parameter(method));
                    });

            // Get all compiledPattern
            List<UrlParameterMapping> annotations = AnnotationReader.getAnnotationsFor(that.getClass(), UrlParameterMapping.class);
            StringBuilder patternBuilder = new StringBuilder();
            for (int i = 0; i < annotations.size(); i++) {
                // Each pattern will have unique id, that will be used for properties
                String patternId = String.format("p%d", i);
                String routePattern = annotations.get(i).value();

                Map<String, String> parameterRegEx = new HashMap<>();

                Mapping.MappingPattern mappingPattern = new Mapping.MappingPattern();
                mappingPattern.id = patternId;
                mappingPattern.pattern = routePattern;
                mappingPattern.parameters = new HashSet<>();
                mappingPattern.index = i;

                // Add leading / for simplicity
                if (!routePattern.startsWith("/")) routePattern = "/" + routePattern;

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
                routePattern = matcher.reset().replaceAll("/:$2");

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
                                                        matches[2], that.getClass().getSimpleName()));
                                            }
                                            return "/(?<" + patternId + matches[2] + ">"
                                                    + parameterRegEx.getOrDefault(matches[2], parameter.getRegex(that))
                                                    + ")";
                                        }
                                ))
                        .append(")");

                mapping.mappingPatterns.add(mappingPattern);
            }

            mapping.compiledPattern = Pattern.compile("^(" + patternBuilder.toString() + ")$");

            return mapping;
        });
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

}
