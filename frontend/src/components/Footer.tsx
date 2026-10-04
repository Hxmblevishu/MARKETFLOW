import React from 'react';

interface Props {
  onNavigate: (path: string) => void;
}

export const Footer: React.FC<Props> = ({ onNavigate }) => {
  return (
    <footer className="relative z-20 border-t border-white/[0.08] py-14 px-6 select-none bg-[#050508]">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-start md:items-center justify-between gap-8 text-xs text-neutral-400">
        <div className="space-y-2">
          <div className="flex items-center gap-2.5">
            <div className="w-5 h-5 rounded bg-white text-black font-extrabold text-[10px] flex items-center justify-center tracking-tighter">
              M
            </div>
            <span className="font-bold text-white tracking-tight">MarketFlow</span>
            <span className="text-neutral-600">/</span>
            <span className="text-neutral-500 font-mono text-[11px]">Minimalist Marketing Engine</span>
          </div>
          <p className="text-[11px] text-neutral-500 max-w-sm">
            AI-driven intent qualification, deterministic logic gates, and sub-50ms multi-channel CRM dispatch.
          </p>
        </div>

        {/* Navigation Links */}
        <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-xs">
          <button
            onClick={() => onNavigate('/')}
            className="text-neutral-400 hover:text-white transition-colors"
          >
            Overview
          </button>
          <button
            onClick={() => onNavigate('/workflows')}
            className="text-neutral-400 hover:text-white transition-colors"
          >
            Workflows
          </button>
          <button
            onClick={() => onNavigate('/leads')}
            className="text-neutral-400 hover:text-white transition-colors"
          >
            Leads &amp; Executions
          </button>
          <button
            onClick={() => onNavigate('/analytics')}
            className="text-neutral-400 hover:text-white transition-colors"
          >
            Analytics
          </button>
          <button
            onClick={() => onNavigate('/integrations')}
            className="text-neutral-400 hover:text-white transition-colors"
          >
            Integrations
          </button>
        </div>

        <div className="flex items-center gap-3 font-mono text-[11px] text-neutral-500">
          <div className="flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
            <span>Operational</span>
          </div>
          <span>© {new Date().getFullYear()} MarketFlow</span>
        </div>
      </div>
    </footer>
  );
};
