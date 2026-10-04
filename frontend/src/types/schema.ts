export type WorkflowStatus = 'active' | 'draft' | 'paused';
export type ExecutionStatus = 'queued' | 'running' | 'completed' | 'failed';
export type StepStatus = 'pending' | 'running' | 'completed' | 'failed';

export interface WorkflowNode {
  id: string;
  type: 'trigger' | 'ai_qualifier' | 'condition' | 'action' | 'crm_sink';
  label: string;
  categoryTag: string;
  description: string;
  xPercent: number; // 0-100 canvas position
  yPercent: number;
  config: {
    model?: string;
    endpoint?: string;
    threshold?: number;
    channel?: string;
    template?: string;
  };
  latencyMs: number;
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
  branch?: 'YES' | 'NO' | 'ALL';
  scrollRange?: [number, number];
}

export interface WorkflowDefinition {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
}

export interface Workflow {
  id: string;
  name: string;
  category: string;
  description: string;
  status: WorkflowStatus;
  definition: WorkflowDefinition;
  runsCount: number;
  successRate: string;
  avgLatency: string;
  created_at: string;
  updated_at: string;
}

export interface ExecutionStep {
  id: string;
  execution_id: string;
  node_id: string;
  node_label: string;
  status: StepStatus;
  input_data: Record<string, any>;
  output_data: Record<string, any>;
  latency_ms: number;
  error_message?: string;
  started_at: string;
  completed_at: string;
}

export interface Execution {
  id: string;
  workflow_id: string;
  workflow_name: string;
  lead_name: string;
  lead_company: string;
  lead_email: string;
  intent_score: number;
  status: ExecutionStatus;
  input_data: Record<string, any>;
  output_data: Record<string, any>;
  steps: ExecutionStep[];
  total_latency_ms: number;
  created_at: string;
}

export interface InboundLead {
  id: string;
  name: string;
  email: string;
  company: string;
  industry: string;
  budget: string;
  score: number;
  status: 'QUALIFIED' | 'NURTURING' | 'DISPATCHED';
  destination: string;
  channel: string;
  timestamp: string;
}

export interface IntegrationItem {
  id: string;
  name: string;
  category: 'CRM' | 'COMMUNICATION' | 'ADS' | 'DATABASE' | 'PAYMENT';
  description: string;
  connected: boolean;
  eventsSynced: number;
  lastSync: string;
  endpointUrl: string;
}

export interface SystemMetrics {
  totalLeads: number;
  pipelineGenerated: string;
  qualificationRate: string;
  avgExecutionMs: number;
  uptime: string;
  leadsToday: number;
}
