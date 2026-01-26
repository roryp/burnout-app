#!/bin/bash
# Issue Seeding Script for Burnout Demo
# Run: bash scripts/seed-issues.sh

REPO="${1:-roryp/burnout-demo}"

echo "Seeding chaotic issues for $REPO..."

# 5 Mystery Meat (urgent, no owner, vague)
gh issue create -R $REPO -t "URGENT: Production is slow" -b "" -l urgent
gh issue create -R $REPO -t "Fix the thing" -b "You know which one" -l urgent
gh issue create -R $REPO -t "ASAP: Customer unhappy" -b "" -l urgent,priority:critical
gh issue create -R $REPO -t "!!!" -b "Look at logs" -l urgent
gh issue create -R $REPO -t "Need this yesterday" -b "" -l urgent

# 3 Contradictory labels
gh issue create -R $REPO -t "Refactor auth module" -b "Might be a bug or enhancement" -l bug,enhancement
gh issue create -R $REPO -t "Blocked but critical" -b "Waiting on API team" -l blocked,priority:critical
gh issue create -R $REPO -t "Won't fix but do it" -b "Legacy requirement" -l wontfix,priority:high

# 4 Stale issues (use demo:stale-14d to simulate staleness)
gh issue create -R $REPO -t "Update README" -b "Been meaning to do this" -l documentation,demo:stale-14d
gh issue create -R $REPO -t "Investigate memory usage" -b "Opened 2 weeks ago" -l investigation,demo:stale-14d
gh issue create -R $REPO -t "Add dark mode" -b "Community request" -l enhancement,demo:stale-14d
gh issue create -R $REPO -t "Flaky test in CI" -b "Fails sometimes" -l bug,flaky,demo:stale-14d

# 3 Overlapping deadlines (assigned to demo user)
gh issue create -R $REPO -t "Fix N+1 query in dashboard" -b "DEEP WORK: Performance critical. Look at DashboardService.java line 47." -l priority:critical,performance --assignee "@me"
gh issue create -R $REPO -t "Quarterly report due" -b "Due Friday" -l priority:high --assignee "@me"
gh issue create -R $REPO -t "Security audit findings" -b "Also due Friday" -l priority:critical,security --assignee "@me"

# 2 Clean quick-wins (will classify correctly for satisfying reshape)
gh issue create -R $REPO -t "Bump lodash to 4.17.21" -b "- [ ] Update package.json\n- [ ] Run tests\n\nDone when: CI green" -l quick-win,size:S,dependencies --assignee "@me"
gh issue create -R $REPO -t "Add alt text to logo image" -b "Accessibility fix. Done when: lighthouse score improves." -l quick-win,size:S,accessibility --assignee "@me"

# 2 Clean maintenance items (will classify correctly)
gh issue create -R $REPO -t "Update CONTRIBUTING.md" -b "Add section on running tests locally." -l documentation,maintenance --assignee "@me"
gh issue create -R $REPO -t "Remove unused CSS classes" -b "Cleanup from Q3 refactor." -l tech-debt,cleanup,maintenance --assignee "@me"

echo "✅ Seeded $(gh issue list -R $REPO --json number | jq length) issues for $REPO"
