# MARKETFLOW Data Schema

The schema is intentionally small for the 24-hour MVP.

## Entity Relationship Overview

```text
User
 │
 ├──< Workflow
 │       │
 │       └──< Execution
 │
 └──< WorkflowTemplate
```

## users

| Field | Type | Key | Description |
|---|---|---|---|
| id | UUID | PK | User identifier |
| name | VARCHAR | | Display name |
| email | VARCHAR | UNIQUE | Login email |
| password_hash | TEXT | | Hashed password |
| created_at | TIMESTAMP | | Creation time |

## workflows

| Field | Type | Key | Description |
|---|---|---|---|
| id | UUID | PK | Workflow identifier |
| user_id | UUID | FK | Owner |
| name | VARCHAR | | Workflow name |
| description | TEXT | | Workflow description |
| status | VARCHAR | | draft/active/paused |
| definition | JSONB | | Nodes and edges |
| created_at | TIMESTAMP | | Creation time |
| updated_at | TIMESTAMP | | Last update |

### `definition`

Stores the visual workflow:

```json
{
  "nodes": [
    {
      "id": "node_1",
      "type": "trigger",
      "label": "New Lead"
    }
  ],
  "edges": [
    {
      "source": "node_1",
      "target": "node_2"
    }
  ]
}
```

## executions

| Field | Type | Key | Description |
|---|---|---|---|
| id | UUID | PK | Execution identifier |
| workflow_id | UUID | FK | Workflow executed |
| status | VARCHAR | | queued/running/completed/failed |
| input_data | JSONB | | Execution input |
| output_data | JSONB | | Execution output |
| started_at | TIMESTAMP | | Start time |
| completed_at | TIMESTAMP | | End time |

## execution_steps

| Field | Type | Key | Description |
|---|---|---|---|
| id | UUID | PK | Step identifier |
| execution_id | UUID | FK | Parent execution |
| node_id | VARCHAR | | Workflow node |
| status | VARCHAR | | pending/running/completed/failed |
| input_data | JSONB | | Step input |
| output_data | JSONB | | Step output |
| error_message | TEXT | | Failure details |
| started_at | TIMESTAMP | | Start time |
| completed_at | TIMESTAMP | | Completion time |

## workflow_templates

| Field | Type | Key | Description |
|---|---|---|---|
| id | UUID | PK | Template identifier |
| name | VARCHAR | | Template name |
| category | VARCHAR | | Marketing category |
| description | TEXT | | Template description |
| definition | JSONB | | Workflow definition |

## Relationships

```text
users.id
   │
   └──── workflows.user_id

workflows.id
   │
   └──── executions.workflow_id

executions.id
   │
   └──── execution_steps.execution_id
```

## MVP Design Decision

The visual workflow itself is stored as JSON rather than creating a separate relational table for every node and edge. This keeps the hackathon implementation simple and allows the React Flow representation to be persisted directly.
