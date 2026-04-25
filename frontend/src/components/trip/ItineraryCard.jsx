import React, { useState } from 'react';
import { formatDate, formatCurrency } from '../../utils/formatters.js';

const CATEGORY_ICONS = {
  nature:    '🌿', temple: '🛕', beach: '🏖️', adventure: '🧗',
  food:      '🍜', nightlife: '🌙', shopping: '🛍️',
};

export default function ItineraryCard({ day }) {
  const [expanded, setExpanded] = useState(true);
  if (!day) return null;

  return (
    <div className="card overflow-hidden animate-slide-up">
      {/* Day header */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between p-5 hover:bg-navy-700/30
                   transition-colors text-left"
      >
        <div className="flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-brand-500/20 border border-brand-500/30
                          flex items-center justify-center shrink-0">
            <span className="text-brand-400 font-bold text-sm">D{day.dayNumber}</span>
          </div>
          <div>
            <p className="font-semibold text-slate-100">{day.theme || 'Exploration Day'}</p>
            <p className="text-xs text-slate-500">{formatDate(day.date)}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-xs text-slate-500">{day.places?.length || 0} places</span>
          <span className="text-slate-500 text-sm">{expanded ? '▲' : '▼'}</span>
        </div>
      </button>

      {/* Places */}
      {expanded && day.places?.length > 0 && (
        <div className="border-t border-navy-700 divide-y divide-navy-700/50">
          {day.places.map((place, idx) => (
            <div key={idx} className="flex items-start gap-4 px-5 py-4 hover:bg-navy-700/20
                                      transition-colors">
              {/* Time indicator */}
              <div className="shrink-0 text-center w-16">
                <p className="text-xs font-medium text-brand-400">{place.visitTime || '—'}</p>
                <p className="text-xs text-slate-600 mt-0.5">
                  {place.avgVisitHours ? `${place.avgVisitHours}h` : ''}
                </p>
              </div>

              {/* Connector line */}
              <div className="flex flex-col items-center shrink-0 mt-1">
                <div className="w-2.5 h-2.5 rounded-full bg-brand-500/60 border border-brand-400" />
                {idx < day.places.length - 1 && (
                  <div className="w-px flex-1 bg-navy-600 mt-1 min-h-[24px]" />
                )}
              </div>

              {/* Place info */}
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-medium text-slate-200 text-sm">
                      {CATEGORY_ICONS[place.category?.toLowerCase()] || '📍'} {place.name}
                    </p>
                    {place.category && (
                      <p className="text-xs text-slate-500 capitalize mt-0.5">{place.category}</p>
                    )}
                    {place.notes && (
                      <p className="text-xs text-slate-600 mt-1">{place.notes}</p>
                    )}
                  </div>
                  {place.ticketCost > 0 && (
                    <span className="text-xs text-slate-400 shrink-0">
                      {formatCurrency(place.ticketCost)}
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {expanded && (!day.places || day.places.length === 0) && (
        <div className="px-5 py-6 text-center text-sm text-slate-500 border-t border-navy-700">
          No attractions planned for this day.
        </div>
      )}
    </div>
  );
}
