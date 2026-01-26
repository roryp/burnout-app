import * as vscode from 'vscode';

// === SCHEMA VERSION VALIDATION (catches backend/extension drift) ===
const EXPECTED_SCHEMA_VERSIONS: Record<string, number> = {
    ChaosMetrics: 1,
    ComplianceReport: 1,
    StressResponse: 1,
    GitHubMutationPlan: 1,
    GoapActionSummary: 1,
    FridayScoreResponse: 1,
    ReshapeResponse: 2,  // Updated: includes agent explanation fields
};

interface SchemaVersioned {
    schemaVersion?: number;
}

export function validateSchemaVersion<T extends SchemaVersioned>(
    data: T, 
    type: keyof typeof EXPECTED_SCHEMA_VERSIONS
): T {
    const expected = EXPECTED_SCHEMA_VERSIONS[type];
    const actual = data.schemaVersion;
    
    if (actual !== expected) {
        const msg = `Schema mismatch for ${type}: expected v${expected}, got v${actual ?? 'undefined'}. ` +
                    `Extension and backend may be out of sync. Rebuild both.`;
        vscode.window.showWarningMessage(`⚠️ ${msg}`);
        console.error(`[Schema Drift] ${msg}`);
    }
    
    return data;
}

export function isValidActionType(type: string): type is 'AddLabels' | 'RemoveLabels' | 'Comment' {
    return ['AddLabels', 'RemoveLabels', 'Comment'].includes(type);
}
