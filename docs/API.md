# MARKETFLOW API
 
Live Cloud Base URL (Render):
 
```text
https://marketflow-pb8i.onrender.com/api
```

Local Development Base URL:

```text
http://localhost:8080/api
```

## Authentication

If authentication is enabled:

```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

## 1. Health Check

### GET `/health`

Response:

```json
{
  "status": "ok"
}
```

## 2. List Workflows

### GET `/workflows`

Response:

```json
{
  "workflows": [
    {
      "id": "wf_001",
      "name": "Instagram Lead Qualification",
      "status": "active",
      "updatedAt": "2026-10-04T10:00:00Z"
    }
  ]
}
```

## 3. Get Workflow

### GET `/workflows/:id`

Response:

```json
{
  "id": "wf_001",
  "name": "Instagram Lead Qualification",
  "status": "active",
  "nodes": [],
  "edges": []
}
```

## 4. Create Workflow

### POST `/workflows`

Request:

```json
{
  "name": "Instagram Lead Qualification",
  "description": "Qualify new Instagram leads and route them based on score.",
  "nodes": [],
  "edges": []
}
```

Response:

```json
{
  "id": "wf_001",
  "message": "Workflow created"
}
```

## 5. Update Workflow

### PUT `/workflows/:id`

Request:

```json
{
  "name": "Instagram Lead Qualification",
  "nodes": [],
  "edges": []
}
```

Response:

```json
{
  "id": "wf_001",
  "message": "Workflow updated"
}
```

## 6. Delete Workflow

### DELETE `/workflows/:id`

Response:

```json
{
  "message": "Workflow deleted"
}
```

## 7. Execute Workflow

### POST `/workflows/:id/execute`

Request:

```json
{
  "input": {
    "leadName": "Aarav",
    "email": "aarav@example.com",
    "source": "instagram"
  }
}
```

Response:

```json
{
  "executionId": "exec_001",
  "workflowId": "wf_001",
  "status": "running"
}
```

## 8. Get Execution

### GET `/executions/:id`

Response:

```json
{
  "id": "exec_001",
  "workflowId": "wf_001",
  "status": "completed",
  "startedAt": "2026-10-04T10:30:00Z",
  "completedAt": "2026-10-04T10:30:03Z",
  "steps": [
    {
      "nodeId": "node_1",
      "status": "completed"
    }
  ]
}
```

## 9. Generate Workflow with AI

### POST `/ai/generate-workflow`

Request:

```json
{
  "prompt": "When a new Instagram lead arrives, score it and notify sales if the score is above 70."
}
```

Response:

```json
{
  "workflow": {
    "name": "Instagram Lead Qualification",
    "nodes": [],
    "edges": []
  }
}
```

> AI generation is an optional product innovation layer. The core PS functionality must remain usable without it.

## 10. Error Format

All errors should follow a consistent structure:

```json
{
  "error": {
    "code": "WORKFLOW_NOT_FOUND",
    "message": "Workflow does not exist."
  }
}
```
