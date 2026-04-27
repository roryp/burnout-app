#!/bin/bash
# Label Bootstrap Script for Burnout Demo
# Run: bash scripts/setup-labels.sh

REPO="${1:-roryp/burnout-demo}"

echo "Creating labels for $REPO..."

# Demo labels (signals for demo mode)
gh label create "demo:stale-14d" -R $REPO --color "808080" --description "Demo: simulate 14-day staleness" --force
gh label create "demo:after-hours" -R $REPO --color "ffa500" --description "Demo: simulate late-night work" --force
gh label create "demo:touched-today" -R $REPO --color "1d76db" --description "Demo: simulate context switching" --force
gh label create "demo:friday" -R $REPO --color "5319e7" --description "Demo: Friday deploy scenario" --force

# 3-3-3 workflow labels
gh label create "deep-work" -R $REPO --color "d73a4a" --description "3-3-3: Deep focus work" --force
gh label create "quick-win" -R $REPO --color "0e8a16" --description "3-3-3: Quick win (<30min)" --force
gh label create "maintenance" -R $REPO --color "c5def5" --description "3-3-3: Routine maintenance" --force
gh label create "deferred" -R $REPO --color "cfd3d7" --description "3-3-3: Deferred to next sprint" --force
gh label create "delegated" -R $REPO --color "f9d0c4" --description "Marked for delegation" --force
gh label create "needs-owner" -R $REPO --color "fbca04" --description "Needs someone to own" --force
gh label create "needs-scope" -R $REPO --color "d93f0b" --description "Needs clearer definition" --force
gh label create "blocked" -R $REPO --color "b60205" --description "Blocked on something" --force
gh label create "focus" -R $REPO --color "6f42c1" --description "Today's focus item" --force
gh label create "next-sprint" -R $REPO --color "bfd4f2" --description "Scheduled for next sprint" --force
gh label create "3-3-3" -R $REPO --color "0052cc" --description "Part of 3-3-3 plan" --force

# Priority labels
gh label create "priority:critical" -R $REPO --color "b60205" --description "Critical priority" --force
gh label create "priority:high" -R $REPO --color "d93f0b" --description "High priority" --force
gh label create "priority:low" -R $REPO --color "0e8a16" --description "Low priority" --force
gh label create "urgent" -R $REPO --color "ff0000" --description "Urgent" --force

# Size labels
gh label create "size:S" -R $REPO --color "c2e0c6" --description "Small (< 1 day)" --force
gh label create "size:M" -R $REPO --color "fef2c0" --description "Medium (1-3 days)" --force
gh label create "size:L" -R $REPO --color "f9d0c4" --description "Large (> 3 days)" --force
gh label create "scope-defined" -R $REPO --color "bfdadc" --description "Scope is clear" --force

# Category labels
gh label create "documentation" -R $REPO --color "0075ca" --description "Documentation work" --force
gh label create "tech-debt" -R $REPO --color "d4c5f9" --description "Technical debt" --force
gh label create "cleanup" -R $REPO --color "e4e669" --description "Cleanup task" --force
gh label create "routine" -R $REPO --color "c5def5" --description "Routine work" --force
gh label create "architecture" -R $REPO --color "5319e7" --description "Architecture work" --force
gh label create "security" -R $REPO --color "b60205" --description "Security related" --force
gh label create "performance" -R $REPO --color "ff7619" --description "Performance work" --force

# Additional workflow labels
gh label create "bug" -R $REPO --color "d73a4a" --description "Bug" --force
gh label create "enhancement" -R $REPO --color "a2eeef" --description "Enhancement" --force
gh label create "wontfix" -R $REPO --color "ffffff" --description "Won't fix" --force
gh label create "investigation" -R $REPO --color "d4c5f9" --description "Investigation needed" --force
gh label create "flaky" -R $REPO --color "fbca04" --description "Flaky test" --force
gh label create "dependencies" -R $REPO --color "0366d6" --description "Dependency updates" --force
gh label create "accessibility" -R $REPO --color "a2eeef" --description "Accessibility improvements" --force
gh label create "triage" -R $REPO --color "d876e3" --description "Needs triage" --force
gh label create "chore" -R $REPO --color "fef2c0" --description "Chore/housekeeping" --force
gh label create "refactor" -R $REPO --color "d4c5f9" --description "Refactoring" --force
gh label create "ci" -R $REPO --color "000000" --description "CI/CD related" --force
gh label create "devops" -R $REPO --color "006b75" --description "DevOps work" --force

echo "✅ All labels created for $REPO"
