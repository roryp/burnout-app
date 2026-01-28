import type { ReshapeResponse, StressResponse, Issue } from './backend-client.js';

export function getDemoReshapeData(repo: string): ReshapeResponse {
  return {
    dayPlan: {
      deepWork: { number: 42, title: 'Implement caching layer', complexity: 8 },
      quickWins: [
        { number: 18, title: 'Fix typo in README', complexity: 1 },
        { number: 23, title: 'Update dependencies', complexity: 2 },
        { number: 31, title: 'Add unit test for parser', complexity: 2 },
      ],
      maintenance: [
        { number: 15, title: 'Refactor auth module', complexity: 5 },
        { number: 19, title: 'Database migration script', complexity: 4 },
        { number: 27, title: 'CI pipeline optimization', complexity: 3 },
      ],
      deferred: [
        { number: 8, title: 'Major feature X redesign', complexity: 13 },
        { number: 12, title: 'Full performance audit', complexity: 8 },
      ],
    },
    stressScore: 35,
    fridayScore: 78,
    agentExplanation: `📊 **Demo Mode** - Backend not available

Here's a sample 3-3-3 plan for **${repo}**:

🎯 **Deep Work**: Issue #42 (Implement caching layer)
⚡ **Quick Wins**: 3 small tasks
🔧 **Maintenance**: 3 routine items
📦 **Deferred**: 2 low-priority items

*Connect the backend for real data!*`,
  };
}

export function getDemoStressData(): StressResponse {
  return {
    stressScore: 42,
    stressLevel: 'MODERATE',
    is333Compliant: false,
  };
}

export function getDemoIssues(): Issue[] {
  return [
    { number: 42, title: 'Implement caching layer', labels: ['enhancement'], complexity: 8 },
    { number: 18, title: 'Fix typo in README', labels: ['documentation'], complexity: 1 },
    { number: 23, title: 'Update dependencies', labels: ['dependencies'], complexity: 2 },
    { number: 31, title: 'Add unit test for parser', labels: ['testing'], complexity: 2 },
    { number: 15, title: 'Refactor auth module', labels: ['refactor'], complexity: 5 },
    { number: 19, title: 'Database migration script', labels: ['database'], complexity: 4 },
    { number: 27, title: 'CI pipeline optimization', labels: ['ci'], complexity: 3 },
    { number: 8, title: 'Major feature X redesign', labels: ['enhancement'], complexity: 13 },
    { number: 12, title: 'Full performance audit', labels: ['performance'], complexity: 8 },
  ];
}
