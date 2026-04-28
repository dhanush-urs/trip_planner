import React from 'react';
import { formatCurrency, resolveTripCurrency } from '../../utils/formatters.js';

/**
 * BudgetBreakdownCard — renders the trip budget breakdown.
 *
 * Data binding (from normalizeBudget in formatters.js):
 *   budget.totalEstimated   — estimated total spend
 *   budget.totalBudget      — user's budget cap
 *   budget.remainingBudget  — totalBudget - totalEstimated
 *   budget.overBudget       — boolean
 *   budget.hotelCost        — hotel line item
 *   budget.foodCost         — food line item
 *   budget.transportCost    — transport line item
 *   budget.attractionCost   — activities line item
 *   budget.miscCost         — misc / buffer line item
 *   budget.currencyCode     — currency for all amounts
 */
export default function BudgetBreakdownCard({ budget }) {
  if (!budget) return null;

  // ── Currency: use budget's own currencyCode, never default to INR ────────
  const currency = budget.currencyCode || 'USD';
  const estimated   = budget.totalEstimated  ?? 0;
  const userBudget  = budget.totalBudget     ?? 0;
  const remaining   = budget.remainingBudget ?? (userBudget - estimated);
  const isOver      = budget.overBudget ?? (estimated > userBudget);
  const pct         = userBudget > 0 ? Math.min(100, (estimated / userBudget) * 100) : 0;

  const fmt = (v) => formatCurrency(v ?? 0, currency);

  // Line items — all from real backend fields
  const lineItems = [
    { label: 'Hotel',       value: budget.hotelCost,      icon: '🏨' },
    { label: 'Food',        value: budget.foodCost,       icon: '🍽️' },
    { label: 'Transport',   value: budget.transportCost,  icon: '🚗' },
    { label: 'Activities',  value: budget.attractionCost, icon: '🎡' },
    { label: 'Buffer',      value: budget.miscCost,       icon: '📦' },
  ].filter(item => (item.value ?? 0) > 0);   // only show non-zero items

  return (
    <div className="card-premium p-7 space-y-7 bg-[#121214] relative overflow-hidden rounded-3xl">
      {/* Subtle glow */}
      <div className="absolute top-0 right-0 w-40 h-40 bg-emerald-500/5 blur-[60px] rounded-full pointer-events-none"></div>

      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-base font-bold text-zinc-100">Budget Breakdown</h3>
        <span className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${
          isOver
            ? 'bg-red-500/10 text-red-400 border-red-500/20'
            : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
        }`}>
          {isOver ? 'Over budget' : 'Within budget'}
        </span>
      </div>

      {/* Total vs Budget */}
      <div className="grid grid-cols-2 gap-6">
        <div>
          <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Your Budget</p>
          <p className="text-2xl font-black text-zinc-100 tracking-tight">{fmt(userBudget)}</p>
        </div>
        <div>
          <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Estimated Spend</p>
          <p className={`text-2xl font-black tracking-tight ${isOver ? 'text-red-400' : 'text-emerald-400'}`}>
            {fmt(estimated)}
          </p>
        </div>
      </div>

      {/* Progress bar */}
      {userBudget > 0 && (
        <div className="space-y-2">
          <div className="flex justify-between text-xs text-zinc-500">
            <span>Budget used</span>
            <span className={isOver ? 'text-red-400 font-bold' : 'text-zinc-300 font-semibold'}>
              {Math.round(pct)}%
            </span>
          </div>
          <div className="h-2 w-full bg-zinc-900 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-700 ${
                isOver ? 'bg-red-500' : 'bg-brand-500'
              }`}
              style={{ width: `${pct}%` }}
            />
          </div>
        </div>
      )}

      {/* Line items */}
      {lineItems.length > 0 && (
        <div className="space-y-3 pt-2 border-t border-white/5">
          {lineItems.map((item, i) => (
            <div key={i} className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="text-base">{item.icon}</span>
                <span className="text-xs font-medium text-zinc-400">{item.label}</span>
              </div>
              <span className="text-sm font-semibold text-zinc-200">{fmt(item.value)}</span>
            </div>
          ))}
        </div>
      )}

      {/* Remaining / over-budget footer */}
      <div className={`flex items-center justify-between pt-3 border-t ${
        isOver ? 'border-red-500/20' : 'border-white/5'
      }`}>
        <span className="text-xs font-bold text-zinc-500 uppercase tracking-wider">
          {isOver ? 'Over by' : 'Remaining'}
        </span>
        <span className={`text-base font-black ${isOver ? 'text-red-400' : 'text-emerald-400'}`}>
          {fmt(Math.abs(remaining))}
        </span>
      </div>

      {/* FX fallback notice */}
      {budget.fxFallbackUsed && currency !== 'INR' && (
        <div className="flex items-start gap-3 p-3 rounded-xl bg-amber-500/5 border border-amber-500/15 text-xs text-amber-400/80">
          <span className="shrink-0">ℹ</span>
          <span>Exchange rate unavailable — amounts shown in INR (target: {currency})</span>
        </div>
      )}

      {/* Over-budget notice */}
      {isOver && (
        <div className="flex items-start gap-3 p-4 rounded-2xl bg-red-500/5 border border-red-500/15">
          <span className="text-red-400 shrink-0">⚠️</span>
          <div>
            <p className="text-xs font-bold text-red-400">Estimated spend exceeds your budget</p>
            <p className="text-xs text-red-300/60 mt-0.5 leading-relaxed">
              Consider adjusting your hotel preference or trip duration to stay within budget.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
