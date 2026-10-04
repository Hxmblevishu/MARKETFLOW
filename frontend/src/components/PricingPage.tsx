import React, { useState } from 'react';
import { Check, ArrowRight } from 'lucide-react';

interface Props {
  onSelectPlan?: (planName: string) => void;
}

export const PricingPage: React.FC<Props> = ({ onSelectPlan }) => {
  const [annual, setAnnual] = useState(true);

  const tiers = [
    {
      name: 'Starter',
      price: annual ? '$0' : '$0',
      period: 'forever free',
      description: 'Ideal for early-stage startups and indie creators exploring workflow automation.',
      highlight: false,
      features: [
        'Up to 1,000 inbound leads / mo',
        '3 active visual DAG pipelines',
        'Standard sub-300ms scoring model',
        'Slack & Webhook connectors',
        '7-day execution audit retention',
        'Community Discord support',
      ],
      cta: 'Start Free',
    },
    {
      name: 'Pro',
      price: annual ? '$39' : '$49',
      period: 'per month',
      description: 'For scaling marketing & sales teams needing deterministic routing and multi-channel sync.',
      highlight: true,
      features: [
        'Up to 25,000 inbound leads / mo',
        'Unlimited visual DAG pipelines',
        'Sub-150ms high-accuracy scoring model',
        'Salesforce, HubSpot & Stripe connectors',
        '30-day execution audit history',
        'Custom condition logic gates',
        'Priority email & Slack support',
      ],
      cta: 'Start 14-Day Pro Trial',
    },
    {
      name: 'Enterprise',
      price: annual ? '$159' : '$199',
      period: 'per month',
      description: 'Dedicated infrastructure with custom SLA guarantees, SOC-2 compliance, and direct database sinks.',
      highlight: false,
      features: [
        'Unlimited inbound throughput',
        'Dedicated sub-50ms inference nodes',
        'Custom private model weights',
        'PostgreSQL & Snowflake streaming sinks',
        'Unlimited audit retention & replay',
        '99.99% uptime SLA guarantee',
        'Dedicated account manager & phone support',
      ],
      cta: 'Contact Sales',
    },
  ];

  const faqs = [
    {
      q: 'How does the background scroll workflow relate to live execution?',
      a: 'The visual canvas reflects your actual execution DAG. Every node represents a real deterministic function: webhooks, qualification models, logic gates, or CRM dispatchers.',
    },
    {
      q: 'Can I add custom webhook endpoints or external APIs?',
      a: 'Yes. MarketFlow allows you to plug arbitrary REST webhooks into any step of the pipeline with HMAC verification.',
    },
    {
      q: 'What happens if a lead fails a qualification condition?',
      a: 'Deterministic branching bifurcates the execution path immediately. Qualified leads route to your sales team, while non-qualifying leads enter automated nurture sequences.',
    },
    {
      q: 'Can I change plans at any time?',
      a: 'Yes, upgrade or downgrade anytime with immediate prorated billing and zero workflow downtime.',
    },
  ];

  return (
    <div className="max-w-6xl mx-auto px-6 py-12 space-y-16 select-none">
      {/* Header */}
      <div className="text-center space-y-4 max-w-2xl mx-auto">
        <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white">
          Simple, transparent pricing.
        </h1>
        <p className="text-sm sm:text-base text-neutral-400">
          No credit calculators or surprise overage bills. Pick a plan that matches your pipeline velocity.
        </p>

        {/* Toggle */}
        <div className="inline-flex items-center gap-3 p-1 rounded-full bg-[#09090f] border border-white/10 text-xs">
          <button
            onClick={() => setAnnual(false)}
            className={`px-4 py-1.5 rounded-full transition-all ${
              !annual ? 'bg-white text-black font-semibold shadow-sm' : 'text-neutral-400 hover:text-white'
            }`}
          >
            Monthly
          </button>
          <button
            onClick={() => setAnnual(true)}
            className={`px-4 py-1.5 rounded-full transition-all flex items-center gap-1.5 ${
              annual ? 'bg-white text-black font-semibold shadow-sm' : 'text-neutral-400 hover:text-white'
            }`}
          >
            <span>Annual</span>
            <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-neutral-800 text-neutral-300">
              Save 20%
            </span>
          </button>
        </div>
      </div>

      {/* Tiers Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {tiers.map((tier) => (
          <div
            key={tier.name}
            className={`rounded-2xl p-6 flex flex-col justify-between transition-all duration-200 ${
              tier.highlight
                ? 'bg-[#09090f] border-2 border-white shadow-[0_0_40px_rgba(255,255,255,0.12)] relative'
                : 'bg-[#09090f] border border-white/10'
            }`}
          >
            {tier.highlight && (
              <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-white text-black font-mono text-[10px] font-bold uppercase tracking-wider px-3 py-0.5 rounded-full shadow-lg">
                Most Popular
              </span>
            )}

            <div>
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-lg font-bold text-white">{tier.name}</h3>
              </div>
              <p className="text-xs text-neutral-400 mb-6 min-h-[32px]">{tier.description}</p>

              <div className="flex items-baseline gap-1.5 mb-6">
                <span className="text-4xl font-extrabold text-white tracking-tight">{tier.price}</span>
                <span className="text-xs text-neutral-500 font-mono">/ {tier.period}</span>
              </div>

              <div className="space-y-3 pt-6 border-t border-white/[0.08]">
                <div className="text-[11px] font-mono text-neutral-500 uppercase tracking-wider">
                  Included Capabilities:
                </div>
                {tier.features.map((feat, i) => (
                  <div key={i} className="flex items-start gap-2.5 text-xs text-neutral-300">
                    <Check className="w-4 h-4 text-white shrink-0 mt-0.5" />
                    <span>{feat}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-8">
              <button
                onClick={() => {
                  if (onSelectPlan) onSelectPlan(tier.name);
                  alert(`Selected ${tier.name} plan`);
                }}
                className={`w-full py-2.5 rounded-xl text-xs font-semibold transition-all flex items-center justify-center gap-1.5 ${
                  tier.highlight
                    ? 'bg-white text-black hover:bg-neutral-200'
                    : 'bg-white/10 border border-white/15 text-white hover:border-white/30'
                }`}
              >
                <span>{tier.cta}</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* FAQ */}
      <div className="pt-12 border-t border-white/[0.08] max-w-3xl mx-auto space-y-6">
        <h2 className="text-xl font-bold text-white tracking-tight text-center mb-8">
          Frequently Asked Questions
        </h2>

        <div className="space-y-4">
          {faqs.map((faq, i) => (
            <div key={i} className="p-5 rounded-2xl bg-[#09090f] border border-white/10 space-y-2">
              <h3 className="text-sm font-semibold text-white">{faq.q}</h3>
              <p className="text-xs text-neutral-400 leading-relaxed">{faq.a}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
