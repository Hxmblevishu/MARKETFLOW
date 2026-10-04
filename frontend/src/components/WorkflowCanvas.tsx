import React, { useRef, useState, useEffect, useMemo } from 'react';
import { WorkflowNode, WorkflowEdge } from '../types/schema';
import { Play, Check, ChevronRight } from 'lucide-react';

interface Props {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  scrollProgress?: number; // 0.0 to 1.0 (for scroll-driven mode)
  activeNodeId?: string | null;
  onSelectNode?: (node: WorkflowNode) => void;
  simulationActiveNodeId?: string | null;
  isBackground?: boolean;
}

export const WorkflowCanvas: React.FC<Props> = ({
  nodes,
  edges,
  scrollProgress = 0,
  activeNodeId = null,
  onSelectNode,
  simulationActiveNodeId = null,
  isBackground = false,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 1200, height: 750 });
  const [hoveredNodeId, setHoveredNodeId] = useState<string | null>(null);

  useEffect(() => {
    const updateDimensions = () => {
      if (containerRef.current) {
        setDimensions({
          width: containerRef.current.clientWidth || window.innerWidth,
          height: containerRef.current.clientHeight || (isBackground ? window.innerHeight : 650),
        });
      }
    };
    updateDimensions();
    window.addEventListener('resize', updateDimensions);
    return () => window.removeEventListener('resize', updateDimensions);
  }, [isBackground]);

  // Convert percentage coordinates to absolute canvas pixels
  const getNodePos = (node: WorkflowNode) => {
    const isMobile = dimensions.width < 768;
    const xPct = isMobile ? Math.min(88, Math.max(12, node.xPercent)) : node.xPercent;
    const yPct = node.yPercent;
    return {
      x: (xPct / 100) * dimensions.width,
      y: (yPct / 100) * dimensions.height,
    };
  };

  // Compute smooth Cubic Bézier Path
  const computeBezierPath = (x1: number, y1: number, x2: number, y2: number) => {
    const dx = x2 - x1;
    const cp1x = x1 + dx * 0.5;
    const cp1y = y1;
    const cp2x = x2 - dx * 0.5;
    const cp2y = y2;
    return `M ${x1} ${y1} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${x2} ${y2}`;
  };

  // Calculate point on Bezier curve at t [0, 1]
  const getBezierPoint = (x1: number, y1: number, x2: number, y2: number, t: number) => {
    const dx = x2 - x1;
    const cp1x = x1 + dx * 0.5;
    const cp1y = y1;
    const cp2x = x2 - dx * 0.5;
    const cp2y = y2;

    const oneMinusT = 1 - t;
    const x =
      Math.pow(oneMinusT, 3) * x1 +
      3 * Math.pow(oneMinusT, 2) * t * cp1x +
      3 * oneMinusT * Math.pow(t, 2) * cp2x +
      Math.pow(t, 3) * x2;
    const y =
      Math.pow(oneMinusT, 3) * y1 +
      3 * Math.pow(oneMinusT, 2) * t * cp1y +
      3 * oneMinusT * Math.pow(t, 2) * cp2y +
      Math.pow(t, 3) * y2;

    return { x, y };
  };

  const nodeMap = useMemo(() => {
    const map = new Map<string, WorkflowNode>();
    nodes.forEach((n) => map.set(n.id, n));
    return map;
  }, [nodes]);

  return (
    <div
      ref={containerRef}
      className={`w-full h-full select-none ${
        isBackground
          ? 'fixed inset-0 pointer-events-auto bg-[#050508]'
          : 'relative bg-[#07070b] rounded-2xl border border-white/[0.08] overflow-hidden min-h-[620px]'
      }`}
      style={{ zIndex: isBackground ? 0 : 1 }}
    >
      {/* Background Dot Matrix */}
      <div className="absolute inset-0 bg-grid opacity-25 pointer-events-none" />

      {/* SVG Conduits Layer */}
      <svg className="absolute inset-0 w-full h-full pointer-events-none">
        <defs>
          <linearGradient id="flow-gradient" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#ffffff" stopOpacity="0.4" />
            <stop offset="100%" stopColor="#ffffff" stopOpacity="1" />
          </linearGradient>
          <filter id="soft-glow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="3" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>

        {/* Base Inactive Edges */}
        {edges.map((edge) => {
          const sNode = nodeMap.get(edge.source);
          const tNode = nodeMap.get(edge.target);
          if (!sNode || !tNode) return null;

          const p1 = getNodePos(sNode);
          const p2 = getNodePos(tNode);
          const path = computeBezierPath(p1.x, p1.y, p2.x, p2.y);

          return (
            <path
              key={`base-${edge.id}`}
              d={path}
              fill="none"
              stroke="rgba(255, 255, 255, 0.08)"
              strokeWidth="2"
            />
          );
        })}

        {/* Dynamic Active Animated Edges */}
        {edges.map((edge) => {
          const sNode = nodeMap.get(edge.source);
          const tNode = nodeMap.get(edge.target);
          if (!sNode || !tNode) return null;

          const p1 = getNodePos(sNode);
          const p2 = getNodePos(tNode);
          const path = computeBezierPath(p1.x, p1.y, p2.x, p2.y);

          // Calculate progression from scroll or simulation
          const range = edge.scrollRange || [0.1, 0.9];
          const scrollProg = Math.max(0, Math.min(1, (scrollProgress - range[0]) / (range[1] - range[0])));
          
          const isSimActive =
            simulationActiveNodeId === edge.source || simulationActiveNodeId === edge.target;
          
          const effectiveProgress = isSimActive ? 1 : scrollProg;
          const isActive = effectiveProgress > 0;

          const packetPos = getBezierPoint(p1.x, p1.y, p2.x, p2.y, Math.min(1, effectiveProgress));

          return (
            <g key={`active-${edge.id}`}>
              {/* Highlight stroke */}
              {isActive && (
                <path
                  d={path}
                  fill="none"
                  stroke="#ffffff"
                  strokeWidth="2.2"
                  pathLength={1}
                  strokeDasharray={1}
                  strokeDashoffset={1 - effectiveProgress}
                  filter="url(#soft-glow)"
                  className="transition-all duration-100"
                />
              )}

              {/* Data packet pulse */}
              {isActive && effectiveProgress < 1 && (
                <g transform={`translate(${packetPos.x}, ${packetPos.y})`}>
                  <circle r="4" fill="#ffffff" filter="url(#soft-glow)" />
                  <circle
                    r="8"
                    fill="none"
                    stroke="rgba(255, 255, 255, 0.7)"
                    strokeWidth="1.5"
                    className="animate-ping"
                  />
                </g>
              )}

              {/* Label */}
              {edge.label && (
                <text
                  x={(p1.x + p2.x) / 2}
                  y={(p1.y + p2.y) / 2 - 8}
                  fill={isActive ? '#ffffff' : 'rgba(255, 255, 255, 0.4)'}
                  fontSize="10"
                  fontFamily="'JetBrains Mono', monospace"
                  textAnchor="middle"
                  className="transition-colors"
                >
                  {edge.label}
                </text>
              )}
            </g>
          );
        })}
      </svg>

      {/* Nodes Layer */}
      <div className="absolute inset-0 pointer-events-none">
        {nodes.map((node) => {
          const pos = getNodePos(node);
          const isSelected = activeNodeId === node.id;
          const isHovered = hoveredNodeId === node.id;
          const isSimActive = simulationActiveNodeId === node.id;

          const isHighlighted = isSelected || isHovered || isSimActive;

          return (
            <div
              key={node.id}
              onClick={() => onSelectNode && onSelectNode(node)}
              onMouseEnter={() => setHoveredNodeId(node.id)}
              onMouseLeave={() => setHoveredNodeId(null)}
              className={`absolute cursor-pointer pointer-events-auto transition-all duration-300 transform -translate-x-1/2 -translate-y-1/2 ${
                isHighlighted ? 'scale-105 z-30' : 'scale-100 z-10'
              }`}
              style={{
                left: `${pos.x}px`,
                top: `${pos.y}px`,
              }}
            >
              {/* Minimalist Node Card */}
              <div
                className={`w-64 p-4 rounded-xl border backdrop-blur-xl transition-all duration-200 ${
                  isSimActive
                    ? 'bg-[#09090f] border-white text-white shadow-[0_0_30px_rgba(255,255,255,0.3)] ring-1 ring-white'
                    : isHighlighted
                    ? 'bg-[#0c0c14]/90 border-white/60 text-white shadow-[0_0_20px_rgba(255,255,255,0.1)]'
                    : 'bg-[#09090f]/80 border-white/10 text-neutral-300 hover:border-white/30'
                }`}
              >
                {/* Header: Tag + Live Status Dot */}
                <div className="flex items-center justify-between pb-2 mb-2 border-b border-white/[0.08]">
                  <span className="font-mono text-[9px] tracking-wider text-neutral-400 font-semibold">
                    {node.categoryTag}
                  </span>
                  <div className="flex items-center gap-1.5">
                    <span
                      className={`w-1.5 h-1.5 rounded-full ${
                        isHighlighted ? 'bg-white shadow-[0_0_8px_#ffffff]' : 'bg-neutral-600'
                      }`}
                    />
                    <span className="font-mono text-[9px] text-neutral-400 uppercase">
                      {isSimActive ? 'Executing' : isHighlighted ? 'Selected' : 'Idle'}
                    </span>
                  </div>
                </div>

                {/* Node Title */}
                <h4 className="text-xs font-semibold text-white mb-1 tracking-tight">
                  {node.label}
                </h4>

                {/* Description */}
                <p className="text-[11px] text-neutral-400 line-clamp-2 leading-relaxed mb-3">
                  {node.description}
                </p>

                {/* Footer telemetry */}
                <div className="flex items-center justify-between pt-2 border-t border-white/[0.06] text-[10px] font-mono text-neutral-500">
                  <span>Latency: {node.latencyMs}ms</span>
                  <span className="text-neutral-400 hover:text-white flex items-center gap-0.5">
                    inspect <ChevronRight className="w-2.5 h-2.5" />
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
