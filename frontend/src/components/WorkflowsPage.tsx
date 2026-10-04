import React, { useState, useEffect } from 'react';
import { Play, RotateCcw, Sparkles, Plus, Code, CheckCircle, ChevronDown, Activity, ArrowLeft } from 'lucide-react';
import { WorkflowCanvas } from './WorkflowCanvas';
import { DEFAULT_WORKFLOWS } from '../data/mockData';
import { Workflow, WorkflowNode } from '../types/schema';
import { api } from '../services/api';

interface Props {
  onNavigateHome: () => void;
}

export const WorkflowsPage: React.FC<Props> = ({ onNavigateHome }) => {
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string>(DEFAULT_WORKFLOWS[0].id);
  const [selectedNode, setSelectedNode] = useState<WorkflowNode | null>(DEFAULT_WORKFLOWS[0].definition.nodes[0]);
  const [isSimulating, setIsSimulating] = useState(false);
  const [simulationNodeId, setSimulationNodeId] = useState<string | null>(null);
  const [apiNotice, setApiNotice] = useState<string | null>(null);

  const activeWorkflow: Workflow =
    DEFAULT_WORKFLOWS.find((w) => w.id === selectedWorkflowId) || DEFAULT_WORKFLOWS[0];

  useEffect(() => {
    let mounted = true;
    api.getWorkflows().catch(() => {
      // Backend not running yet, gracefully keep defaults
    });
    return () => { mounted = false; };
  }, []);

  const runSimulation = async () => {
    if (isSimulating) return;
    setIsSimulating(true);
    setApiNotice(null);

    // Attempt to invoke backend API
    try {
      const res = await api.executeWorkflow(activeWorkflow.id, {
        source: 'frontend_studio',
        timestamp: new Date().toISOString(),
      });
      setApiNotice(`Backend: Execution #${res.executionId} (${res.status})`);
    } catch {
      setApiNotice('Simulated locally (Connected to backend on :8080 / Render cloud for live execution)');
    }

    const nodes = activeWorkflow.definition.nodes;
    nodes.forEach((node, index) => {
      setTimeout(() => {
        setSimulationNodeId(node.id);
        if (index === nodes.length - 1) {
          setTimeout(() => {
            setSimulationNodeId(null);
            setIsSimulating(false);
          }, 900);
        }
      }, index * 800);
    });
  };

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-6 select-none">
      {/* Top Breadcrumb & Controls Header */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 p-5 rounded-2xl bg-[#09090f] border border-white/10 shadow-xl">
        <div>
          <button
            onClick={onNavigateHome}
            className="flex items-center gap-1.5 text-xs font-mono text-neutral-400 hover:text-white mb-2 transition-colors"
          >
            <ArrowLeft className="w-3 h-3" />
            <span>Back to Overview</span>
          </button>

          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold text-white tracking-tight">
              {activeWorkflow.name}
            </h1>
            <span
              className={`px-2 py-0.5 rounded text-[10px] font-mono uppercase tracking-wider ${
                activeWorkflow.status === 'active'
                  ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-400'
                  : 'bg-neutral-800 text-neutral-400'
              }`}
            >
              {activeWorkflow.status}
            </span>
          </div>
          <p className="text-xs text-neutral-400 mt-1 max-w-xl">
            {activeWorkflow.description}
          </p>
        </div>

        {/* Workflow Switcher & Action Controls */}
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="relative">
            <select
              value={selectedWorkflowId}
              onChange={(e) => {
                setSelectedWorkflowId(e.target.value);
                const wf = DEFAULT_WORKFLOWS.find((w) => w.id === e.target.value);
                if (wf) setSelectedNode(wf.definition.nodes[0]);
              }}
              className="bg-[#050508] border border-white/15 text-neutral-200 text-xs font-mono rounded-xl px-3 py-2 focus:outline-none focus:border-white/40 appearance-none pr-8 cursor-pointer"
            >
              {DEFAULT_WORKFLOWS.map((wf) => (
                <option key={wf.id} value={wf.id}>
                  {wf.name}
                </option>
              ))}
            </select>
            <ChevronDown className="w-3.5 h-3.5 text-neutral-500 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          </div>

          <button
            onClick={runSimulation}
            disabled={isSimulating}
            className="px-4 py-2 rounded-xl bg-white text-black text-xs font-semibold hover:bg-neutral-200 transition-all flex items-center gap-1.5 shadow-[0_0_20px_rgba(255,255,255,0.2)]"
          >
            {isSimulating ? (
              <>
                <RotateCcw className="w-3.5 h-3.5 animate-spin" />
                <span>Simulating...</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Simulate Pipeline</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Backend API Execution Notice */}
      {apiNotice && (
        <div className="px-4 py-2.5 rounded-xl bg-[#09090f] border border-white/10 text-xs font-mono flex items-center justify-between animate-in fade-in">
          <div className="flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-neutral-300">{apiNotice}</span>
          </div>
          <button
            onClick={() => setApiNotice(null)}
            className="text-neutral-500 hover:text-white text-xs ml-4"
          >
            ✕
          </button>
        </div>
      )}

      {/* Workflow Operational Stats Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs font-mono">
        <div className="p-3.5 rounded-xl bg-[#09090f] border border-white/[0.08]">
          <span className="text-[10px] text-neutral-500 block mb-1">TOTAL RUNS</span>
          <div className="text-base font-bold text-white">{activeWorkflow.runsCount.toLocaleString()}</div>
        </div>
        <div className="p-3.5 rounded-xl bg-[#09090f] border border-white/[0.08]">
          <span className="text-[10px] text-neutral-500 block mb-1">SUCCESS RATE</span>
          <div className="text-base font-bold text-emerald-400">{activeWorkflow.successRate}</div>
        </div>
        <div className="p-3.5 rounded-xl bg-[#09090f] border border-white/[0.08]">
          <span className="text-[10px] text-neutral-500 block mb-1">MEDIAN SPEED</span>
          <div className="text-base font-bold text-white">{activeWorkflow.avgLatency}</div>
        </div>
        <div className="p-3.5 rounded-xl bg-[#09090f] border border-white/[0.08]">
          <span className="text-[10px] text-neutral-500 block mb-1">NODE COUNT</span>
          <div className="text-base font-bold text-white">{activeWorkflow.definition.nodes.length} Nodes</div>
        </div>
      </div>

      {/* Visual Canvas */}
      <div className="w-full h-[520px] rounded-2xl border border-white/10 overflow-hidden relative shadow-2xl">
        <WorkflowCanvas
          nodes={activeWorkflow.definition.nodes}
          edges={activeWorkflow.definition.edges}
          activeNodeId={selectedNode?.id || null}
          onSelectNode={(node) => setSelectedNode(node)}
          simulationActiveNodeId={simulationNodeId}
        />

        <div className="absolute top-4 left-4 z-20 pointer-events-none">
          <span className="text-[11px] font-mono px-3 py-1.5 rounded-lg bg-black/80 border border-white/10 text-neutral-300 backdrop-blur-md">
            Click any node on canvas to view configuration &amp; schema
          </span>
        </div>
      </div>

      {/* Selected Node Details Drawer */}
      {selectedNode && (
        <div className="p-6 rounded-2xl bg-[#09090f] border border-white/10 space-y-4 animate-in fade-in">
          <div className="flex items-start justify-between border-b border-white/[0.08] pb-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="font-mono text-[10px] text-neutral-400 uppercase tracking-wider bg-white/5 px-2 py-0.5 rounded border border-white/10">
                  {selectedNode.categoryTag}
                </span>
                <span className="font-mono text-[10px] text-neutral-500">ID: {selectedNode.id}</span>
              </div>
              <h3 className="text-base font-bold text-white">{selectedNode.label}</h3>
              <p className="text-xs text-neutral-400 mt-0.5">{selectedNode.description}</p>
            </div>

            <button
              onClick={() => setSelectedNode(null)}
              className="text-xs font-mono text-neutral-500 hover:text-white"
            >
              ✕ Close
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs font-mono">
            <div className="p-3.5 rounded-xl bg-[#050508] border border-white/[0.06] space-y-1">
              <span className="text-[10px] text-neutral-500">MODEL / HANDLER</span>
              <div className="text-neutral-200">{selectedNode.config.model || selectedNode.config.endpoint || 'Built-in Function'}</div>
            </div>

            <div className="p-3.5 rounded-xl bg-[#050508] border border-white/[0.06] space-y-1">
              <span className="text-[10px] text-neutral-500">EXECUTION LATENCY</span>
              <div className="text-white font-bold">{selectedNode.latencyMs} ms</div>
            </div>

            <div className="p-3.5 rounded-xl bg-[#050508] border border-white/[0.06] space-y-1">
              <span className="text-[10px] text-neutral-500">STATUS</span>
              <div className="text-emerald-400 flex items-center gap-1.5">
                <CheckCircle className="w-3.5 h-3.5" />
                <span>Ready &amp; Verified</span>
              </div>
            </div>
          </div>

          <div>
            <div className="text-[11px] font-mono text-neutral-400 mb-1.5 flex items-center gap-1.5">
              <Code className="w-3.5 h-3.5" />
              <span>Node Parameters &amp; Config:</span>
            </div>
            <pre className="p-4 rounded-xl bg-[#050508] border border-white/[0.08] text-[11px] font-mono text-neutral-300 overflow-x-auto">
              {JSON.stringify(selectedNode.config, null, 2)}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
};
