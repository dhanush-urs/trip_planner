import React from 'react';
import { formatCurrency, resolveTripCurrency } from '../../utils/formatters.js';

/**
 * SplitSummaryCard — READ-ONLY view of how the estimated trip cost is split.
 *
 * This component is intentionally read-only on the Trip Details page.
 * Split editing is available on the Plan Trip flow only.
 *
 * Data binding:
 *   split.totalAmount      — total amount being split
 *   split.perPersonAmount  — per-person share
 *   split.participants[]   — participant rows
 *   split.currencyCode     — currency (falls back to resolveTripCurrency)
 *   trip.currency          — fallback currency
 *   trip.travelers         — fallback participant count
 */
export default function SplitExpenseCard({ split, trip }) {
  if (!split && !trip) return null;

  // ── Currency: use shared resolver — never hardcode INR ───────────────────
  const currency = split?.currencyCode || resolveTripCurrency(trip);
  const fmt = (v) => formatCurrency(v ?? 0, currency);

  // ── Total: prefer split.totalAmount, then estimated spend, then budget ───
  const splitTotal = split?.totalAmount
    ?? (split?.participants?.reduce((s, p) => s + (Number(p.amount) || 0), 0) || 0);
  const total = splitTotal > 0
    ? splitTotal
    : (trip?.budgetBreakdown?.totalEstimated ?? trip?.totalBudget ?? 0);

  // ── Participants: from real split data or fallback ───────────────────────
  const participants = split?.participants?.length > 0
    ? split.participants
    : Array.from({ length: trip?.travelers || 2 }, (_, i) => ({
        name: `Traveler ${i + 1}`,
        amount: total / Math.max(trip?.travelers || 2, 1),
      }));

  // ── Safe percentage ───────────────────────────────────────────────────────
  const safePct = (amount) => {
    const t = Number(total || 0);
    const a = Number(amount || 0);
    if (!Number.isFinite(t) || t <= 0 || !Number.isFinite(a)) return 0;
    const pct = (a / t) * 100;
    return Number.isFinite(pct) ? Math.round(pct) : 0;
  };

  // ── Split mode label ──────────────────────────────────────────────────────
  const splitModeLabel = (() => {
    const m = split?.splitMode || 'EQUAL';
    if (m === 'EQUAL') return 'Equal split';
    if (m === 'CUSTOM_PERCENTAGE') return 'Custom percentage split';
    if (m === 'CUSTOM_AMOUNT') return 'Custom amount split';
    return 'Equal split';
  })();

  // ── Split basis label ─────────────────────────────────────────────────────
  const basisLabel = splitTotal > 0 && trip?.budgetBreakdown?.totalEstimated
    ? Math.abs(splitTotal - trip.budgetBreakdown.totalEstimated) < 1
      ? 'Based on estimated spend'
      : 'Based on total budget'
    : null;

  return (
    <div className="card-premium p-7 space-y-6 bg-[#121214] relative overflow-hidden rounded-3xl">
      {/* Subtle glow */}
      <div className="absolute top-0 right-0 w-40 h-40 bg-accent-500/5 blur-[60px] rounded-full pointer-events-none"></div>

      {/* Header */}
      <div>
        <h3 className="text-base font-bold text-zinc-100">Split Summary</h3>
        <p className="text-[10px] text-zinc-500 mt-0.5">
          {participants.length} {participants.length === 1 ? 'traveler' : 'travelers'} · {splitModeLabel}
          {basisLabel && <span className="text-zinc-600"> · {basisLabel}</span>}
        </p>
      </div>

      {/* Summary stats */}
      <div className="grid grid-cols-3 gap-3">
        {[
          { label: 'Total',      value: fmt(total) },
          { label: 'Travelers',  value: participants.length },
          { label: 'Per Person', value: fmt(total / Math.max(participants.length, 1)) },
        ].map(item => (
          <div key={item.label} className="bg-zinc-950/50 rounded-2xl p-3 text-center border border-white/5">
            <p className="text-sm font-bold text-brand-400">{item.value}</p>
            <p className="text-[10px] text-zinc-500 mt-0.5">{item.label}</p>
          </div>
        ))}
      </div>

      {/* Participant rows — read-only */}
      <div className="space-y-2">
        {participants.map((p, idx) => {
          const pct = safePct(p.amount);
          const name = p.name || p.participantName || `Traveler ${idx + 1}`;
          return (
            <div key={idx} className="flex items-center justify-between bg-zinc-950/40 rounded-2xl px-4 py-3 border border-white/5">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-brand-500/20 border border-brand-500/30 flex items-center justify-center text-brand-400 text-xs font-bold shrink-0">
                  {name.charAt(0).toUpperCase()}
                </div>
                <span className="text-sm text-zinc-200">{name}</span>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-zinc-100">{fmt(p.amount)}</p>
                <p className="text-[10px] text-zinc-600">{pct}%</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Total footer */}
      <div className="flex items-center justify-between pt-3 border-t border-white/5">
        <span className="text-xs font-bold text-zinc-500 uppercase tracking-wider">Total Split</span>
        <span className="text-base font-black text-zinc-100">{fmt(total)}</span>
      </div>
    </div>
  );
}
