import type { ReshapeResponse, StressResponse, Issue } from './backend-client.js';

/**
 * Demo fallback data — exercises the BEFORE→AFTER reshape narrative
 * (stressScore = current chaos, expectedStressScore = projected after supervisor).
 * Used when the backend is unreachable or the repo isn't synced.
 */
export function getDemoReshapeData(repo: string): ReshapeResponse {
  return {
    status: 'demo',
    dayPlan: {
      deepWork: { number: 42, title: 'Implement caching layer', complexity: 8, labels: [{ name: 'priority:high' }] },
      quickWins: [
        { number: 18, title: 'Fix typo in README', complexity: 1, labels: [{ name: 'good-first-issue' }] },
        { number: 23, title: 'Update dependencies', complexity: 2, labels: [{ name: 'dependencies' }] },
        { number: 31, title: 'Add unit test for parser', complexity: 2, labels: [{ name: 'quick-win' }] },
      ],
      maintenance: [
        { number: 15, title: 'Refactor auth module', complexity: 5, labels: [{ name: 'refactor' }] },
        { number: 19, title: 'Database migration script', complexity: 4, labels: [{ name: 'maintenance' }] },
        { number: 27, title: 'CI pipeline optimization', complexity: 3, labels: [{ name: 'ci' }] },
      ],
      deferred: [
        { number: 8, title: 'Major feature X redesign', complexity: 13, labels: [{ name: 'epic' }] },
        { number: 12, title: 'Full performance audit', complexity: 8, labels: [{ name: 'feature' }] },
      ],
    },
    actionPlan: { repo, actions: [] },
    chaos: { score: 6, unassignedUrgentCount: 1, afterHoursSignal: false },
    compliance: { isCompliant: true, violations: [] },
    stressScore: 58,
    stressLevel: 'HIGH',
    expectedStressScore: 32,
    fridayScore: 78,
    agentExplanation: `📊 **Demo Mode** — backend unreachable.

Sample 3-3-3 plan for **${repo}**:

🎯 **Deep Work**: #42 Implement caching layer
⚡ **Quick Wins**: 3 small tasks
🔧 **Maintenance**: 3 routine items
📦 **Deferred**: 2 low-priority items

Connect the backend (sync the repo) for real data.`,
    protectiveTriggered: false,
    protectiveMessage: '',
    llmEnabled: false,
  };
}

export function getDemoStressData(): StressResponse {
  return {
    stressScore: 42,
    stressLevel: 'MODERATE',
    breakdown: {
      workload: 12,
      chaos: 8,
      contextSwitching: 6,
      clarity: 4,
      sustained: 8,
      afterHours: 4,
    },
    is333Compliant: false,
    initialStressScore: 42,
    expectedStressScore: 12,
  };
}

export function getDemoIssues(): Issue[] {
  return [
    { number: 42, title: 'Implement caching layer', labels: [{ name: 'enhancement' }], complexity: 8 },
    { number: 18, title: 'Fix typo in README', labels: [{ name: 'documentation' }], complexity: 1 },
    { number: 23, title: 'Update dependencies', labels: [{ name: 'dependencies' }], complexity: 2 },
    { number: 31, title: 'Add unit test for parser', labels: [{ name: 'testing' }], complexity: 2 },
    { number: 15, title: 'Refactor auth module', labels: [{ name: 'refactor' }], complexity: 5 },
    { number: 19, title: 'Database migration script', labels: [{ name: 'database' }], complexity: 4 },
    { number: 27, title: 'CI pipeline optimization', labels: [{ name: 'ci' }], complexity: 3 },
    { number: 8, title: 'Major feature X redesign', labels: [{ name: 'enhancement' }], complexity: 13 },
    { number: 12, title: 'Full performance audit', labels: [{ name: 'performance' }], complexity: 8 },
  ];
}
