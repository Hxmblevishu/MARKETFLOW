import React, { useState, useEffect } from 'react';
import { Menu, X, Play, RotateCcw, ArrowUpRight, User, LogOut, ShieldCheck, ChevronDown } from 'lucide-react';
import { SYSTEM_METRICS } from '../data/mockData';
import { api, authStorage, UserProfile } from '../services/api';

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
  const [user, setUser] = useState<UserProfile | null>(() => authStorage.getUser());
  const [authDropdown, setAuthDropdown] = useState(false);
  const [authLoading, setAuthLoading] = useState(false);

  useEffect(() => {
    let mounted = true;
    const check = () => {
      api.checkConnection().then((connected) => {
        if (mounted) {
          setBackendConnected(connected);
          // If backend is connected and no active session, auto-authenticate demo judge
          if (connected && !authStorage.getAccessToken()) {
            api.loginDemo()
              .then((session) => {
                if (mounted) setUser(session.user);
              })
              .catch(() => {});
          } else if (mounted) {
            setUser(authStorage.getUser());
          }
        }
      });
    };

    check();
    const interval = setInterval(check, 10000);
    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, []);

  const handleLogout = async (singleDevice: boolean) => {
    setAuthLoading(true);
    try {
      await api.logout(singleDevice);
      setUser(null);
      setAuthDropdown(false);
    } finally {
      setAuthLoading(false);
    }
  };

  const handleDemoLogin = async () => {
    setAuthLoading(true);
    try {
      const session = await api.loginDemo();
      setUser(session.user);
      setAuthDropdown(false);
    } catch (err: any) {
      alert(err.message || 'Demo login failed');
    } finally {
      setAuthLoading(false);
    }
  };

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
              {backendConnected === null ? 'Checking...' : backendConnected ? 'Live Cloud / :8080' : 'Offline (Demo Mode)'}
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

        {/* Right CTA Actions & Auth Session */}
        <div className="hidden md:flex items-center gap-3">
          {/* Simulation Trigger */}
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

          {/* User Account / Multi-Device Logout Dropdown */}
          <div className="relative">
            {user ? (
              <div>
                <button
                  onClick={() => setAuthDropdown(!authDropdown)}
                  className="px-2.5 py-1.5 rounded-lg border border-emerald-500/30 bg-emerald-500/10 text-xs font-mono text-emerald-400 hover:bg-emerald-500/20 transition-all flex items-center gap-1.5"
                >
                  <ShieldCheck className="w-3.5 h-3.5" />
                  <span className="max-w-[120px] truncate">{user.email.split('@')[0]}</span>
                  <ChevronDown className="w-3 h-3 text-neutral-400" />
                </button>

                {authDropdown && (
                  <div className="absolute right-0 mt-2 w-64 rounded-xl bg-[#0c0c14] border border-white/15 shadow-2xl p-3 space-y-2 z-50 text-xs font-sans">
                    <div className="pb-2 border-b border-white/10">
                      <p className="text-white font-semibold truncate">{user.name}</p>
                      <p className="text-neutral-400 font-mono text-[11px] truncate">{user.email}</p>
                      <span className="inline-block mt-1 px-1.5 py-0.5 rounded text-[10px] font-mono bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                        {user.role}
                      </span>
                    </div>

                    <div className="space-y-1">
                      <button
                        onClick={() => handleLogout(true)}
                        disabled={authLoading}
                        className="w-full text-left px-2.5 py-1.5 rounded hover:bg-white/10 text-neutral-300 hover:text-white transition-all flex items-center justify-between"
                      >
                        <span>Log Out (This Device)</span>
                        <LogOut className="w-3 h-3 text-neutral-400" />
                      </button>

                      <button
                        onClick={() => handleLogout(false)}
                        disabled={authLoading}
                        className="w-full text-left px-2.5 py-1.5 rounded hover:bg-red-500/20 text-red-400 hover:text-red-300 transition-all flex items-center justify-between"
                      >
                        <span>Log Out (All Devices)</span>
                        <LogOut className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <button
                onClick={handleDemoLogin}
                disabled={authLoading}
                className="px-2.5 py-1.5 rounded-lg border border-white/15 bg-white/5 hover:bg-white/10 text-xs font-mono text-neutral-300 hover:text-white transition-all flex items-center gap-1.5"
              >
                <User className="w-3.5 h-3.5" />
                <span>Demo Login</span>
              </button>
            )}
          </div>

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
          <div className="pt-2 border-t border-white/10 flex flex-col gap-2">
            {user ? (
              <button
                onClick={() => handleLogout(true)}
                className="w-full py-2 rounded-lg bg-red-500/20 border border-red-500/30 text-red-300 text-xs text-center"
              >
                Log Out (This Device)
              </button>
            ) : (
              <button
                onClick={handleDemoLogin}
                className="w-full py-2 rounded-lg bg-emerald-500/20 border border-emerald-500/30 text-emerald-300 text-xs text-center"
              >
                Demo Judge Login
              </button>
            )}
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
