import React, { useState } from 'react';
import { Check, Copy, Plus, RefreshCw, Sliders } from 'lucide-react';
import { SAMPLE_INTEGRATIONS } from '../data/mockData';
import { IntegrationItem } from '../types/schema';

export const IntegrationsPage: React.FC = () => {
  const [integrations, setIntegrations] = useState<IntegrationItem[]>(SAMPLE_INTEGRATIONS);
  const [category, setCategory] = useState<string>('ALL');
  const [selectedItem, setSelectedItem] = useState<IntegrationItem | null>(null);
  const [copied, setCopied] = useState(false);

  const toggleConnect = (id: string) => {
    setIntegrations((prev) =>
      prev.map((item) =>
        item.id === id
          ? { ...item, connected: !item.connected, lastSync: !item.connected ? 'Just now' : 'Disconnected' }
          : item
      )
    );
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const filtered = integrations.filter((item) => {
    if (category === 'ALL') return true;
    return item.category === category;
  });

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8 select-none">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Integrations &amp; Connectors</h1>
          <p className="text-sm text-neutral-400 mt-0.5">
            Connect your inbound lead channels, qualification LLMs, CRMs, and notification endpoints.
          </p>
        </div>

        <button
          onClick={() => alert('New webhook creation endpoint opened.')}
          className="px-4 py-2 rounded-xl bg-white text-black text-xs font-semibold hover:bg-neutral-200 transition-all flex items-center gap-1.5 shadow-[0_0_15px_rgba(255,255,255,0.15)]"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>New Custom Webhook</span>
        </button>
      </div>

      {/* Category Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 text-xs border-b border-white/[0.08]">
        {['ALL', 'CRM', 'ADS', 'COMMUNICATION', 'DATABASE', 'PAYMENT'].map((cat) => (
          <button
            key={cat}
            onClick={() => setCategory(cat)}
            className={`px-3.5 py-1.5 rounded-lg transition-all whitespace-nowrap ${
              category === cat
                ? 'bg-white/10 text-white font-medium border border-white/15'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            {cat === 'ALL' ? 'All Connectors' : cat}
          </button>
        ))}
      </div>

      {/* Integrations Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map((item) => (
          <div
            key={item.id}
            className="p-5 rounded-2xl bg-[#09090f] border border-white/10 hover:border-white/25 transition-all flex flex-col justify-between"
          >
            <div>
              <div className="flex items-start justify-between gap-2 mb-3">
                <div>
                  <h3 className="text-sm font-semibold text-white">{item.name}</h3>
                  <span className="font-mono text-[10px] text-neutral-500 uppercase">
                    {item.category}
                  </span>
                </div>

                <button
                  onClick={() => toggleConnect(item.id)}
                  className={`px-2.5 py-0.5 rounded-md text-[10px] font-mono transition-all ${
                    item.connected
                      ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-400'
                      : 'bg-white/5 border border-white/10 text-neutral-400 hover:text-white'
                  }`}
                >
                  {item.connected ? 'Connected' : 'Connect'}
                </button>
              </div>

              <p className="text-xs text-neutral-400 leading-relaxed mb-4">
                {item.description}
              </p>
            </div>

            <div className="pt-3 border-t border-white/[0.06] space-y-2">
              <div className="text-[11px] font-mono text-neutral-300 truncate bg-[#050508] px-2.5 py-1.5 rounded-lg border border-white/[0.06]">
                {item.endpointUrl}
              </div>

              <div className="flex items-center justify-between text-[11px] font-mono text-neutral-500">
                <span>Synced: {item.eventsSynced.toLocaleString()} events</span>
                <button
                  onClick={() => setSelectedItem(item)}
                  className="text-neutral-300 hover:text-white flex items-center gap-1 transition-colors"
                >
                  <Sliders className="w-3 h-3" />
                  <span>Configure</span>
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Production Inbound Webhook Endpoint */}
      <div className="p-6 rounded-2xl bg-[#09090f] border border-white/10 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 shadow-xl">
        <div>
          <h2 className="text-sm font-semibold text-white mb-1">Global Inbound Production Webhook URL</h2>
          <p className="text-xs text-neutral-400">
            Submit HTTP POST requests with JSON payloads to this endpoint to trigger your live visual DAGs.
          </p>
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto">
          <code className="px-3.5 py-2 rounded-xl bg-[#050508] border border-white/15 text-xs font-mono text-neutral-200 truncate select-all">
            https://api.marketflow.io/v1/wh/prod-8812a-lead
          </code>
          <button
            onClick={() => handleCopy('https://api.marketflow.io/v1/wh/prod-8812a-lead')}
            className="p-2 rounded-xl bg-white/10 border border-white/15 text-neutral-300 hover:text-white transition-all"
            title="Copy URL"
          >
            {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Configuration Modal */}
      {selectedItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in">
          <div className="w-full max-w-lg rounded-2xl bg-[#09090f] border border-white/15 p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-white/[0.08] pb-3">
              <div>
                <h3 className="text-base font-bold text-white">Configure {selectedItem.name}</h3>
                <span className="font-mono text-xs text-neutral-500 uppercase">{selectedItem.category}</span>
              </div>
              <button
                onClick={() => setSelectedItem(null)}
                className="text-neutral-500 hover:text-white text-xs font-mono"
              >
                ✕ Close
              </button>
            </div>

            <div className="space-y-4 text-xs">
              <div>
                <label className="block text-neutral-400 mb-1 font-mono text-[11px]">API Key / Webhook Secret Token</label>
                <div className="flex items-center gap-2">
                  <input
                    type="password"
                    defaultValue="mk_live_99a82bb19047b0198aa"
                    className="w-full bg-[#050508] border border-white/10 rounded-xl px-3 py-2 text-neutral-200 font-mono text-xs focus:outline-none focus:border-white/30"
                  />
                  <button
                    onClick={() => alert('Secret token regenerated.')}
                    className="p-2 rounded-xl border border-white/10 hover:border-white/30 text-neutral-400 hover:text-white"
                    title="Regenerate"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-neutral-400 mb-1 font-mono text-[11px]">Destination Channel / Object Mapping</label>
                <input
                  type="text"
                  defaultValue="#sales-priority"
                  className="w-full bg-[#050508] border border-white/10 rounded-xl px-3 py-2 text-neutral-200 font-mono text-xs focus:outline-none focus:border-white/30"
                />
              </div>

              <div className="p-3.5 rounded-xl bg-[#050508] border border-white/[0.06] text-neutral-400 leading-relaxed text-[11px]">
                Events trigger in real-time when the active DAG finishes node processing. Zero polling overhead.
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-white/[0.08]">
              <button
                onClick={() => setSelectedItem(null)}
                className="px-3 py-1.5 rounded-lg text-xs text-neutral-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  alert(`Settings successfully saved for ${selectedItem.name}`);
                  setSelectedItem(null);
                }}
                className="px-4 py-2 rounded-xl bg-white text-black font-semibold text-xs hover:bg-neutral-200 transition-all"
              >
                Save Changes
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
