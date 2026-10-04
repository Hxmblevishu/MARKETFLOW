import React, { useState } from 'react';
import { Search, Download, Filter, CheckCircle2, ArrowRight, Zap, X } from 'lucide-react';
import { SAMPLE_LEADS, SYSTEM_METRICS } from '../data/mockData';
import { InboundLead } from '../types/schema';
import { api } from '../services/api';

export const ExecutionsPage: React.FC = () => {
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<'ALL' | 'QUALIFIED' | 'NURTURING'>('ALL');
  const [selectedLead, setSelectedLead] = useState<InboundLead | null>(null);

  // Score Sandbox
  const [companyInput, setCompanyInput] = useState('Acme Corp');
  const [budgetInput, setBudgetInput] = useState('$50,000/yr');
  const [calcScore, setCalcScore] = useState<number | null>(null);
  const [apiStatus, setApiStatus] = useState<string | null>(null);

  const calculateIntent = async (e: React.FormEvent) => {
    e.preventDefault();
    let score = 55;
    if (budgetInput.includes('50') || budgetInput.includes('100') || budgetInput.includes('60')) score += 30;
    if (companyInput.length > 5) score += 7;
    const finalScore = Math.min(98, score);
    setCalcScore(finalScore);

    try {
      const res = await api.executeWorkflow('wf_001', {
        company: companyInput,
        budget: budgetInput,
        score: finalScore,
      });
      setApiStatus(`Dispatched live: Execution #${res.executionId} (${res.status})`);
    } catch {
      setApiStatus('Simulated locally (Connected to backend on :8080 / Render cloud to stream live)');
    }
  };

  const filteredLeads = SAMPLE_LEADS.filter((l) => {
    const matchSearch =
      l.name.toLowerCase().includes(search.toLowerCase()) ||
      l.company.toLowerCase().includes(search.toLowerCase()) ||
      l.email.toLowerCase().includes(search.toLowerCase());
    const matchFilter = filter === 'ALL' ? true : l.status === filter;
    return matchSearch && matchFilter;
  });

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8 select-none">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Leads &amp; Execution Stream</h1>
          <p className="text-sm text-neutral-400 mt-0.5">
            Real-time inbound lead submissions, AI qualification scores, and deterministic pipeline routing.
          </p>
        </div>

        <button
          onClick={() => alert('Exporting verified leads to CSV format...')}
          className="px-3.5 py-1.5 rounded-xl border border-white/10 hover:border-white/25 text-xs font-mono text-neutral-300 hover:text-white transition-all flex items-center gap-1.5 bg-white/[0.03]"
        >
          <Download className="w-3.5 h-3.5" />
          <span>Export CSV</span>
        </button>
      </div>

      {/* KPI Stats Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs font-mono">
        <div className="p-4 rounded-xl bg-[#09090f] border border-white/[0.08]">
          <span className="text-[10px] text-neutral-500 uppercase block mb-1">Total Inbound</span>
          <div className="text-xl font-bold text-white tracking-tight">14,820</div>
          <span className="text-[10px] text-emerald-400">Live Webhook Sink</span>
        </div>
        <div className="p-4 rounded-xl bg-[#09090f] border border-white/[0.08]">
          <span className="text-[10px] text-neutral-500 uppercase block mb-1">Qualified Rate</span>
          <div className="text-xl font-bold text-white tracking-tight">{SYSTEM_METRICS.qualificationRate}</div>
          <span className="text-[10px] text-neutral-400">Score ≥ 70</span>
        </div>
        <div className="p-4 rounded-xl bg-[#09090f] border border-white/[0.08]">
          <span className="text-[10px] text-neutral-500 uppercase block mb-1">Pipeline Generated</span>
          <div className="text-xl font-bold text-white tracking-tight">{SYSTEM_METRICS.pipelineGenerated}</div>
          <span className="text-[10px] text-neutral-400">Attributed</span>
        </div>
        <div className="p-4 rounded-xl bg-[#09090f] border border-white/[0.08]">
          <span className="text-[10px] text-neutral-500 uppercase block mb-1">Execution SLA</span>
          <div className="text-xl font-bold text-white tracking-tight">{SYSTEM_METRICS.avgExecutionMs}ms</div>
          <span className="text-[10px] text-neutral-400">Median Routing</span>
        </div>
      </div>

      {/* Interactive Intent Scoring Sandbox */}
      <div className="p-5 rounded-2xl bg-[#09090f] border border-white/10 space-y-4">
        <div className="flex items-center justify-between border-b border-white/[0.06] pb-3">
          <div className="flex items-center gap-2">
            <Zap className="w-4 h-4 text-white" />
            <h2 className="text-sm font-semibold text-white">Live Intent Scoring Sandbox</h2>
          </div>
          <span className="text-[10px] font-mono text-neutral-400 uppercase bg-white/5 px-2 py-0.5 rounded">
            Sub-150ms Model
          </span>
        </div>

        <form onSubmit={calculateIntent} className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-[11px] font-mono text-neutral-400 mb-1">Company Name</label>
            <input
              type="text"
              value={companyInput}
              onChange={(e) => setCompanyInput(e.target.value)}
              className="w-full bg-[#050508] border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-white/30 font-mono"
            />
          </div>
          <div>
            <label className="block text-[11px] font-mono text-neutral-400 mb-1">Declared Annual Budget</label>
            <input
              type="text"
              value={budgetInput}
              onChange={(e) => setBudgetInput(e.target.value)}
              className="w-full bg-[#050508] border border-white/10 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-white/30 font-mono"
            />
          </div>
          <div className="flex items-end">
            <button
              type="submit"
              className="w-full py-2 rounded-xl bg-white text-black font-semibold text-xs hover:bg-neutral-200 transition-all flex items-center justify-center gap-1.5"
            >
              <span>Test Qualification</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </form>

        {calcScore !== null && (
          <div className="p-3.5 rounded-xl bg-[#050508] border border-white/15 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs font-mono animate-in fade-in">
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-neutral-400">Calculated Intent Score:</span>
              <span className="text-white font-bold text-sm">{calcScore} / 100</span>
              <span className="text-neutral-600">|</span>
              <span className="text-emerald-400">
                {calcScore >= 70 ? 'PASS (Enterprise AE Route & Slack Ping)' : 'NURTURE (HubSpot Sequence)'}
              </span>
            </div>
            <div className="flex items-center gap-3 text-neutral-500 text-[11px]">
              {apiStatus && <span className="text-emerald-400">{apiStatus}</span>}
              <span>Latency: 112ms</span>
            </div>
          </div>
        )}
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 p-3.5 rounded-2xl bg-[#09090f] border border-white/[0.08]">
        <div className="flex items-center gap-2 flex-1 max-w-md px-3.5 py-1.5 rounded-xl bg-[#050508] border border-white/10 focus-within:border-white/25 transition-all">
          <Search className="w-3.5 h-3.5 text-neutral-500" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search leads by name, email, or company..."
            className="w-full bg-transparent text-xs text-neutral-200 placeholder:text-neutral-600 focus:outline-none font-mono"
          />
        </div>

        <div className="flex items-center gap-1.5 font-mono text-xs">
          <span className="text-neutral-500 mr-1 text-[11px] flex items-center gap-1">
            <Filter className="w-3 h-3" /> Filter:
          </span>
          <button
            onClick={() => setFilter('ALL')}
            className={`px-3 py-1 rounded-lg transition-all ${
              filter === 'ALL'
                ? 'bg-white text-black font-semibold'
                : 'text-neutral-400 hover:text-white bg-[#050508] border border-white/10'
            }`}
          >
            All ({SAMPLE_LEADS.length})
          </button>
          <button
            onClick={() => setFilter('QUALIFIED')}
            className={`px-3 py-1 rounded-lg transition-all ${
              filter === 'QUALIFIED'
                ? 'bg-white text-black font-semibold'
                : 'text-neutral-400 hover:text-white bg-[#050508] border border-white/10'
            }`}
          >
            Qualified (≥70)
          </button>
          <button
            onClick={() => setFilter('NURTURING')}
            className={`px-3 py-1 rounded-lg transition-all ${
              filter === 'NURTURING'
                ? 'bg-white text-black font-semibold'
                : 'text-neutral-400 hover:text-white bg-[#050508] border border-white/10'
            }`}
          >
            Nurturing (&lt;70)
          </button>
        </div>
      </div>

      {/* Leads Table */}
      <div className="rounded-2xl border border-white/[0.08] bg-[#09090f] overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#050508] text-neutral-400 font-mono border-b border-white/[0.08] text-[11px]">
              <tr>
                <th className="py-3.5 px-4 font-normal">LEAD PROFILE</th>
                <th className="py-3.5 px-4 font-normal">COMPANY &amp; INDUSTRY</th>
                <th className="py-3.5 px-4 font-normal">ANNUAL BUDGET</th>
                <th className="py-3.5 px-4 font-normal">AI SCORE</th>
                <th className="py-3.5 px-4 font-normal">ROUTING DESTINATION</th>
                <th className="py-3.5 px-4 font-normal">CHANNEL</th>
                <th className="py-3.5 px-4 font-normal text-right">ACTION</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.05]">
              {filteredLeads.map((lead) => (
                <tr
                  key={lead.id}
                  onClick={() => setSelectedLead(lead)}
                  className="hover:bg-white/[0.02] transition-colors cursor-pointer group"
                >
                  <td className="py-3.5 px-4">
                    <div className="font-semibold text-white group-hover:underline">
                      {lead.name}
                    </div>
                    <div className="text-neutral-500 font-mono text-[11px]">
                      {lead.email}
                    </div>
                  </td>
                  <td className="py-3.5 px-4 text-neutral-300">
                    <div className="font-medium text-white">{lead.company}</div>
                    <div className="text-neutral-500 text-[11px]">{lead.industry}</div>
                  </td>
                  <td className="py-3.5 px-4 font-mono text-neutral-300">
                    {lead.budget}
                  </td>
                  <td className="py-3.5 px-4 font-mono">
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-md text-[11px] font-bold ${
                        lead.score >= 70
                          ? 'bg-white text-black shadow-[0_0_12px_rgba(255,255,255,0.2)]'
                          : 'bg-white/5 text-neutral-400 border border-white/10'
                      }`}
                    >
                      {lead.score} / 100
                    </span>
                  </td>
                  <td className="py-3.5 px-4 text-neutral-300 text-[11px]">
                    <div className="flex items-center gap-1.5">
                      <span
                        className={`w-1.5 h-1.5 rounded-full ${
                          lead.score >= 70 ? 'bg-emerald-400' : 'bg-neutral-600'
                        }`}
                      />
                      <span>{lead.destination}</span>
                    </div>
                  </td>
                  <td className="py-3.5 px-4 font-mono text-neutral-400 text-[11px]">
                    {lead.channel}
                  </td>
                  <td className="py-3.5 px-4 text-right">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedLead(lead);
                      }}
                      className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 text-neutral-300 hover:text-white hover:border-white/30 text-[11px] font-mono transition-all"
                    >
                      Audit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Selected Lead Audit Modal */}
      {selectedLead && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in">
          <div className="w-full max-w-lg rounded-2xl bg-[#09090f] border border-white/15 p-6 space-y-5 shadow-2xl">
            <div className="flex items-start justify-between border-b border-white/[0.08] pb-3">
              <div>
                <span className="font-mono text-[10px] text-neutral-500 uppercase tracking-wider">
                  Lead Execution Audit
                </span>
                <h3 className="text-lg font-bold text-white mt-0.5">{selectedLead.name}</h3>
                <p className="text-xs text-neutral-400 font-mono">{selectedLead.email}</p>
              </div>
              <button
                onClick={() => setSelectedLead(null)}
                className="text-neutral-500 hover:text-white p-1"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs font-mono">
              <div className="p-3.5 rounded-xl bg-[#050508] border border-white/[0.06]">
                <span className="text-[10px] text-neutral-500 block mb-0.5">COMPANY & INDUSTRY</span>
                <div className="text-white font-medium">{selectedLead.company}</div>
                <div className="text-neutral-400 text-[11px]">{selectedLead.industry}</div>
              </div>

              <div className="p-3.5 rounded-xl bg-[#050508] border border-white/[0.06]">
                <span className="text-[10px] text-neutral-500 block mb-0.5">INTENT SCORE</span>
                <div className="text-white text-base font-bold">{selectedLead.score} / 100</div>
                <div className="text-neutral-400 text-[11px]">
                  {selectedLead.score >= 70 ? 'Passes High-Intent ICP' : 'Sub-Threshold'}
                </div>
              </div>

              <div className="p-3.5 rounded-xl bg-[#050508] border border-white/[0.06]">
                <span className="text-[10px] text-neutral-500 block mb-0.5">ANNUAL BUDGET</span>
                <div className="text-white font-medium">{selectedLead.budget}</div>
                <div className="text-neutral-400 text-[11px]">Declared via form</div>
              </div>

              <div className="p-3.5 rounded-xl bg-[#050508] border border-white/[0.06]">
                <span className="text-[10px] text-neutral-500 block mb-0.5">ROUTING TARGET</span>
                <div className="text-white font-medium">{selectedLead.destination}</div>
                <div className="text-neutral-400 text-[11px]">Sync verified</div>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-[#050508] border border-white/[0.08] text-xs text-neutral-400 space-y-1.5">
              <div className="text-white font-semibold text-[11px] flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                <span>Deterministic Execution Trace:</span>
              </div>
              <p className="text-[11px] leading-relaxed">
                Lead was captured from <strong className="text-neutral-200">{selectedLead.channel}</strong>. Evaluated by MarketFlow Intent Scoring Model (Score: {selectedLead.score}/100). Condition gate evaluated to <strong className="text-white">{selectedLead.score >= 70 ? 'TRUE' : 'FALSE'}</strong> and dispatched immediately to <strong className="text-neutral-200">{selectedLead.destination}</strong> in 42ms.
              </p>
            </div>

            <div className="flex items-center justify-end pt-2 border-t border-white/[0.08]">
              <button
                onClick={() => setSelectedLead(null)}
                className="px-4 py-2 rounded-xl bg-white text-black font-semibold text-xs hover:bg-neutral-200 transition-all"
              >
                Close Audit
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
