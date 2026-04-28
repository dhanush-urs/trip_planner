import React from 'react';
import { formatDate, calcDuration, formatCurrency, resolveTripCurrency } from '../../utils/formatters.js';
import Badge from '../common/Badge.jsx';

const STATUS_VARIANT = {
  PLANNED:   'brand',
  ACTIVE:    'success',
  COMPLETED: 'muted',
  CANCELLED: 'danger',
};

export default function TripSummaryCard({ trip }) {
  if (!trip) return null;

  const duration = trip.durationDays || calcDuration(trip.startDate, trip.endDate);
  const currency = resolveTripCurrency(trip);

  return (
    <div className="card-dossier p-8 sm:p-10 relative overflow-hidden">
      {/* Subtle background glow */}
      <div className="absolute top-0 right-0 w-96 h-full bg-gradient-to-l from-brand-500/5 to-transparent pointer-events-none"></div>

      <div className="relative z-10 flex flex-col lg:flex-row lg:items-end justify-between gap-10">

        {/* Left: destination + trip info */}
        <div className="space-y-8 flex-1">
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <Badge variant={STATUS_VARIANT[trip.status] || 'muted'}>
                {trip.status}
              </Badge>
              <span className="text-xs text-zinc-600 font-medium">
                Trip #{trip.tripId?.toString().padStart(4, '0')}
              </span>
            </div>

            <h1 className="text-5xl sm:text-6xl font-black tracking-tighter text-zinc-100 leading-none">
              {trip.destination}
            </h1>
          </div>

          {/* Key stats */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-x-10 gap-y-6">
            <div>
              <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Travel Dates</p>
              <p className="text-sm font-semibold text-zinc-200">
                {formatDate(trip.startDate)} — {formatDate(trip.endDate)}
              </p>
            </div>
            <div>
              <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Duration</p>
              <p className="text-sm font-semibold text-zinc-200">{duration} {duration === 1 ? 'Night' : 'Nights'}</p>
            </div>
            <div>
              <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Travelers</p>
              <p className="text-sm font-semibold text-zinc-200">{trip.travelers} {trip.travelers === 1 ? 'Person' : 'People'}</p>
            </div>
            <div>
              <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Hotel Preference</p>
              <p className="text-sm font-semibold text-brand-400 capitalize">
                {(trip.hotelPreference || 'Standard').toLowerCase()}
              </p>
            </div>
          </div>
        </div>

        {/* Right: budget card */}
        <div className="lg:w-72 p-6 rounded-2xl bg-zinc-900/50 border border-white/5 space-y-4">
          <div>
            <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Total Budget</p>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-black text-zinc-100 tracking-tight">
                {formatCurrency(trip.totalBudget, currency)}
              </span>
              <span className="text-xs font-bold text-zinc-600 uppercase">{currency}</span>
            </div>
          </div>

          {/* Per-person estimate */}
          {trip.travelers > 1 && trip.totalBudget > 0 && (
            <div className="pt-3 border-t border-white/5">
              <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">Per Person</p>
              <p className="text-sm font-semibold text-zinc-300">
                {formatCurrency(trip.totalBudget / trip.travelers, currency)}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Interests */}
      {trip.interests?.length > 0 && (
        <div className="mt-8 pt-6 border-t border-zinc-900/50 flex flex-wrap items-center gap-2">
          <p className="text-[10px] font-bold text-zinc-600 uppercase tracking-widest mr-2">Interests</p>
          {trip.interests.map((interest, i) => (
            <span
              key={i}
              className="px-3 py-1.5 rounded-xl bg-zinc-900/60 border border-white/5 text-[10px] font-bold text-zinc-400 uppercase tracking-wider capitalize"
            >
              {interest}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
