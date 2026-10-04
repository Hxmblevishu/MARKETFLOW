import React, { useState } from 'react';
import { ArrowRight, Play, RotateCcw, Layers, Cpu, BarChart3, Share2, Sparkles } from 'lucide-react';
import { SYSTEM_METRICS } from '../data/mockData';
import { WorkflowNode } from '../types/schema';

interface Props {
  scrollProgress: number;
  onNavigate: (path: string) => void;
  onRunSimulation: (score?: number) => void;
  isSimulating: boolean;
  onSelectNode: (node: WorkflowNode) => void;
  nodes: WorkflowNode[];
}

export const OverviewPage: React.FC<Props> = ({
  scrollProgress,
  onNavigate,
  onRunSimulation,
  isSimulating,
  onSelectNode,
  nodes,
}) => {
  const [testScore, setTestScore] = useState<number>(87);
  const [customPrompt, setCustomPrompt] = useState<string>(
    'When an Instagram lead arrives, qualify with AI. Route score > 70 to Slack & Salesforce, else nurture.'
  );

  const isStage1 = scrollProgress >= 0.05 && scrollProgress < 0.28;
  const isStage2 = scrollProgress >= 0.28 && scrollProgress < 0.55;
  const isStage3 = scrollProgress >= 0.55 && scrollProgress < 0.78;
  const isStage4 = scrollProgress >= 0.78;

  const node1 = nodes.find((n) => n.id === 'node_1');
  const node2 = nodes.find((n) => n.id === 'node_2');
  const node3 = nodes.find((n) => n.id === 'node_3');
  const node4a = nodes.find((n) => n.id === 'node_4a');

  return (
    <div className="relative z-10 w-full select-none pointer-events-auto">
      {/* Hero Section with Open Center DAG */}
      <section className="min-h-[85vh] relative flex flex-col justify-between items-center px-6 max-w-7xl mx-auto pt-6 pb-12">
        {/* Minimal Corner Title (Where Space is Left) */}
        <div className="w-full flex justify-start items-start pt-2">
          <div className="p-3.5 rounded-xl bg-[#09090f]/85 border border-white/10 backdrop-blur-xl shadow-2xl max-w-xs text-left pointer-events-auto">
            <div className="flex items-center gap-1.5 mb-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              <span className="font-mono text-[9px] text-neutral-400 uppercase tracking-widest font-semibold">
                MarketFlow Engine
              </span>
            </div>
            <h1 className="text-xs sm:text-sm font-mono font-bold tracking-wider text-white uppercase leading-snug">
              VISUAL MARKETING WORKFLOWS.<br />
              <span className="text-neutral-400 font-medium">ZERO COMPLEXITY.</span>
            </h1>
          </div>
        </div>

        {/* Center Space is completely open for the Workflow DAG */}
        <div className="flex-1 min-h-[140px] pointer-events-none" />

        {/* Bottom Controls & Stats Grid */}
        <div className="w-full max-w-4xl mx-auto flex flex-col items-center gap-4 z-20 pointer-events-auto">
          {/* Interactive Prompt & Simulation Bar */}
          <div className="w-full max-w-2xl p-2 rounded-2xl bg-[#0d0d14]/90 border border-white/15 backdrop-blur-2xl shadow-2xl flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
            <div className="flex-1 px-3 py-2 text-left font-mono text-xs text-neutral-300 flex items-center gap-2">
              <Sparkles className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
              <input
                type="text"
                value={customPrompt}
                onChange={(e) => setCustomPrompt(e.target.value)}
                className="w-full bg-transparent text-xs text-neutral-200 focus:outline-none placeholder:text-neutral-600 font-mono truncate"
              />
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <button
                onClick={() => onRunSimulation(testScore)}
                disabled={isSimulating}
                className="px-4 py-2 rounded-xl bg-white text-black font-mono text-xs font-semibold hover:bg-neutral-200 transition-all flex items-center justify-center gap-1.5"
              >
                {isSimulating ? (
                  <>
                    <RotateCcw className="w-3.5 h-3.5 animate-spin" />
                    <span>Tracing Flow...</span>
                  </>
                ) : (
                  <>
                    <Play className="w-3.5 h-3.5 fill-current" />
                    <span>Simulate (Score: {testScore})</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Live System Stats Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 w-full max-w-2xl text-left font-mono">
            <div className="p-3 rounded-xl bg-[#09090f]/80 border border-white/10 backdrop-blur-xl">
              <span className="text-[10px] text-neutral-500 uppercase block mb-0.5">Total Ingested</span>
              <div className="text-base font-bold text-white tracking-tight">14,820</div>
              <span className="text-[9px] text-emerald-400">+14.2% this mo</span>
            </div>

            <div className="p-3 rounded-xl bg-[#09090f]/80 border border-white/10 backdrop-blur-xl">
              <span className="text-[10px] text-neutral-500 uppercase block mb-0.5">ICP Match Rate</span>
              <div className="text-base font-bold text-white tracking-tight">{SYSTEM_METRICS.qualificationRate}</div>
              <span className="text-[9px] text-neutral-400">10,140 qualified</span>
            </div>

            <div className="p-3 rounded-xl bg-[#09090f]/80 border border-white/10 backdrop-blur-xl">
              <span className="text-[10px] text-neutral-500 uppercase block mb-0.5">Median Latency</span>
              <div className="text-base font-bold text-white tracking-tight">{SYSTEM_METRICS.avgExecutionMs}ms</div>
              <span className="text-[9px] text-neutral-400">sub-50ms SLA</span>
            </div>

            <div className="p-3 rounded-xl bg-[#09090f]/80 border border-white/10 backdrop-blur-xl">
              <span className="text-[10px] text-neutral-500 uppercase block mb-0.5">Pipeline Created</span>
              <div className="text-base font-bold text-white tracking-tight">{SYSTEM_METRICS.pipelineGenerated}</div>
              <span className="text-[9px] text-neutral-400">attributed</span>
            </div>
          </div>

          {/* Scroll Cue */}
          <div className="flex flex-col items-center gap-1 text-[11px] font-mono text-neutral-500 pt-1">
            <span>Scroll down to step through the execution DAG</span>
            <span className="animate-bounce">↓</span>
          </div>
        </div>
      </section>

      {/* Scroll Story Sections (Synchronized with the background DAG) */}
      <section className="max-w-5xl mx-auto px-6 py-20 pointer-events-none">
        <div className="space-y-52">
          {/* Stage 01: Ingestion */}
          <div className="flex flex-col items-start pointer-events-auto">
            <div
              className={`w-full max-w-md p-6 rounded-2xl border transition-all duration-300 ${
                isStage1
                  ? 'bg-[#09090f]/95 border-white text-white shadow-[0_0_35px_rgba(255,255,255,0.15)] ring-1 ring-white/20'
                  : 'bg-[#09090f]/70 border-white/10 text-neutral-400 opacity-60'
              }`}
              style={{ backdropFilter: 'blur(16px)' }}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-mono text-[11px] text-neutral-500 uppercase tracking-wider">
                  01 / INGESTION
                </span>
                <span className="font-mono text-[10px] px-2 py-0.5 rounded bg-white/10 border border-white/10 text-neutral-300">
                  38ms · 14,820 leads/mo
                </span>
              </div>
              <h3 className="text-lg font-bold text-white mb-2">
                Inbound Webhook & Ad Form Capture
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-4">
                Real-time ingestion for Meta Graph API, Google Search Lead Forms, and custom REST webhooks. Verifies HMAC-SHA256 signatures and parses payloads in under 40ms.
              </p>
              {node1 && (
                <button
                  onClick={() => onSelectNode(node1)}
                  className="text-xs font-mono text-neutral-300 hover:text-white flex items-center gap-1.5 transition-colors"
                >
                  <span>Inspect webhook schema</span>
                  <ArrowRight className="w-3 h-3" />
                </button>
              )}
            </div>
          </div>

          {/* Stage 02: AI Intent Qualification */}
          <div className="flex flex-col items-end pointer-events-auto">
            <div
              className={`w-full max-w-md p-6 rounded-2xl border transition-all duration-300 ${
                isStage2
                  ? 'bg-[#09090f]/95 border-white text-white shadow-[0_0_35px_rgba(255,255,255,0.15)] ring-1 ring-white/20'
                  : 'bg-[#09090f]/70 border-white/10 text-neutral-400 opacity-60'
              }`}
              style={{ backdropFilter: 'blur(16px)' }}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-mono text-[11px] text-neutral-500 uppercase tracking-wider">
                  02 / INTELLIGENCE
                </span>
                <span className="font-mono text-[10px] px-2 py-0.5 rounded bg-white/10 border border-white/10 text-neutral-300">
                  142ms · Score 87/100
                </span>
              </div>
              <h3 className="text-lg font-bold text-white mb-2">
                AI Intent & ICP Qualification
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-4">
                Parses company domain authority, matches declared budget against historical AE win rates, and produces a structured intent score without hallucination risk.
              </p>
              {node2 && (
                <button
                  onClick={() => onSelectNode(node2)}
                  className="text-xs font-mono text-neutral-300 hover:text-white flex items-center gap-1.5 transition-colors"
                >
                  <span>Inspect scoring output</span>
                  <ArrowRight className="w-3 h-3" />
                </button>
              )}
            </div>
          </div>

          {/* Stage 03: Decision Gate */}
          <div className="flex flex-col items-start pointer-events-auto">
            <div
              className={`w-full max-w-md p-6 rounded-2xl border transition-all duration-300 ${
                isStage3
                  ? 'bg-[#09090f]/95 border-white text-white shadow-[0_0_35px_rgba(255,255,255,0.15)] ring-1 ring-white/20'
                  : 'bg-[#09090f]/70 border-white/10 text-neutral-400 opacity-60'
              }`}
              style={{ backdropFilter: 'blur(16px)' }}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-mono text-[11px] text-neutral-500 uppercase tracking-wider">
                  03 / DECISION GATE
                </span>
                <span className="font-mono text-[10px] px-2 py-0.5 rounded bg-white/10 border border-white/10 text-neutral-300">
                  12ms · Threshold ≥ 70
                </span>
              </div>
              <h3 className="text-lg font-bold text-white mb-2">
                Deterministic Branching Logic
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-4">
                High-intent prospects (Score ≥ 70) route immediately to the sales team; prospects under 70 bifurcate into personalized educational email nurture campaigns.
              </p>
              {node3 && (
                <button
                  onClick={() => onSelectNode(node3)}
                  className="text-xs font-mono text-neutral-300 hover:text-white flex items-center gap-1.5 transition-colors"
                >
                  <span>Inspect logic rules</span>
                  <ArrowRight className="w-3 h-3" />
                </button>
              )}
            </div>
          </div>

          {/* Stage 04: Multi-Channel Dispatch */}
          <div className="flex flex-col items-end pointer-events-auto">
            <div
              className={`w-full max-w-md p-6 rounded-2xl border transition-all duration-300 ${
                isStage4
                  ? 'bg-[#09090f]/95 border-white text-white shadow-[0_0_35px_rgba(255,255,255,0.15)] ring-1 ring-white/20'
                  : 'bg-[#09090f]/70 border-white/10 text-neutral-400 opacity-60'
              }`}
              style={{ backdropFilter: 'blur(16px)' }}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-mono text-[11px] text-neutral-500 uppercase tracking-wider">
                  04 / DISPATCH
                </span>
                <span className="font-mono text-[10px] px-2 py-0.5 rounded bg-white/10 border border-white/10 text-neutral-300">
                  78ms · 99.98% SLA
                </span>
              </div>
              <h3 className="text-lg font-bold text-white mb-2">
                Slack Alert & Salesforce Opportunity
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-4">
                Dispatches a notification card with 1-click claim to #sales-priority and synchronously creates the Opportunity record in Salesforce without delay.
              </p>
              {node4a && (
                <button
                  onClick={() => onSelectNode(node4a)}
                  className="text-xs font-mono text-neutral-300 hover:text-white flex items-center gap-1.5 transition-colors"
                >
                  <span>Inspect dispatch endpoints</span>
                  <ArrowRight className="w-3 h-3" />
                </button>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Feature Modules Cards (Opens Different Dedicated Pages) */}
      <section className="max-w-5xl mx-auto px-6 py-24 border-t border-white/[0.08]">
        <div className="flex flex-col md:flex-row items-start md:items-end justify-between gap-4 mb-14">
          <div>
            <span className="font-mono text-xs uppercase tracking-wider text-neutral-500 mb-2 block">
              DEDICATED FEATURE PAGES
            </span>
            <h2 className="text-2xl sm:text-4xl font-extrabold text-white tracking-tight">
              Minimalist tools. Focused workspaces.
            </h2>
          </div>
          <p className="text-xs sm:text-sm text-neutral-400 max-w-sm">
            Each capability opens on its own dedicated page so your team can work with absolute focus and clarity.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Card 1: Workflows Studio */}
          <div
            onClick={() => onNavigate('/workflows')}
            className="p-6 rounded-2xl bg-[#09090f] border border-white/10 hover:border-white/30 transition-all cursor-pointer group flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-9 h-9 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-white">
                  <Layers className="w-4 h-4" />
                </div>
                <span className="font-mono text-[10px] text-neutral-400 bg-white/5 px-2 py-0.5 rounded border border-white/10">
                  Visual DAG Builder
                </span>
              </div>
              <h3 className="text-base font-bold text-white mb-1.5 group-hover:text-neutral-200">
                Workflows Studio
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-6">
                Inspect, test, and deploy visual marketing pipelines. Switch templates and simulate executions in real-time.
              </p>
            </div>
            <div className="pt-4 border-t border-white/[0.08] flex items-center justify-between text-xs font-mono text-neutral-300 group-hover:text-white">
              <span>Open Workflows Page</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>

          {/* Card 2: Leads & Executions */}
          <div
            onClick={() => onNavigate('/leads')}
            className="p-6 rounded-2xl bg-[#09090f] border border-white/10 hover:border-white/30 transition-all cursor-pointer group flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-9 h-9 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-white">
                  <Cpu className="w-4 h-4" />
                </div>
                <span className="font-mono text-[10px] text-neutral-400 bg-white/5 px-2 py-0.5 rounded border border-white/10">
                  Live Lead Inbox
                </span>
              </div>
              <h3 className="text-base font-bold text-white mb-1.5 group-hover:text-neutral-200">
                Leads &amp; Executions Stream
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-6">
                Interactive intent scoring sandbox, filterable inbound lead stream, and step-by-step audit logs.
              </p>
            </div>
            <div className="pt-4 border-t border-white/[0.08] flex items-center justify-between text-xs font-mono text-neutral-300 group-hover:text-white">
              <span>Open Leads Page</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>

          {/* Card 3: Analytics */}
          <div
            onClick={() => onNavigate('/analytics')}
            className="p-6 rounded-2xl bg-[#09090f] border border-white/10 hover:border-white/30 transition-all cursor-pointer group flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-9 h-9 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-white">
                  <BarChart3 className="w-4 h-4" />
                </div>
                <span className="font-mono text-[10px] text-neutral-400 bg-white/5 px-2 py-0.5 rounded border border-white/10">
                  Real-Time Telemetry
                </span>
              </div>
              <h3 className="text-base font-bold text-white mb-1.5 group-hover:text-neutral-200">
                Performance Analytics
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-6">
                Minimalist conversion funnel charts, daily volume breakdowns, and latency benchmark graphs.
              </p>
            </div>
            <div className="pt-4 border-t border-white/[0.08] flex items-center justify-between text-xs font-mono text-neutral-300 group-hover:text-white">
              <span>Open Analytics Page</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>

          {/* Card 4: Integrations */}
          <div
            onClick={() => onNavigate('/integrations')}
            className="p-6 rounded-2xl bg-[#09090f] border border-white/10 hover:border-white/30 transition-all cursor-pointer group flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-9 h-9 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-white">
                  <Share2 className="w-4 h-4" />
                </div>
                <span className="font-mono text-[10px] text-neutral-400 bg-white/5 px-2 py-0.5 rounded border border-white/10">
                  Connectors Hub
                </span>
              </div>
              <h3 className="text-base font-bold text-white mb-1.5 group-hover:text-neutral-200">
                Integrations Ecosystem
              </h3>
              <p className="text-xs text-neutral-400 leading-relaxed mb-6">
                Plug in Slack, Salesforce, HubSpot, Stripe, PostgreSQL, and custom webhooks with HMAC authentication.
              </p>
            </div>
            <div className="pt-4 border-t border-white/[0.08] flex items-center justify-between text-xs font-mono text-neutral-300 group-hover:text-white">
              <span>Open Integrations Hub</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>
        </div>
      </section>

      {/* Studio CTA Banner */}
      <section className="max-w-5xl mx-auto px-6 py-20 border-t border-white/[0.08] text-center space-y-4">
        <h2 className="text-2xl sm:text-3xl font-bold text-white">Start automating without friction.</h2>
        <p className="text-xs sm:text-sm text-neutral-400 max-w-lg mx-auto">
          Deploy autonomous marketing pipelines, intelligent lead triage, and deterministic routing in minutes.
        </p>
        <div className="pt-2 flex items-center justify-center gap-3">
          <button
            onClick={() => onNavigate('/workflows')}
            className="px-5 py-2.5 rounded-xl bg-white text-black font-semibold text-xs hover:bg-neutral-200 transition-all flex items-center gap-1.5"
          >
            <span>Launch Studio</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => onNavigate('/leads')}
            className="px-5 py-2.5 rounded-xl bg-white/5 border border-white/15 text-white font-medium text-xs hover:border-white/30 transition-all"
          >
            View Live Stream
          </button>
        </div>
      </section>
    </div>
  );
};
