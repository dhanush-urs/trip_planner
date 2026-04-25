import React from 'react';
import { formatCurrency, formatDate } from '../../utils/formatters.js';
import Badge from '../common/Badge.jsx';
import { TRIP_STATUS_COLORS } from '../../utils/constants.js';

export default function TripSummaryCard({ trip }) {
  if (!trip) return null;
  const statusVariant = {
    PLANNED: 'cyan', ACTIVE: 'green', COMPLETED: 'gray', CANCELLED: 'red',
  }[trip.status] || 'gray';

  return (
    <div className="card p-6 animate-slide-up">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <h2 className="text-2xl font-bold text-slate-100">{trip.destination}</h2>
            <Badge variant={statusVariant}>{trip.status}</Badge>
          </div>
          <p className="text-slate-400 text-sm">
            {formatDate(trip.startDate)} → {formatDate(trip.endDate)}
          </p>
        </div>
        <div className="text-right">
          <p className="text-2xl font-bold text-brand-400">{formatCurrency(trip.totalBudget)}</p>
          <p className="text-xs text-slate-500">Total Budget</p>
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-6">
        {[
          { label: 'Duration',   value: `${trip.durationDays} days`,    icon: '📅' },
          { label: 'Travelers',  value: `${trip.travelers} people`,      icon: '👥' },
          { label: 'Hotel Pref', value: trip.hotelPreference || '—',     icon: '🏨' },
          { label: 'Interests',  value: trip.interests?.length
              ? trip.interests.slice(0, 2).join(', ') + (trip.interests.length > 2 ? '…' : '')
              : '—',                                                      icon: '🎯' },
        ].map((item) => (
          <div key={item.label} className="bg-navy-900 rounded-xl p-3">
            <p className="text-xs text-slate-500 mb-1">{item.icon} {item.label}</p>
            <p className="text-sm font-semibold text-slate-200 capitalize">{item.value}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
