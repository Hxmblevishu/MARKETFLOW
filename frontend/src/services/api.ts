// API client configured to connect to MarketFlow Spring Boot backend via /api proxy or VITE_API_BASE_URL
const BASE_URL = (import.meta as any).env?.VITE_API_BASE_URL || '/api';

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  role: string;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
}

export const authStorage = {
  getAccessToken: (): string | null => localStorage.getItem('marketflow_access_token'),
  getRefreshToken: (): string | null => localStorage.getItem('marketflow_refresh_token'),
  getUser: (): UserProfile | null => {
    try {
      const u = localStorage.getItem('marketflow_user');
      return u ? JSON.parse(u) : null;
    } catch {
      return null;
    }
  },
  setSession: (session: AuthSession): void => {
    localStorage.setItem('marketflow_access_token', session.accessToken);
    localStorage.setItem('marketflow_refresh_token', session.refreshToken);
    localStorage.setItem('marketflow_user', JSON.stringify(session.user));
  },
  clearSession: (): void => {
    localStorage.removeItem('marketflow_access_token');
    localStorage.removeItem('marketflow_refresh_token');
    localStorage.removeItem('marketflow_user');
  },
};

// Authenticated fetch wrapper with automatic token injection and transparent token refresh
async function authFetch(url: string, options: RequestInit = {}): Promise<Response> {
  let token = authStorage.getAccessToken();

  // If no token exists, auto-authenticate with pre-seeded demo judge account
  if (!token) {
    try {
      await api.loginDemo();
      token = authStorage.getAccessToken();
    } catch {
      // Continue without token if backend is offline
    }
  }

  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let res = await fetch(url, { ...options, headers });

  // If 401 Unauthorized, attempt refresh token rotation or demo re-auth
  if (res.status === 401) {
    const refreshToken = authStorage.getRefreshToken();
    let refreshed = false;

    if (refreshToken) {
      try {
        const refreshRes = await fetch(`${BASE_URL}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });
        if (refreshRes.ok) {
          const data = await refreshRes.json();
          authStorage.setSession(data.data || data);
          token = authStorage.getAccessToken();
          refreshed = true;
        }
      } catch {
        refreshed = false;
      }
    }

    // Fallback: Re-authenticate with Demo Judge account
    if (!refreshed) {
      try {
        await api.loginDemo();
        token = authStorage.getAccessToken();
        refreshed = true;
      } catch {
        refreshed = false;
      }
    }

    if (refreshed && token) {
      headers.set('Authorization', `Bearer ${token}`);
      res = await fetch(url, { ...options, headers });
    }
  }

  return res;
}

export const api = {
  // 1. Health & Connection Checks
  async health(): Promise<{ status: string }> {
    const res = await fetch(`${BASE_URL}/health`);
    if (!res.ok) throw new Error(`Health check failed: ${res.statusText}`);
    return res.json();
  },

  async checkConnection(): Promise<boolean> {
    try {
      const res = await fetch(`${BASE_URL}/health`, { signal: AbortSignal.timeout(3000) });
      return res.ok;
    } catch {
      return false;
    }
  },

  // 2. Authentication & Multi-Device Logout
  async login(email: string, password: string): Promise<AuthSession> {
    const res = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error?.message || `Login failed: ${res.statusText}`);
    }
    const data = await res.json();
    const session: AuthSession = data.data || data;
    authStorage.setSession(session);
    return session;
  },

  async register(email: string, password: string, name: string): Promise<AuthSession> {
    const res = await fetch(`${BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, name }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.error?.message || `Registration failed: ${res.statusText}`);
    }
    const data = await res.json();
    const session: AuthSession = data.data || data;
    authStorage.setSession(session);
    return session;
  },

  async loginDemo(): Promise<AuthSession> {
    return this.login('judge@marketflow.demo', 'JudgeDemo2026!');
  },

  async logout(thisDeviceOnly = true): Promise<void> {
    const refreshToken = authStorage.getRefreshToken();
    try {
      if (thisDeviceOnly) {
        // Single device logout: only revokes the token belonging to this current device
        if (refreshToken) {
          await fetch(`${BASE_URL}/auth/logout`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
          });
        }
      } else {
        // Multi-device logout: terminates all active sessions across all devices
        await authFetch(`${BASE_URL}/auth/logout-all`, { method: 'POST' });
      }
    } finally {
      authStorage.clearSession();
    }
  },

  async getCurrentUser(): Promise<UserProfile> {
    const res = await authFetch(`${BASE_URL}/auth/me`);
    if (!res.ok) throw new Error(`Failed to fetch user profile: ${res.statusText}`);
    const data = await res.json();
    return data.data || data;
  },

  // 3. Workflows Studio Endpoints
  async getWorkflows(): Promise<{ workflows: any[] }> {
    const res = await authFetch(`${BASE_URL}/workflows`);
    if (!res.ok) throw new Error(`Failed to fetch workflows: ${res.statusText}`);
    return res.json();
  },

  async getWorkflow(id: string): Promise<any> {
    const res = await authFetch(`${BASE_URL}/workflows/${id}`);
    if (!res.ok) throw new Error(`Failed to fetch workflow ${id}: ${res.statusText}`);
    return res.json();
  },

  async createWorkflow(data: { name: string; description?: string; nodes: any[]; edges: any[] }): Promise<{ id: string; name: string; status: string }> {
    const res = await authFetch(`${BASE_URL}/workflows`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error(`Failed to create workflow: ${res.statusText}`);
    return res.json();
  },

  async updateWorkflow(id: string, data: { name?: string; description?: string; nodes?: any[]; edges?: any[]; status?: string }): Promise<any> {
    const res = await authFetch(`${BASE_URL}/workflows/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error(`Failed to update workflow: ${res.statusText}`);
    return res.json();
  },

  async deleteWorkflow(id: string): Promise<{ message: string }> {
    const res = await authFetch(`${BASE_URL}/workflows/${id}`, {
      method: 'DELETE',
    });
    if (!res.ok) throw new Error(`Failed to delete workflow: ${res.statusText}`);
    return res.json();
  },

  async duplicateWorkflow(id: string): Promise<any> {
    const res = await authFetch(`${BASE_URL}/workflows/${id}/duplicate`, {
      method: 'POST',
    });
    if (!res.ok) throw new Error(`Failed to duplicate workflow: ${res.statusText}`);
    return res.json();
  },

  async exportWorkflow(id: string): Promise<any> {
    const res = await authFetch(`${BASE_URL}/workflows/${id}/export`);
    if (!res.ok) throw new Error(`Failed to export workflow: ${res.statusText}`);
    return res.json();
  },

  async importWorkflow(bundle: any): Promise<any> {
    const res = await authFetch(`${BASE_URL}/workflows/import`, {
      method: 'POST',
      body: JSON.stringify(bundle),
    });
    if (!res.ok) throw new Error(`Failed to import workflow: ${res.statusText}`);
    return res.json();
  },

  // 4. DAG Execution Engine
  async executeWorkflow(workflowId: string, inputPayload: Record<string, any>): Promise<{ executionId: string; workflowId: string; status: string }> {
    const res = await authFetch(`${BASE_URL}/workflows/${workflowId}/execute`, {
      method: 'POST',
      body: JSON.stringify({ input: inputPayload }),
    });
    if (!res.ok) throw new Error(`Execution failed: ${res.statusText}`);
    return res.json();
  },

  async getExecution(executionId: string): Promise<any> {
    const res = await authFetch(`${BASE_URL}/executions/${executionId}`);
    if (!res.ok) throw new Error(`Failed to fetch execution: ${res.statusText}`);
    return res.json();
  },

  async listExecutions(workflowId?: string): Promise<any[]> {
    const query = workflowId ? `?workflowId=${encodeURIComponent(workflowId)}` : '';
    const res = await authFetch(`${BASE_URL}/executions${query}`);
    if (!res.ok) throw new Error(`Failed to list executions: ${res.statusText}`);
    return res.json();
  },

  async retryExecution(executionId: string): Promise<any> {
    const res = await authFetch(`${BASE_URL}/executions/${executionId}/retry`, {
      method: 'POST',
    });
    if (!res.ok) throw new Error(`Failed to retry execution: ${res.statusText}`);
    return res.json();
  },

  // 5. AI Generation Layer
  async generateWorkflowAI(prompt: string): Promise<{ workflow: any }> {
    const res = await authFetch(`${BASE_URL}/ai/generate-workflow`, {
      method: 'POST',
      body: JSON.stringify({ prompt }),
    });
    if (!res.ok) throw new Error(`AI generation failed: ${res.statusText}`);
    return res.json();
  },

  // 6. Marketing Templates Catalog
  async getTemplates(): Promise<any[]> {
    const res = await fetch(`${BASE_URL}/templates`);
    if (!res.ok) throw new Error(`Failed to fetch templates: ${res.statusText}`);
    return res.json();
  },

  // 7. System Metrics & ROI
  async getMetrics(): Promise<any> {
    const res = await authFetch(`${BASE_URL}/metrics`);
    if (!res.ok) throw new Error(`Failed to fetch metrics: ${res.statusText}`);
    return res.json();
  },
};
