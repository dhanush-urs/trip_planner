import React from 'react';
import { formatCurrency } from '../../utils/formatters.js';

export default function SplitExpenseCard({ split }) {
  if (!split) return null;

  return (
    <div className="card p-6 animate-slide-up">
      <h3 className="section-title mb-5">
        <span>👥</span> Expense Split
      </h3>

      {/* Summary */}
      <div className="grid grid-cols-3 gap-3 mb-5">
        {[
          { label: 'Total',      value: formatCurrency(split.totalAmount) },
          { label: 'Travelers',  value: split.travelers },
          { label: 'Per Person', value: formatCurrency(split.perPersonAmount) },
        ].map((item) => (
          <div key={item.label} className="bg-navy-900 rounded-xl p-3 text-center">
            <p className="text-lg font-bold text-brand-400">{item.value}</p>
            <p className="text-xs text-slate-500 mt-0.5">{item.label}</p>
          </div>
        ))}
      </div>

      {/* Participant list */}
      {split.participants?.length > 0 && (
        <div className="space-y-2">
          {split.participants.map((p, idx) => (
            <div
              key={idx}
              className="flex items-center justify-between bg-navy-900 rounded-xl px-4 py-3"
            >
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-brand-500/20 border border-brand-500/30
                                flex items-center justify-center text-brand-400 text-xs font-bold">
                  {(p.name || `T${idx + 1}`).charAt(0).toUpperCase()}
                </div>
                <span className="text-sm text-slate-200">{p.name || `Traveler ${idx + 1}`}</span>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-slate-100">{formatCurrency(p.amount)}</p>
                {p.percentage != null && (
                  <p className="text-xs text-slate-500">{p.percentage?.toFixed(1)}%</p>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
