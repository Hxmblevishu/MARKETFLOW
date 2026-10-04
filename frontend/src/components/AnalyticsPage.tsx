import React, { useState } from 'react';
import { TrendingUp, Clock, CheckCircle2, Activity, ArrowUpRight } from 'lucide-react';
import { SYSTEM_METRICS } from '../data/mockData';

export const AnalyticsPage: React.FC = () => {
  const [range, setRange] = useState<'24h' | '7d' | '30d'>('7d');

  const dailyVolume = [
    { day: 'Mon', leads: 1820, qualified: 1240 },
    { day: 'Tue', leads: 2240, qualified: 1530 },
    { day: 'Wed', leads: 2480, qualified: 1710 },
    { day: 'Thu', leads: 2190, qualified: 1500 },
    { day: 'Fri', leads: 2650, qualified: 1820 },
    { day: 'Sat', leads: 1720, qualified: 1180 },
    { day: 'Sun', leads: 1720, qualified: 1160 },
  ];

  const maxVolume = 2800;

  const funnelStages = [
    { label: 'Inbound Webhook Received', count: '14,820', rate: '100%', latency: '38ms' },
    { label: 'AI Intent Evaluated', count: '14,792', rate: '99.8%', latency: '142ms' },
    { label: 'High Intent Qualified (≥70)', count: '10,140', rate: '68.4%', latency: '12ms' },
    { label: 'Synchronized to CRM / Slack', count: '10,136', rate: '99.9%', latency: '78ms' },
  ];

  const executionLogs = [
    { id: 'exec-9821', time: '12:44:02', flow: 'Instagram Lead Triage', lead: 'sarah.lin@apexcloud.io', latency: '270ms', status: 'SUCCESS' },
    { id: 'exec-9820', time: '12:43:55', flow: 'Instagram Lead Triage', lead: 'marcus@novatech.co', latency: '264ms', status: 'SUCCESS' },
    { id: 'exec-9819', time: '12:42:18', flow: 'Demo Request Router', lead: 'elena@solardesign.studio', latency: '310ms', status: 'SUCCESS' },
    { id: 'exec-9818', time: '12:40:41', flow: 'Instagram Lead Triage', lead: 'j.hayes@vertexsupply.com', latency: '190ms', status: 'NURTURE' },
    { id: 'exec-9817', time: '12:38:09', flow: 'Demo Request Router', lead: 'amira@luminafin.org', latency: '255ms', status: 'SUCCESS' },
  ];

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8 select-none">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Activity className="w-4 h-4 text-white" />
            <h1 className="text-2xl font-bold text-white tracking-tight">Real-Time Performance Analytics</h1>
          </div>
          <p className="text-sm text-neutral-400">
            End-to-end telemetry across webhook ingestion, qualification rates, and CRM dispatch latencies.
          </p>
        </div>

        {/* Range switcher */}
        <div className="flex items-center gap-1 p-1 rounded-xl bg-[#09090f] border border-white/10">
          {(['24h', '7d', '30d'] as const).map((r) => (
            <button
              key={r}
              onClick={() => setRange(r)}
              className={`px-3 py-1 rounded-lg text-xs font-mono transition-all ${
                range === r
                  ? 'bg-white text-black font-semibold shadow-sm'
                  : 'text-neutral-400 hover:text-white'
              }`}
            >
              {r.toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-2xl bg-[#09090f] border border-white/[0.08]">
          <div className="flex items-center justify-between text-xs text-neutral-400 mb-2">
            <span>Throughput Volume</span>
            <span className="font-mono text-[10px] text-neutral-500">PROCESSED</span>
          </div>
          <div className="text-2xl font-extrabold text-white tracking-tight">14,820</div>
          <div className="flex items-center gap-1.5 text-xs text-neutral-400 mt-2">
            <TrendingUp className="w-3.5 h-3.5 text-emerald-400" />
            <span className="text-emerald-400 font-semibold">+14.2%</span>
            <span>vs previous period</span>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-[#09090f] border border-white/[0.08]">
          <div className="flex items-center justify-between text-xs text-neutral-400 mb-2">
            <span>Median Speed</span>
            <span className="font-mono text-[10px] text-neutral-500">SLA</span>
          </div>
          <div className="text-2xl font-extrabold text-white tracking-tight">{SYSTEM_METRICS.avgExecutionMs} ms</div>
          <div className="flex items-center gap-1.5 text-xs text-neutral-400 mt-2">
            <Clock className="w-3.5 h-3.5 text-white" />
            <span className="text-white font-medium">-18ms</span>
            <span>faster than target</span>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-[#09090f] border border-white/[0.08]">
          <div className="flex items-center justify-between text-xs text-neutral-400 mb-2">
            <span>Qualification Rate</span>
            <span className="font-mono text-[10px] text-neutral-500">SCORE ≥ 70</span>
          </div>
          <div className="text-2xl font-extrabold text-white tracking-tight">{SYSTEM_METRICS.qualificationRate}</div>
          <div className="flex items-center gap-1.5 text-xs text-neutral-400 mt-2">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
            <span className="text-white font-medium">10,140</span>
            <span>enterprise leads</span>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-[#09090f] border border-white/[0.08]">
          <div className="flex items-center justify-between text-xs text-neutral-400 mb-2">
            <span>Pipeline Created</span>
            <span className="font-mono text-[10px] text-neutral-500">ATTRIBUTED</span>
          </div>
          <div className="text-2xl font-extrabold text-white tracking-tight">{SYSTEM_METRICS.pipelineGenerated}</div>
          <div className="flex items-center gap-1.5 text-xs text-neutral-400 mt-2">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
            <span className="text-neutral-300">99.98% delivery rate</span>
          </div>
        </div>
      </div>

      {/* Daily Volume Bar Chart & Funnel */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Daily Inbound Chart */}
        <div className="lg:col-span-2 p-6 rounded-2xl bg-[#09090f] border border-white/10 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-1">
              <h2 className="text-sm font-semibold text-white">Daily Inbound Volume</h2>
              <div className="flex items-center gap-4 text-xs font-mono text-neutral-400">
                <div className="flex items-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-sm bg-neutral-800" />
                  <span>Total Inbound</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-sm bg-white" />
                  <span>Qualified (Score ≥ 70)</span>
                </div>
              </div>
            </div>
            <p className="text-xs text-neutral-500 mb-6">
              Distribution of raw incoming submissions vs AI-qualified pipeline.
            </p>
          </div>

          <div className="grid grid-cols-7 gap-3 items-end h-52 pt-4 pb-2 border-b border-white/[0.08]">
            {dailyVolume.map((item) => {
              const totalH = (item.leads / maxVolume) * 100;
              const qualH = (item.qualified / maxVolume) * 100;
              return (
                <div key={item.day} className="flex flex-col items-center h-full justify-end group">
                  <div className="w-full max-w-[40px] flex flex-col items-center relative h-full justify-end">
                    <div className="absolute -top-10 opacity-0 group-hover:opacity-100 transition-opacity bg-black border border-white/20 text-[10px] font-mono px-2 py-1 rounded text-white whitespace-nowrap pointer-events-none z-10 shadow-lg">
                      {item.qualified} / {item.leads}
                    </div>
                    <div
                      className="w-full bg-neutral-800 rounded-t-sm transition-all duration-300 relative flex flex-col justify-end overflow-hidden"
                      style={{ height: `${totalH}%` }}
                    >
                      <div
                        className="w-full bg-white transition-all duration-300"
                        style={{ height: `${(qualH / totalH) * 100}%` }}
                      />
                    </div>
                  </div>
                  <span className="text-[11px] font-mono text-neutral-500 mt-2">
                    {item.day}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Funnel Stage Breakdown */}
        <div className="p-6 rounded-2xl bg-[#09090f] border border-white/10 flex flex-col justify-between">
          <div>
            <h2 className="text-sm font-semibold text-white mb-1">Execution Funnel</h2>
            <p className="text-xs text-neutral-500 mb-6">
              Pass-through rate and latency per execution node.
            </p>

            <div className="space-y-4">
              {funnelStages.map((step, idx) => (
                <div key={idx} className="space-y-1.5">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-neutral-300 truncate pr-2">{step.label}</span>
                    <span className="font-mono text-white shrink-0">{step.count}</span>
                  </div>
                  <div className="w-full h-1.5 rounded-full bg-neutral-900 overflow-hidden">
                    <div
                      className="h-full bg-white transition-all duration-500"
                      style={{ width: step.rate }}
                    />
                  </div>
                  <div className="flex items-center justify-between text-[10px] font-mono text-neutral-500">
                    <span>{step.rate} pass rate</span>
                    <span>{step.latency}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="pt-4 mt-6 border-t border-white/[0.08] flex items-center justify-between text-xs text-neutral-400">
            <span>Overall Success Rate</span>
            <span className="font-mono text-emerald-400 font-bold">99.98%</span>
          </div>
        </div>
      </div>

      {/* Live Pipeline Execution Stream */}
      <div className="p-6 rounded-2xl bg-[#09090f] border border-white/10 shadow-xl">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-sm font-semibold text-white">Live Execution Audit Stream</h2>
            <p className="text-xs text-neutral-500">Recent leads processed deterministically by active DAGs.</p>
          </div>
          <span className="px-2.5 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-[11px] font-mono text-emerald-400 flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
            <span>Streaming</span>
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-white/[0.08] text-neutral-500 font-mono text-[11px]">
                <th className="pb-3 font-normal">Execution ID</th>
                <th className="pb-3 font-normal">Timestamp</th>
                <th className="pb-3 font-normal">Workflow</th>
                <th className="pb-3 font-normal">Inbound Lead</th>
                <th className="pb-3 font-normal">Latency</th>
                <th className="pb-3 font-normal text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.05]">
              {executionLogs.map((log) => (
                <tr key={log.id} className="hover:bg-white/[0.02] transition-colors">
                  <td className="py-3 font-mono text-neutral-400">{log.id}</td>
                  <td className="py-3 font-mono text-neutral-500">{log.time}</td>
                  <td className="py-3 text-neutral-300 font-medium">{log.flow}</td>
                  <td className="py-3 font-mono text-white">{log.lead}</td>
                  <td className="py-3 font-mono text-neutral-400">{log.latency}</td>
                  <td className="py-3 text-right">
                    <span
                      className={`inline-block px-2.5 py-0.5 rounded text-[10px] font-mono ${
                        log.status === 'SUCCESS'
                          ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400'
                          : 'bg-white/5 border border-white/10 text-neutral-400'
                      }`}
                    >
                      {log.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
