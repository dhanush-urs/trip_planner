import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';
import { tripApi } from '../api/tripApi.js';
import { formatDate, formatCurrency } from '../utils/formatters.js';

const TIPS = [
  '🌿 Goa is best visited between November and February.',
  '🏔️ Book Manali trips early — accommodation fills up fast in peak season.',
  '🛕 Mysore Palace is most spectacular during Dasara festival.',
  '🍜 Ooty is famous for its tea gardens and homemade chocolate.',
  '🌆 Bangalore has great weather year-round — perfect for a city break.',
];

export default function DashboardPage() {
  const { user } = useAuth();
  const [recentTrips, setRecentTrips] = useState([]);
  const [loadingTrips, setLoadingTrips] = useState(true);
  const tip = TIPS[new Date().getDay() % TIPS.length];

  useEffect(() => {
    if (!user?.id) return;
    // tripApi.getUserTrips() returns TripSummaryDto[] directly ([] if empty)
    tripApi.getUserTrips(user.id)
      .then((trips) => setRecentTrips((Array.isArray(trips) ? trips : []).slice(0, 3)))
      .catch(() => {})
      .finally(() => setLoadingTrips(false));
  }, [user?.id]);

  return (
    <div className="space-y-8 animate-fade-in">

      {/* Hero */}
      <div className="card p-8 bg-gradient-to-br from-navy-800 to-navy-900
                      border-brand-500/20 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-brand-500/5 rounded-full
                        -translate-y-1/2 translate-x-1/2 pointer-events-none" />
        <div className="relative">
          <p className="text-brand-400 text-sm font-medium mb-1">Welcome back</p>
          <h1 className="text-3xl font-bold text-slate-100 mb-2">
            {user?.firstName} {user?.lastName} ✦
          </h1>
          <p className="text-slate-400 max-w-lg">
            Ready to forge your next adventure? Let AI plan the perfect trip for you.
          </p>
          <div className="flex flex-wrap gap-3 mt-6">
            <Link to="/trip/create" className="btn-primary btn-lg">
              ✦ Plan New Trip
            </Link>
            <Link to="/trip/history" className="btn-secondary btn-lg">
              📋 My Trips
            </Link>
          </div>
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[
          { icon: '🗺️', label: 'Trips Planned',   value: loadingTrips ? '…' : recentTrips.length || '0' },
          { icon: '✦',  label: 'AI Powered',       value: 'Hybrid ML' },
          { icon: '🏨', label: 'Destinations',      value: '5 Cities' },
        ].map((s) => (
          <div key={s.label} className="card p-5 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-brand-500/15 border border-brand-500/30
                            flex items-center justify-center text-2xl shrink-0">
              {s.icon}
            </div>
            <div>
              <p className="text-xl font-bold text-slate-100">{s.value}</p>
              <p className="text-xs text-slate-500">{s.label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Recent trips */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="section-title">Recent Trips</h2>
          <Link to="/trip/history" className="text-sm text-brand-400 hover:text-brand-300">
            View all →
          </Link>
        </div>

        {loadingTrips ? (
          <div className="card p-8 text-center text-slate-500 text-sm">Loading trips…</div>
        ) : recentTrips.length === 0 ? (
          <div className="card p-10 text-center">
            <p className="text-4xl mb-3">🗺️</p>
            <p className="text-slate-300 font-medium">No trips yet</p>
            <p className="text-slate-500 text-sm mt-1 mb-4">
              Create your first trip and let AI do the planning.
            </p>
            <Link to="/trip/create" className="btn-primary">Plan Your First Trip</Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {recentTrips.map((trip) => (
              <Link
                key={trip.tripId}
                to={`/trip/${trip.tripId}`}
                className="card-hover p-5 block"
              >
                <div className="flex items-start justify-between mb-3">
                  <h3 className="font-semibold text-slate-100">{trip.destination}</h3>
                  <span className="badge-cyan text-xs">{trip.status}</span>
                </div>
                <p className="text-xs text-slate-500 mb-2">
                  {formatDate(trip.startDate)} → {formatDate(trip.endDate)}
                </p>
                <p className="text-sm font-semibold text-brand-400">
                  {formatCurrency(trip.totalBudget)}
                </p>
                <p className="text-xs text-slate-600 mt-1">{trip.travelers} travelers</p>
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Tip of the day */}
      <div className="card p-5 border-brand-500/20 bg-brand-500/5">
        <p className="text-xs font-medium text-brand-400 mb-1">✦ Travel Tip</p>
        <p className="text-sm text-slate-300">{tip}</p>
      </div>
    </div>
  );
}
