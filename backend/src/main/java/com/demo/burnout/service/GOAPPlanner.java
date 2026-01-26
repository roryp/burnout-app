package com.demo.burnout.service;

import com.demo.burnout.goap.*;
import com.demo.burnout.goap.actions.*;
import com.demo.burnout.goap.goals.*;
import com.demo.burnout.model.Issue;
import com.demo.burnout.model.WorldState;
import com.demo.burnout.util.DemoLabels;
import com.demo.burnout.util.LabelUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.util.*;

@Service
public class GOAPPlanner {

    private static final int MAX_ACTIONS = 5;

    private final List<Goal> goals;
    private final Clock clock;
    private final boolean demoFridayEnabled;
    private final Comparator<Issue> STABLE_ISSUE_ORDER;

    public GOAPPlanner(Clock clock, 
                       @Value("${demo.friday.enabled:false}") boolean demoFridayEnabled) {
        this.clock = clock;
        this.demoFridayEnabled = demoFridayEnabled;
        
        this.goals = List.of(
            new PreventBurnout(),
            new Achieve333Compliance(),
            new ProtectDeepWork(),
            new ReduceChaos(),
            new EnableFridayDeploy(clock, demoFridayEnabled),
            new ClearMysteryMeat()
        );

        this.STABLE_ISSUE_ORDER = Comparator
            .comparing((Issue i) -> getPriorityWeight(i))
            .thenComparing(i -> DemoLabels.hasLabel(i, DemoLabels.TOUCHED_TODAY) ? 0 : 1)
            .thenComparing(Issue::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Issue::number);
    }

    private int getPriorityWeight(Issue issue) {
        if (LabelUtils.hasLabel(issue, "priority:critical")) return 0;
        if (LabelUtils.hasLabel(issue, "priority:high")) return 1;
        if (LabelUtils.hasLabel(issue, "urgent")) return 1;
        return 2;
    }

    public boolean isFridayScenario(List<Issue> issues) {
        if (demoFridayEnabled) return true;
        if (issues.stream().anyMatch(i -> DemoLabels.hasLabel(i, DemoLabels.FRIDAY))) return true;
        return clock.instant().atZone(clock.getZone()).getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    public GoapActionPlan plan(WorldState initial, List<Issue> availableIssues, String userId) {
        List<Issue> sortedIssues = availableIssues.stream()
            .sorted(STABLE_ISSUE_ORDER)
            .toList();

        List<Action> possibleActions = generateActions(initial, sortedIssues, userId);
        
        List<Action> allActions = new ArrayList<>();
        WorldState currentState = initial;
        Set<String> appliedIds = new HashSet<>();

        while (allActions.size() < MAX_ACTIONS) {
            Goal targetGoal = goals.stream()
                .filter(g -> !g.isSatisfied(currentState))
                .max(Comparator.comparingInt(g -> g.priority() + g.insistence(currentState)))
                .orElse(null);
            
            if (targetGoal == null) {
                break;
            }

            Action nextAction = findBestAction(currentState, targetGoal, possibleActions, appliedIds);
            if (nextAction == null) {
                break;
            }

            allActions.add(nextAction);
            appliedIds.add(nextAction.id());
            currentState = nextAction.apply(currentState);
        }
        
        return toActionPlan(allActions, initial);
    }

    private Action findBestAction(WorldState state, Goal goal, List<Action> actions, Set<String> alreadyApplied) {
        return actions.stream()
            .filter(a -> !alreadyApplied.contains(a.id()))
            .filter(a -> a.preconditionsMet(state))
            .filter(a -> goal.insistence(a.apply(state)) < goal.insistence(state))
            .min(Comparator.comparingInt(a -> a.cost(state)))
            .orElse(null);
    }

    private GoapActionPlan toActionPlan(List<Action> actions, WorldState initial) {
        if (actions.isEmpty()) return GoapActionPlan.empty();
        
        WorldState finalState = actions.stream()
            .reduce(initial, (s, a) -> a.apply(s), (s1, s2) -> s2);
        
        return new GoapActionPlan(actions, initial.calculateStressScore(), finalState.calculateStressScore());
    }

    private List<Action> generateActions(WorldState state, List<Issue> sortedIssues, String userId) {
        List<Action> actions = new ArrayList<>();
        
        List<Issue> actionableIssues = sortedIssues.stream()
            .filter(i -> "open".equals(i.state()))
            .filter(i -> i.assignees() != null && i.assignees().stream()
                .anyMatch(a -> a.login().equals(userId)))
            .toList();
        
        for (Issue issue : actionableIssues) {
            actions.add(new DeferIssue(issue));
            actions.add(new ReclassifyAsQuickWin(issue));
            actions.add(new ReclassifyAsMaintenance(issue));
            actions.add(new MarkDeepWorkFocus(issue));
            actions.add(new AddScopeToIssue(issue));
        }
        
        actions.add(new SlowIntake());
        actions.add(new SuggestBreak());
        actions.add(new BlockCalendarTime());
        
        return actions;
    }
}
