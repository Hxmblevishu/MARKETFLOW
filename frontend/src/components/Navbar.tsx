import React, { useState, useEffect } from 'react';
import { Menu, X, Play, RotateCcw, ArrowUpRight } from 'lucide-react';
import { SYSTEM_METRICS } from '../data/mockData';
import { api } from '../services/api';

interface Props {
  currentPath: string;
  onNavigate: (path: string) => void;
  onRunSimulation?: () => void;
  isSimulating?: boolean;
}

export const Navbar: React.FC<Props> = ({
  currentPath,
  onNavigate,
  onRunSimulation,
  isSimulating,
}) => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [backendConnected, setBackendConnected] = useState<boolean | null>(null);

  useEffect(() => {
    let mounted = true;
    api.checkConnection().then((connected) => {
      if (mounted) setBackendConnected(connected);
    });
    const interval = setInterval(() => {
      api.checkConnection().then((connected) => {
        if (mounted) setBackendConnected(connected);
      });
    }, 10000);
    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, []);

  const navItems = [
    { name: 'Overview', path: '/' },
    { name: 'Workflows', path: '/workflows' },
    { name: 'Leads & Executions', path: '/leads' },
    { name: 'Analytics', path: '/analytics' },
    { name: 'Integrations', path: '/integrations' },
  ];

  const handleNav = (path: string) => {
    onNavigate(path);
    setMobileOpen(false);
  };

  return (
    <header className="sticky top-0 z-50 bg-[#050508]/85 backdrop-blur-xl border-b border-white/[0.08] select-none">
      {/* Top micro ticker */}
      <div className="hidden lg:flex items-center justify-between px-6 py-1 bg-black/60 border-b border-white/[0.05] text-[11px] font-mono text-neutral-400">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-white font-medium">14,820</span>
            <span className="text-neutral-500">Leads Ingested</span>
          </div>
          <span className="text-neutral-700">/</span>
          <div className="flex items-center gap-1.5">
            <span className="text-white font-medium">{SYSTEM_METRICS.pipelineGenerated}</span>
            <span className="text-neutral-500">Pipeline Influenced</span>
          </div>
          <span className="text-neutral-700">/</span>
          <div className="flex items-center gap-1.5">
            <span className="text-white font-medium">{SYSTEM_METRICS.avgExecutionMs}ms</span>
            <span className="text-neutral-500">Avg Execution SLA</span>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <span className={`w-1.5 h-1.5 rounded-full ${backendConnected ? 'bg-emerald-400 animate-pulse' : 'bg-neutral-500'}`} />
            <span className="text-neutral-500">Backend API:</span>
            <span className={backendConnected ? 'text-emerald-400 font-mono' : 'text-neutral-400 font-mono'}>
              {backendConnected === null ? 'Checking...' : backendConnected ? 'Connected (:5000)' : 'Offline (Demo Mode)'}
            </span>
          </div>
          <span className="text-neutral-700">/</span>
          <div className="flex items-center gap-2">
            <span className="text-neutral-500">Engine:</span>
            <span className="text-emerald-400 font-mono">99.98%</span>
          </div>
        </div>
      </div>

      {/* Main Navbar */}
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        {/* Brand Logo */}
        <div className="flex items-center gap-8">
          <button
            onClick={() => handleNav('/')}
            className="flex items-center gap-2.5 text-left group"
          >
            <div className="w-6 h-6 rounded-md bg-white text-black font-extrabold text-xs flex items-center justify-center tracking-tighter shadow-[0_0_15px_rgba(255,255,255,0.3)]">
              M
            </div>
            <span className="font-bold text-sm tracking-tight text-white group-hover:text-neutral-300 transition-colors">
              MarketFlow
            </span>
          </button>

          {/* Desktop Navigation Links */}
          <nav className="hidden md:flex items-center gap-1 text-xs">
            {navItems.map((item) => {
              const isActive =
                item.path === '/'
                  ? currentPath === '/'
                  : currentPath.startsWith(item.path);

              return (
                <button
                  key={item.path}
                  onClick={() => handleNav(item.path)}
                  className={`px-3 py-1.5 rounded-lg transition-all ${
                    isActive
                      ? 'bg-white/10 text-white font-medium border border-white/15'
                      : 'text-neutral-400 hover:text-white hover:bg-white/[0.04]'
                  }`}
                >
                  {item.name}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Right CTA Actions */}
        <div className="hidden md:flex items-center gap-3">
          {onRunSimulation && (
            <button
              onClick={onRunSimulation}
              disabled={isSimulating}
              className="px-3 py-1.5 rounded-lg border border-white/15 hover:border-white/30 bg-white/[0.04] text-xs font-mono text-neutral-300 hover:text-white transition-all flex items-center gap-1.5"
            >
              {isSimulating ? (
                <>
                  <RotateCcw className="w-3.5 h-3.5 animate-spin" />
                  <span>Tracing...</span>
                </>
              ) : (
                <>
                  <Play className="w-3.5 h-3.5 fill-current" />
                  <span>Run Simulation</span>
                </>
              )}
            </button>
          )}

          <button
            onClick={() => handleNav('/workflows')}
            className="px-4 py-2 rounded-lg bg-white text-black text-xs font-semibold hover:bg-neutral-200 transition-all flex items-center gap-1 shadow-[0_0_20px_rgba(255,255,255,0.15)]"
          >
            <span>Open Studio</span>
            <ArrowUpRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Mobile menu button */}
        <button
          onClick={() => setMobileOpen(!mobileOpen)}
          className="md:hidden p-2 text-neutral-400 hover:text-white"
        >
          {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
        </button>
      </div>

      {/* Mobile Menu */}
      {mobileOpen && (
        <div className="md:hidden border-t border-white/[0.08] bg-[#050508]/95 px-6 py-4 space-y-2">
          {navItems.map((item) => (
            <button
              key={item.path}
              onClick={() => handleNav(item.path)}
              className="w-full text-left py-2 text-sm text-neutral-300 hover:text-white"
            >
              {item.name}
            </button>
          ))}
          <div className="pt-2 border-t border-white/10 flex gap-2">
            <button
              onClick={() => handleNav('/workflows')}
              className="w-full py-2.5 rounded-lg bg-white text-black text-xs font-semibold text-center"
            >
              Open Studio
            </button>
          </div>
        </div>
      )}
    </header>
  );
};
