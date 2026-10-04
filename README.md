# MARKETFLOW

> **AI-powered visual workflow automation for marketing teams.**  
> Turn a marketing process described in plain English into a visual, executable workflow.

## Problem Statement

**ALG-AUTO-01 — Visual Workflow Automation**

ALG-AUTO-01 requires a visual workflow builder where users can create workflows using triggers, actions, conditions and data transformation, execute workflows, view execution status/logs, and save/load workflows.

## Why MARKETFLOW?

Marketing teams repeatedly move leads and campaign data between tools and manually coordinate repetitive processes.

MARKETFLOW gives marketing executives one visual command center to:

- Build workflows visually
- Generate workflows from natural-language instructions
- Connect triggers, actions and conditions
- Execute workflows
- Monitor execution status
- Inspect failures and logs
- Save and reuse workflows

## Core Demo

Example:

**"When a new Instagram lead arrives, qualify it, send high-quality leads to sales, and put low-quality leads into a nurturing sequence."**

MARKETFLOW converts that instruction into:

```text
Instagram Lead
      ↓
AI Lead Qualification
      ↓
   Score > 70?
    /       \
  YES       NO
   ↓         ↓
Notify      Nurture
Sales       Email
```

## Tech Stack

- **Frontend:** Next.js / React
- **Workflow Canvas:** React Flow
- **Styling:** Tailwind CSS
- **Backend:** Node.js / Express
- **Database:** PostgreSQL / Supabase
- **AI:** OpenAI API (optional innovation layer)
- **API:** REST
- **Deployment:** Vercel + backend hosting of choice

## MVP Scope

### Must Have

- Visual workflow editor
- Triggers
- Actions
- Conditions
- Data transformation
- Workflow execution
- Execution status/logs
- Save/load workflows

### Product Layer

- Marketing-focused templates
- AI workflow generation
- Execution dashboard
- Human-readable execution logs
- Failure/retry states

## Prerequisites

- Node.js 20+
- npm
- PostgreSQL or Supabase project
- API keys configured through `.env`
- Git

## Local Setup

### 1. Clone

```bash
git clone <REPOSITORY_URL>
cd marketflow
```

### 2. Install frontend

```bash
cd frontend
npm install
```

### 3. Install backend

```bash
cd ../backend
npm install
```

### 4. Configure environment

```bash
cp .env.example .env
```

Fill in the required values.

### 5. Start backend

```bash
cd backend
npm run dev
```

### 6. Start frontend

In another terminal:

```bash
cd frontend
npm run dev
```

### 7. Open

```text
Frontend: http://localhost:3000
Backend:  http://localhost:5000
```

## Repository Structure

```text
marketflow/
├── frontend/
├── backend/
├── docs/
│   ├── API.md
│   ├── SCHEMA.md
│   ├── ARCHITECTURE.md
│   ├── ROADMAP.md
│   └── DEMO.md
├── .env.example
├── CONTRIBUTING.md
└── README.md
```

## Team

| Member | Role | Responsibility |
|---|---|---|
| Member 1 | Product / Full Stack | Architecture + integration |
| Member 2 | Frontend | Dashboard + workflow canvas |
| Member 3 | Backend | Execution engine + APIs |
| Member 4 | AI / UX | AI workflow generation + UX |

Replace the placeholder names before submission.

## Hackathon Alignment

The project is designed around the required ALG-AUTO-01 workflow capabilities while adding a marketing-specific product experience.

See:

- `docs/API.md`
- `docs/SCHEMA.md`
- `docs/ARCHITECTURE.md`
- `docs/ROADMAP.md`
- `docs/DEMO.md`
