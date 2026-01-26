package com.demo.burnout.util;

import com.demo.burnout.model.Issue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for label matching - used by WorldState and Action implementations.
 */
public final class LabelUtils {
    private LabelUtils() {}

    public static boolean hasLabel(Issue issue, String labelName) {
        if (issue.labels() == null) return false;
        return issue.labels().stream()
            .anyMatch(l -> l.name().equalsIgnoreCase(labelName));
    }

    public static boolean hasLabel(Issue issue, String... labelNames) {
        if (issue.labels() == null || labelNames.length == 0) return false;
        Set<String> target = Arrays.stream(labelNames)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        return issue.labels().stream()
            .anyMatch(l -> target.contains(l.name().toLowerCase()));
    }

    public static boolean hasAnyLabel(Issue issue, List<String> labelNames) {
        if (issue.labels() == null || labelNames.isEmpty()) return false;
        Set<String> target = labelNames.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        return issue.labels().stream()
            .anyMatch(l -> target.contains(l.name().toLowerCase()));
    }

    public static boolean hasLabelPattern(Issue issue, String regex) {
        if (issue.labels() == null) return false;
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return issue.labels().stream()
            .anyMatch(l -> pattern.matcher(l.name()).matches());
    }
}
