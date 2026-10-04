# Contributing to MARKETFLOW

This project is being developed under a 24-hour hackathon timeline. The goal is to move quickly without creating merge conflicts or breaking the demo.

## Branching Strategy

Never work directly on `main`.

```text
main
├── feature/ui
├── feature/workflow-engine
├── feature/ai-workflows
├── feature/dashboard
├── fix/execution-log
└── chore/docs
```

### Naming

```text
feature/<short-name>
fix/<short-name>
chore/<short-name>
```

Examples:

```text
feature/workflow-canvas
feature/ai-generator
fix/workflow-save
```

## Pull Request Workflow

1. Create a branch from `main`.
2. Make focused changes.
3. Test locally.
4. Commit with a meaningful message.
5. Push the branch.
6. Open a PR.
7. At least one teammate reviews it.
8. Merge into `main`.
9. Pull the latest `main` before starting the next task.

## Commit Convention

Use:

```text
feat: add workflow execution engine
feat: add AI workflow generation
fix: handle failed action execution
ui: improve workflow canvas
docs: update API contract
chore: update dependencies
```

Avoid:

```text
final
final2
changes
working
asdfgh
```

## Ports

| Service | Port |
|---|---:|
| Frontend | 3000 |
| Backend API | 5000 |
| PostgreSQL | 5432 |

## API Rule

Frontend developers should use the documented API contract in `docs/API.md`.

Do not silently change request/response formats. If an API changes:

1. Update the backend.
2. Update `docs/API.md`.
3. Inform the frontend owner.

## Database Rule

Schema changes must be reflected in `docs/SCHEMA.md`.

## 24-Hour Hackathon Rule

Prioritize:

1. Core workflow execution
2. Reliable demo path
3. UI polish
4. AI enhancement
5. Stretch features

Do not introduce a new framework late in the hackathon unless it solves a blocking issue.
