# MARKETFLOW Demo Guide

## Judge Demo Goal

Demonstrate that a marketing executive can:

1. Understand the dashboard.
2. Create or generate a workflow.
3. Inspect the workflow visually.
4. Execute it.
5. Observe execution status.
6. Inspect logs.
7. Understand what happened when an action fails.

## Demo Account

Pre-seeded demo credentials for judges and evaluation:

```text
Email: judge@marketflow.demo
Password: JudgeDemo2026!
Role: ROLE_USER
```

## Deployed Links

```text
Frontend: <DEPLOYED_FRONTEND_URL>
Backend:  https://marketflow-pb8i.onrender.com/api
Swagger:  https://marketflow-pb8i.onrender.com/api/swagger-ui/index.html
Health:   https://marketflow-pb8i.onrender.com/api/health
Repository: https://github.com/Hxmblevishu/MARKETFLOW
Video: <LOOM_OR_VIDEO_URL>
Slides: <SLIDE_DECK_URL>
```

## Recommended 3-Minute Clickthrough

### Step 1 — Dashboard

Open the dashboard.

Show:

- Active workflows
- Recent executions
- Success rate
- Time saved
- Marketing workflow templates

Do not spend too long here.

### Step 2 — AI Workflow Generation

Navigate to **Create Workflow**.

Enter:

```text
When a new Instagram lead arrives, qualify the lead.
If the score is above 70, notify the sales team.
Otherwise add the lead to a nurturing email sequence.
```

Click:

**Generate Workflow**

The generated workflow should appear on the canvas.

### Step 3 — Inspect Workflow

Show:

```text
Instagram Lead
      ↓
AI Qualification
      ↓
Score > 70?
    /     \
  YES      NO
   ↓        ↓
Sales     Nurture
```

Briefly explain that every node is editable.

### Step 4 — Execute

Click:

**Run Workflow**

Use seeded input:

```json
{
  "name": "Aarav Sharma",
  "source": "Instagram",
  "company": "DemoCorp",
  "email": "aarav@example.com"
}
```

### Step 5 — Show Live Execution

The UI should visually update:

```text
✓ Lead received
✓ Lead qualified
✓ Score calculated: 87
✓ Sales notification sent
✓ CRM record updated
```

### Step 6 — Show Logs

Open the execution details.

Show:

- Start time
- Each executed node
- Input/output
- Duration
- Status

### Step 7 — Edge Case

Run a seeded failure scenario.

Example:

```text
Email service unavailable
```

Show:

```text
⚠ Notification failed

Reason:
External email service unavailable.

Actions:
[Retry] [Skip] [View logs]
```

Explain that failures are visible rather than silently disappearing.

## Backup Demo

If the deployed backend fails:

1. Use the local deployment.
2. Use pre-seeded workflow JSON.
3. Use a recorded demo video.
4. Show the architecture and API documentation.

## Judge Talking Points

### Problem

Marketing teams repeat multi-step processes across tools.

### Solution

MARKETFLOW turns those processes into visual, executable workflows.

### Innovation

Natural-language instructions can be converted into editable workflows.

### Technical & Enterprise Architecture

The system has a resilient DAG workflow execution engine, dynamic JSONPath variable evaluation, live WebSocket execution streaming (`/topic/executions/{id}`), SHA-256 workflow bundle export/import, and step-level execution logs.

### Enterprise Security & Multi-Device Session Management

- **JWT Token Rotation & Multi-Device Logout:** 15-minute access tokens with single-use rotating 7-day refresh tokens. Independent session termination: logging out on one device preserves other active devices.
- **IDOR / BOLA Defense:** Strict multi-tenant isolation ensuring unauthorized users cannot read, modify, or delete another user's workflows (RFC 7807 403 Forbidden).
- **SSRF Outbound Guard:** Prevents cloud metadata theft (`169.254.169.254`) and internal private network scanning from HTTP nodes.

### UX

The interface is designed for a marketing executive rather than a developer.

## Final Demo Checklist

- [ ] Frontend deployed
- [ ] Backend deployed
- [ ] Database reachable
- [ ] Demo account works
- [ ] Seed workflows exist
- [ ] AI key works
- [ ] Workflow executes
- [ ] Failure scenario works
- [ ] Logs display correctly
- [ ] Repository is public/private as required
- [ ] README is updated
- [ ] Backup video works
