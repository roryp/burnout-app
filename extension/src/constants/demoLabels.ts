export const DemoLabels = {
    TOUCHED_TODAY: 'demo:touched-today',
    AFTER_HOURS: 'demo:after-hours',
    STALE_14D: 'demo:stale-14d',
    FRIDAY: 'demo:friday',
} as const;

export const ALL_DEMO_LABELS = Object.values(DemoLabels);

export interface Issue {
    number: number;
    title: string;
    body: string | null;
    labels: { name: string }[];
    assignees: { login: string }[];
    createdAt: string;
    updatedAt: string;
    state: string;
    milestone: { title: string; dueOn: string } | null;
}

export function hasDemoLabel(issue: Issue): boolean {
    return issue.labels?.some(l => l.name.toLowerCase().startsWith('demo:')) ?? false;
}
