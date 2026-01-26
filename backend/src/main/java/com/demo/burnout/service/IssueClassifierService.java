package com.demo.burnout.service;

import com.demo.burnout.model.Classification;
import com.demo.burnout.model.Issue;
import com.demo.burnout.util.LabelUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class IssueClassifierService {

    public Classification classify(Issue issue) {
        if (isDeepWork(issue)) {
            return Classification.DEEP_WORK;
        }
        if (isQuickWin(issue)) {
            return Classification.QUICK_WIN;
        }
        if (isMaintenance(issue)) {
            return Classification.MAINTENANCE;
        }
        return Classification.DEFERRED;
    }

    private boolean isDeepWork(Issue issue) {
        return hasAnyLabel(issue, "priority:critical", "priority:high", "architecture", "security", "deep-work")
            || estimateHours(issue) > 2
            || hasLabelPattern(issue, "epic.*|feature.*");
    }

    private boolean isQuickWin(Issue issue) {
        if (issue.body() == null || issue.body().isBlank()) {
            return false;
        }
        return hasAnyLabel(issue, "good-first-issue", "quick-win", "low-hanging-fruit", "trivial")
            || (estimateHours(issue) < 0.5 && hasClearScope(issue))
            || (LabelUtils.hasLabel(issue, "enhancement") && issue.body().length() < 500);
    }

    private boolean isMaintenance(Issue issue) {
        return hasAnyLabel(issue, "dependencies", "documentation", "triage", 
                          "chore", "refactor", "tech-debt", "ci", "devops", "maintenance");
    }

    private double estimateHours(Issue issue) {
        if (issue.labels() == null) return 2.0;
        
        for (Issue.Label label : issue.labels()) {
            String name = label.name().toLowerCase();
            if (name.startsWith("estimate:")) {
                return parseEstimate(name.substring(9));
            }
            if (name.equals("size:s") || name.equals("small")) return 0.5;
            if (name.equals("size:m") || name.equals("medium")) return 2.0;
            if (name.equals("size:l") || name.equals("large")) return 4.0;
            if (name.equals("size:xl")) return 8.0;
        }

        if (issue.body() != null) {
            int bodyLength = issue.body().length();
            if (bodyLength < 100) return 0.5;
            if (bodyLength < 500) return 2.0;
            return 4.0;
        }

        return 2.0;
    }

    private boolean hasClearScope(Issue issue) {
        if (issue.body() == null) return false;
        String body = issue.body().toLowerCase();
        return body.contains("- [ ]") 
            || body.contains("acceptance criteria")
            || body.contains("done when")
            || body.contains("steps:")
            || body.contains("expected:");
    }

    private boolean hasAnyLabel(Issue issue, String... labelNames) {
        if (issue.labels() == null) return false;
        Set<String> target = Arrays.stream(labelNames)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        return issue.labels().stream()
            .anyMatch(l -> target.contains(l.name().toLowerCase()));
    }

    private boolean hasLabelPattern(Issue issue, String regex) {
        if (issue.labels() == null) return false;
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return issue.labels().stream()
            .anyMatch(l -> pattern.matcher(l.name()).matches());
    }

    private double parseEstimate(String est) {
        try {
            return Double.parseDouble(est.replace("h", "").replace("hr", ""));
        } catch (NumberFormatException e) {
            return 2.0;
        }
    }
}
