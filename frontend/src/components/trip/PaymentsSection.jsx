import React, { useState, useEffect, useCallback } from 'react';
import { formatCurrency, resolveTripCurrency } from '../../utils/formatters.js';
import axiosClient from '../../api/axiosClient.js';

/**
 * PaymentStatusCard — shows real payment collection status.
 *
 * Data binding:
 *   trip.paymentSummary     — payment summary from payment-service (may be null)
 *   trip.paymentAvailable   — boolean: payment-service is configured
 *   trip.splitResult        — used to derive total due if payment not initialized
 *   trip.currency           — currency fallback
 *
 * States:
 *   A) Payment service not configured → "Payments Unavailable"
 *   B) Payment initialized, nothing collected → "Ready to Collect"
 *   C) Payment in progress → show collected/pending/participants
 *   D) Fully paid → "Fully Collected"
 */
export default function PaymentsSection({ trip }) {
  const [summary, setSummary]   = useState(trip?.paymentSummary || null);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');
  const [message, setMessage]   = useState('');

  const tripId   = trip?.tripId;
  const currency = summary?.currencyCode || resolveTripCurrency(trip);
  const fmt      = (v) => formatCurrency(v ?? 0, currency);

  const fetchSummary = useCallback(async () => {
    if (!tripId) return;
    try {
      const res = await axiosClient.get(`/api/payments/trip/${tripId}`);
      setSummary(res.data?.data || null);
    } catch {
      // Payment service may not be available — silent fail
    }
  }, [tripId]);

  useEffect(() => {
    if (!summary && tripId) fetchSummary();
  }, [tripId]);

  // ── Determine total due from split or budget ──────────────────────────────
  const splitTotal = trip?.splitResult?.totalAmount
    ?? (trip?.splitResult?.participants?.reduce((s, p) => s + (Number(p.amount) || 0), 0) || 0)
    ?? 0;
  const totalDue = summary?.totalAmount
    ?? (splitTotal > 0 ? splitTotal : (trip?.budgetBreakdown?.totalEstimated ?? 0));

  // ── Safe math ─────────────────────────────────────────────────────────────
  const collected = Number(summary?.amountPaid ?? 0);
  const pending   = Math.max(0, totalDue - collected);
  const pct       = totalDue > 0 && Number.isFinite(collected)
    ? Math.min(100, Math.round((collected / totalDue) * 100))
    : 0;
  const isFullyPaid = pct >= 100 || summary?.status === 'FULLY_PAID';

  // ── Case A: Payment service not configured ────────────────────────────────
  if (!trip?.paymentAvailable && !summary) {
    return (
      <div className="card-premium p-7 space-y-5 bg-[#121214] rounded-3xl border border-white/5">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-zinc-100">Payment Status</h3>
          <span className="px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border bg-zinc-800/80 border-zinc-700 text-zinc-400">
            Unavailable
          </span>
        </div>
        <p className="text-sm text-zinc-500 leading-relaxed">
          Trip split is ready. Online payment collection is not enabled in this environment.
        </p>
        {splitTotal > 0 && (
          <div className="p-4 rounded-2xl bg-zinc-900/50 border border-white/5">
            <p className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1">Split Total</p>
            <p className="text-lg font-bold text-zinc-200">{fmt(splitTotal)}</p>
          </div>
        )}
      </div>
    );
  }

  // ── Case B/C/D: Payment service available ────────────────────────────────
  const statusBadge = isFullyPaid
    ? { label: 'Fully Collected', cls: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' }
    : collected > 0
    ? { label: 'In Progress',     cls: 'bg-brand-500/10 border-brand-500/20 text-brand-400' }
    : { label: 'Ready to Collect', cls: 'bg-zinc-800/80 border-zinc-700 text-zinc-400' };

  return (
    <div className="card-premium p-7 space-y-7 bg-[#121214] relative overflow-hidden rounded-3xl">
      <div className="absolute top-0 right-0 w-40 h-40 bg-emerald-500/5 blur-[60px] rounded-full pointer-events-none"></div>

      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-base font-bold text-zinc-100">Payment Status</h3>
        <span className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border ${statusBadge.cls}`}>
          {statusBadge.label}
        </span>
      </div>

      {/* Status messages */}
      {message && (
        <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-xs">
          {message}
        </div>
      )}
      {error && (
        <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-300 text-xs">
          {error}
        </div>
      )}

      {/* Collected vs Pending */}
      <div className="grid grid-cols-2 gap-6">
        <div>
          <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Collected</p>
          <p className="text-2xl font-black text-emerald-400 tracking-tight">{fmt(collected)}</p>
        </div>
        <div>
          <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Pending</p>
          <p className={`text-2xl font-black tracking-tight ${isFullyPaid ? 'text-zinc-600' : 'text-zinc-100'}`}>
            {fmt(pending)}
          </p>
        </div>
      </div>

      {/* Progress bar */}
      {totalDue > 0 && (
        <div className="space-y-2">
          <div className="flex justify-between text-xs text-zinc-500">
            <span>Collection progress</span>
            <span className="text-zinc-300 font-semibold">{pct}%</span>
          </div>
          <div className="h-2 w-full bg-zinc-900 rounded-full overflow-hidden">
            <div
              className="h-full bg-emerald-500 rounded-full transition-all duration-700"
              style={{ width: `${pct}%` }}
            />
          </div>
        </div>
      )}

      {/* Participant statuses — only if real data exists */}
      {summary?.participants?.length > 0 && (
        <div className="space-y-2 pt-2 border-t border-white/5">
          <p className="text-[10px] text-zinc-500 uppercase tracking-wider">Participants</p>
          {summary.participants.map((p, idx) => (
            <div key={idx} className="flex items-center justify-between py-2">
              <div className="flex items-center gap-2">
                <div className="w-6 h-6 rounded-full bg-brand-500/20 flex items-center justify-center text-brand-400 text-[10px] font-bold">
                  {(p.participantName || `T${idx+1}`).charAt(0).toUpperCase()}
                </div>
                <span className="text-xs text-zinc-300">{p.participantName || `Traveler ${idx + 1}`}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-zinc-200">{fmt(p.allocatedAmount)}</span>
                <span className={`text-[9px] px-2 py-0.5 rounded-full font-bold uppercase ${
                  p.status === 'PAID' || p.status === 'FULLY_PAID'
                    ? 'bg-emerald-500/10 text-emerald-400'
                    : 'bg-zinc-800 text-zinc-500'
                }`}>
                  {p.status === 'PAID' || p.status === 'FULLY_PAID' ? 'Paid' : 'Pending'}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* CTA — only if payment service is real and not fully paid */}
      {trip?.paymentAvailable && !isFullyPaid && (
        <button
          onClick={fetchSummary}
          disabled={loading}
          className="w-full py-3 rounded-2xl bg-zinc-900 border border-zinc-800 hover:border-brand-500/30 text-sm font-semibold text-zinc-400 hover:text-brand-400 transition-all"
        >
          {loading ? 'Refreshing…' : collected > 0 ? 'Refresh Status' : 'Check Payment Status'}
        </button>
      )}
    </div>
  );
}
