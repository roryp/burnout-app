#!/bin/bash
# Issue Seeding Script for Burnout Demo
# Run: bash scripts/seed-issues.sh [owner/repo]
# Creates issues that trigger ALL stress signals in the burnout algorithm.

REPO="${1:-roryp/burnout-demo}"

echo "Seeding chaotic issues for $REPO..."

# --- Deep Work (assigned, triggers workload) ---
gh issue create -R $REPO -t "Refactor authentication module" -b "DEEP WORK: Security critical. Requires 2-3 hours of focused work." -l deep-work --assignee "@me"
gh issue create -R $REPO -t "Redesign database schema" -b "DEEP WORK: Need to normalize tables and migrate data." -l deep-work,architecture --assignee "@me"

# --- Quick Wins (assigned, 3-3-3 bucket) ---
gh issue create -R $REPO -t "Bump lodash to 4.17.21" -b "Quick security fix. Update package.json and run tests." -l quick-win,size:S --assignee "@me"
gh issue create -R $REPO -t "Add alt text to logo" -b "Accessibility fix for screen readers." -l quick-win,size:S --assignee "@me"
gh issue create -R $REPO -t "Fix typo in README" -b "Line 42 has a spelling error." -l quick-win,size:S --assignee "@me"
gh issue create -R $REPO -t "Fix broken link in docs" -b "The API reference link 404s." -l quick-win,size:S --assignee "@me"
gh issue create -R $REPO -t "Fix typo in docs" -b "Minor doc fix" -l quick-win,size:S --assignee "@me"

# --- Maintenance (assigned, 3-3-3 bucket) ---
gh issue create -R $REPO -t "Update CONTRIBUTING.md" -b "Add testing section" -l maintenance --assignee "@me"
gh issue create -R $REPO -t "Remove unused CSS" -b "Cleanup from refactor" -l maintenance --assignee "@me"
gh issue create -R $REPO -t "Update dependencies" -b "Routine maintenance" -l maintenance --assignee "@me"

# --- Mystery Meat: assigned issues with empty body (triggers Clarity + Unclear Quick Wins) ---
gh issue create -R $REPO -t "URGENT: Production is slow" -b "" -l urgent --assignee "@me"
gh issue create -R $REPO -t "Fix the thing" -b "" -l urgent --assignee "@me"
gh issue create -R $REPO -t "ASAP: Customer unhappy" -b "" -l urgent,priority:critical --assignee "@me"
gh issue create -R $REPO -t "!!!" -b "" -l urgent --assignee "@me"
gh issue create -R $REPO -t "Needs fixing" -b "" -l quick-win --assignee "@me"

# --- Context Switching: assigned + demo:touched-today (triggers Context Switching) ---
gh issue create -R $REPO -t "Review PR #42" -b "Code review needed" -l demo:touched-today --assignee "@me"
gh issue create -R $REPO -t "Respond to security alert" -b "Dependabot alert" -l demo:touched-today,urgent --assignee "@me"
gh issue create -R $REPO -t "Fix CI pipeline" -b "Build is red" -l demo:touched-today,priority:high --assignee "@me"
gh issue create -R $REPO -t "Answer Slack question" -b "Team needs clarification" -l demo:touched-today --assignee "@me"
gh issue create -R $REPO -t "Triage new bug report" -b "Customer filed #99" -l demo:touched-today,triage --assignee "@me"
gh issue create -R $REPO -t "Update staging env" -b "Deploy latest" -l demo:touched-today --assignee "@me"
gh issue create -R $REPO -t "Hotfix for login page" -b "Users reporting errors" -l demo:touched-today,priority:critical --assignee "@me"

# --- After Hours: assigned + demo:after-hours (triggers After Hours) ---
gh issue create -R $REPO -t "Late night deploy rollback" -b "Had to rollback at 11pm" -l demo:after-hours --assignee "@me"
gh issue create -R $REPO -t "Weekend incident response" -b "PagerDuty woke me up" -l demo:after-hours,priority:critical --assignee "@me"

# --- Contradictory labels (triggers chaos) ---
gh issue create -R $REPO -t "Refactor auth module" -b "Might be a bug or enhancement" -l bug,enhancement
gh issue create -R $REPO -t "Blocked but critical" -b "Waiting on API team" -l blocked,priority:critical
gh issue create -R $REPO -t "Won't fix but do it" -b "Legacy requirement" -l wontfix,priority:high

# --- Stale issues (triggers staleness via demo label) ---
gh issue create -R $REPO -t "Investigate memory usage" -b "Opened 2 weeks ago" -l investigation,demo:stale-14d
gh issue create -R $REPO -t "Add dark mode" -b "Community request" -l enhancement,demo:stale-14d
gh issue create -R $REPO -t "Flaky test in CI" -b "Fails sometimes" -l bug,flaky,demo:stale-14d

echo "✅ Seeded $(gh issue list -R $REPO --json number | jq length) issues for $REPO"
