// API client configured to connect to MarketFlow backend via /api proxy or VITE_API_URL
const BASE_URL = (import.meta as any).env?.VITE_API_BASE_URL || '/api';

export const api = {
  // 1. Health Check
  async health(): Promise<{ status: string }> {
    const res = await fetch(`${BASE_URL}/health`);
    if (!res.ok) throw new Error(`Health check failed: ${res.statusText}`);
    return res.json();
  },

  // Ping backend to check if live server is running
  async checkConnection(): Promise<boolean> {
    try {
      const res = await fetch(`${BASE_URL}/health`, { signal: AbortSignal.timeout(2000) });
      return res.ok;
    } catch {
      return false;
    }
  },

  // 2. List Workflows
  async getWorkflows(): Promise<{ workflows: any[] }> {
    const res = await fetch(`${BASE_URL}/workflows`);
    if (!res.ok) throw new Error(`Failed to fetch workflows: ${res.statusText}`);
    return res.json();
  },

  // 3. Get Workflow By ID
  async getWorkflow(id: string): Promise<any> {
    const res = await fetch(`${BASE_URL}/workflows/${id}`);
    if (!res.ok) throw new Error(`Failed to fetch workflow ${id}: ${res.statusText}`);
    return res.json();
  },

  // 4. Create Workflow
  async createWorkflow(data: { name: string; description?: string; nodes: any[]; edges: any[] }): Promise<{ id: string; message: string }> {
    const res = await fetch(`${BASE_URL}/workflows`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error(`Failed to create workflow: ${res.statusText}`);
    return res.json();
  },

  // 5. Update Workflow
  async updateWorkflow(id: string, data: { name?: string; description?: string; nodes: any[]; edges: any[] }): Promise<{ id: string; message: string }> {
    const res = await fetch(`${BASE_URL}/workflows/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error(`Failed to update workflow: ${res.statusText}`);
    return res.json();
  },

  // 6. Delete Workflow
  async deleteWorkflow(id: string): Promise<{ message: string }> {
    const res = await fetch(`${BASE_URL}/workflows/${id}`, {
      method: 'DELETE',
    });
    if (!res.ok) throw new Error(`Failed to delete workflow: ${res.statusText}`);
    return res.json();
  },

  // 7. Execute Workflow
  async executeWorkflow(workflowId: string, inputPayload: Record<string, any>): Promise<{ executionId: string; workflowId: string; status: string }> {
    const res = await fetch(`${BASE_URL}/workflows/${workflowId}/execute`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ input: inputPayload }),
    });
    if (!res.ok) throw new Error(`Execution failed: ${res.statusText}`);
    return res.json();
  },

  // 8. Get Execution Status
  async getExecution(executionId: string): Promise<any> {
    const res = await fetch(`${BASE_URL}/executions/${executionId}`);
    if (!res.ok) throw new Error(`Failed to fetch execution: ${res.statusText}`);
    return res.json();
  },

  // 9. Generate Workflow with AI Prompt
  async generateWorkflowAI(prompt: string): Promise<{ workflow: any }> {
    const res = await fetch(`${BASE_URL}/ai/generate-workflow`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt }),
    });
    if (!res.ok) throw new Error(`AI generation failed: ${res.statusText}`);
    return res.json();
  },
};
