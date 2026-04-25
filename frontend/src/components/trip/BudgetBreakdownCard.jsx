import React from 'react';
import { formatCurrency } from '../../utils/formatters.js';

const BUDGET_ITEMS = [
  { key: 'hotelCost',      label: 'Hotel',       icon: '🏨', color: 'bg-brand-500' },
  { key: 'foodCost',       label: 'Food',         icon: '🍜', color: 'bg-green-500' },
  { key: 'transportCost',  label: 'Transport',    icon: '🚗', color: 'bg-yellow-500' },
  { key: 'attractionCost', label: 'Attractions',  icon: '🎡', color: 'bg-purple-500' },
  { key: 'miscCost',       label: 'Misc (5%)',    icon: '📦', color: 'bg-slate-500' },
];

export default function BudgetBreakdownCard({ budget }) {
  if (!budget) return null;

  const total = budget.totalEstimated || 0;

  return (
    <div className="card p-6 animate-slide-up">
      <h3 className="section-title mb-5">
        <span>💰</span> Budget Breakdown
      </h3>

      {/* Over-budget warning */}
      {budget.overBudget && (
        <div className="flex items-center gap-2 p-3 rounded-xl bg-danger/10 border border-danger/30
                        text-red-300 text-sm mb-5">
          <span>⚠</span>
          <span>Estimated cost exceeds your budget by{' '}
            <strong>{formatCurrency(Math.abs(budget.remainingBudget))}</strong>
          </span>
        </div>
      )}

      {/* Line items */}
      <div className="space-y-3">
        {BUDGET_ITEMS.map(({ key, label, icon, color }) => {
          const amount = budget[key] || 0;
          const pct = total > 0 ? (amount / total) * 100 : 0;
          return (
            <div key={key}>
              <div className="flex items-center justify-between mb-1">
                <span className="text-sm text-slate-300">{icon} {label}</span>
                <span className="text-sm font-semibold text-slate-200">{formatCurrency(amount)}</span>
              </div>
              <div className="h-1.5 bg-navy-900 rounded-full overflow-hidden">
                <div
                  className={`h-full ${color} rounded-full transition-all duration-500`}
                  style={{ width: `${Math.min(100, pct)}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>

      {/* Totals */}
      <div className="mt-5 pt-5 border-t border-navy-700 space-y-2">
        <div className="flex justify-between text-sm">
          <span className="text-slate-400">Estimated Total</span>
          <span className="font-bold text-slate-100">{formatCurrency(budget.totalEstimated)}</span>
        </div>
        <div className="flex justify-between text-sm">
          <span className="text-slate-400">Your Budget</span>
          <span className="font-semibold text-slate-200">{formatCurrency(budget.totalBudget)}</span>
        </div>
        <div className={`flex justify-between text-sm font-bold pt-1 border-t border-navy-700 ${
          budget.overBudget ? 'text-danger' : 'text-success'
        }`}>
          <span>{budget.overBudget ? 'Over Budget' : 'Remaining'}</span>
          <span>{formatCurrency(Math.abs(budget.remainingBudget))}</span>
        </div>
      </div>
    </div>
  );
}
