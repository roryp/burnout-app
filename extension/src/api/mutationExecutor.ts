import { GitHubMutationPlan, GitHubAction } from './backend';
import { addLabels, removeLabels, comment, getIssueLabels, ghJson } from './ghCli';
import { isValidActionType } from './schemaValidation';
import { Issue } from '../constants/demoLabels';

const EXPECTED_SCHEMA_VERSION = 1;

export async function executeMutationPlan(plan: GitHubMutationPlan): Promise<void> {
    if ((plan as { schemaVersion?: number }).schemaVersion !== EXPECTED_SCHEMA_VERSION) {
        console.warn(`Schema version mismatch for mutation plan`);
    }

    for (const action of plan.actions) {
        if (!isValidActionType(action.type)) {
            console.warn(`Unknown action type: ${action.type}`);
            continue;
        }
        
        try {
            switch (action.type) {
                case 'AddLabels': {
                    const currentLabels = await getIssueLabels(plan.repo, action.issueNumber);
                    const labelsToAdd = action.labels.filter(l => !currentLabels.includes(l));
                    if (labelsToAdd.length > 0) {
                        await addLabels(plan.repo, action.issueNumber, labelsToAdd);
                    }
                    break;
                }
                case 'RemoveLabels': {
                    const existingLabels = await getIssueLabels(plan.repo, action.issueNumber);
                    const labelsToRemove = action.labels.filter(l => existingLabels.includes(l));
                    if (labelsToRemove.length > 0) {
                        await removeLabels(plan.repo, action.issueNumber, labelsToRemove);
                    }
                    break;
                }
                case 'Comment': 
                    await comment(plan.repo, action.issueNumber, action.body); 
                    break;
                default: {
                    const _exhaustive: never = action;
                    throw new Error(`Unhandled action type: ${(_exhaustive as GitHubAction).type}`);
                }
            }
        } catch (e) {
            console.error(`Action failed (continuing): ${action.type} #${action.issueNumber}`, e);
        }
    }
    
    if (plan.actions.length > 0) {
        await new Promise(resolve => setTimeout(resolve, 500));
    }
}

export async function resyncMutatedIssues(plan: GitHubMutationPlan): Promise<Issue[]> {
    const mutatedNumbers = [...new Set(plan.actions.map(a => a.issueNumber))];
    
    const refreshed: Issue[] = [];
    for (const num of mutatedNumbers) {
        const issue = await ghJson<Issue>([
            'issue', 'view', String(num), '-R', plan.repo, '--json', 
            'number,title,body,labels,assignees,createdAt,updatedAt,state,milestone'
        ]);
        refreshed.push(issue);
    }
    
    return refreshed;
}
