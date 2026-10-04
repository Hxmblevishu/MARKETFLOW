import React, { useState, useEffect, useCallback } from 'react';
import { Navbar } from './components/Navbar';
import { OverviewPage } from './components/OverviewPage';
import { WorkflowCanvas } from './components/WorkflowCanvas';
import { WorkflowsPage } from './components/WorkflowsPage';
import { ExecutionsPage } from './components/ExecutionsPage';
import { AnalyticsPage } from './components/AnalyticsPage';
import { IntegrationsPage } from './components/IntegrationsPage';
import { Footer } from './components/Footer';
import { DEFAULT_WORKFLOWS } from './data/mockData';
import { WorkflowNode } from './types/schema';

export function App() {
  const [currentPath, setCurrentPath] = useState<string>(() => {
    const hash = window.location.hash.replace(/^#/, '');
    return hash || '/';
  });

  const [scrollProgress, setScrollProgress] = useState(0);
  const [selectedNode, setSelectedNode] = useState<WorkflowNode | null>(null);
  const [isSimulating, setIsSimulating] = useState(false);
  const [simulationNodeId, setSimulationNodeId] = useState<string | null>(null);

  const activeWorkflow = DEFAULT_WORKFLOWS[0];

  // Hash route listener
  useEffect(() => {
    const handleHash = () => {
      const hash = window.location.hash.replace(/^#/, '');
      setCurrentPath(hash || '/');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    window.addEventListener('hashchange', handleHash);
    return () => window.removeEventListener('hashchange', handleHash);
  }, []);

  const navigate = useCallback((path: string) => {
    window.location.hash = path;
    setCurrentPath(path);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  // Scroll listener for landing page background animation
  useEffect(() => {
    if (currentPath !== '/') return;

    let ticking = false;
    const handleScroll = () => {
      if (!ticking) {
        window.requestAnimationFrame(() => {
          const total = document.documentElement.scrollHeight - window.innerHeight;
          if (total > 0) {
            const prog = Math.max(0, Math.min(1, window.scrollY / total));
            setScrollProgress(prog);
          }
          ticking = false;
        });
        ticking = true;
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener('scroll', handleScroll);
  }, [currentPath]);

  // Lead execution simulator
  const runSimulation = useCallback(
    (score: number = 87) => {
      if (isSimulating) return;
      setIsSimulating(true);

      if (currentPath !== '/') {
        navigate('/');
      }

      // Step 1: Inbound Webhook
      setSimulationNodeId('node_1');
      const total = document.documentElement.scrollHeight - window.innerHeight;
      if (currentPath === '/') window.scrollTo({ top: 0.12 * total, behavior: 'smooth' });

      // Step 2: AI Scoring
      setTimeout(() => {
        setSimulationNodeId('node_2');
        if (currentPath === '/') window.scrollTo({ top: 0.40 * total, behavior: 'smooth' });
      }, 900);

      // Step 3: Logic Gate
      setTimeout(() => {
        setSimulationNodeId('node_3');
        if (currentPath === '/') window.scrollTo({ top: 0.65 * total, behavior: 'smooth' });
      }, 1800);

      // Step 4: Dispatch
      setTimeout(() => {
        const pass = score >= 70;
        setSimulationNodeId(pass ? 'node_4a' : 'node_4b');
        if (currentPath === '/') window.scrollTo({ top: 0.92 * total, behavior: 'smooth' });
      }, 2700);

      // Complete
      setTimeout(() => {
        setSimulationNodeId(null);
        setIsSimulating(false);
      }, 3600);
    },
    [isSimulating, currentPath, navigate]
  );

  return (
    <div className="relative min-h-screen bg-[#050508] text-white flex flex-col justify-between font-sans">
      <div>
        {/* Navigation Bar */}
        <Navbar
          currentPath={currentPath}
          onNavigate={navigate}
          onRunSimulation={() => runSimulation(87)}
          isSimulating={isSimulating}
        />

        {/* 1. OVERVIEW LANDING PAGE (WITH BACKGROUND SCROLL WORKFLOW) */}
        {currentPath === '/' && (
          <main className="relative">
            {/* Background Scroll Workflow DAG */}
            <WorkflowCanvas
              nodes={activeWorkflow.definition.nodes}
              edges={activeWorkflow.definition.edges}
              scrollProgress={scrollProgress}
              activeNodeId={selectedNode?.id || null}
              onSelectNode={(node) => setSelectedNode(node)}
              simulationActiveNodeId={simulationNodeId}
              isBackground={true}
            />

            {/* Foreground Content */}
            <OverviewPage
              scrollProgress={scrollProgress}
              onNavigate={navigate}
              onRunSimulation={runSimulation}
              isSimulating={isSimulating}
              onSelectNode={(node) => setSelectedNode(node)}
              nodes={activeWorkflow.definition.nodes}
            />
          </main>
        )}

        {/* 2. DEDICATED WORKFLOWS STUDIO PAGE */}
        {currentPath === '/workflows' && (
          <main className="min-h-[85vh]">
            <WorkflowsPage onNavigateHome={() => navigate('/')} />
          </main>
        )}

        {/* 3. DEDICATED LEADS & EXECUTIONS PAGE */}
        {currentPath === '/leads' && (
          <main className="min-h-[85vh]">
            <ExecutionsPage />
          </main>
        )}

        {/* 4. DEDICATED ANALYTICS PAGE */}
        {currentPath === '/analytics' && (
          <main className="min-h-[85vh]">
            <AnalyticsPage />
          </main>
        )}

        {/* 5. DEDICATED INTEGRATIONS PAGE */}
        {currentPath === '/integrations' && (
          <main className="min-h-[85vh]">
            <IntegrationsPage />
          </main>
        )}
      </div>

      {/* Footer */}
      <Footer onNavigate={navigate} />
    </div>
  );
}

export default App;
