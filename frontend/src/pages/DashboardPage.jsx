import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';
import { tripApi } from '../api/tripApi.js';
import { formatDate, formatCurrency } from '../utils/formatters.js';
import Badge from '../components/common/Badge.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import { SkeletonCard } from '../components/common/SkeletonLoader.jsx';

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
    tripApi.getUserTrips(user.id)
      .then((trips) => setRecentTrips((Array.isArray(trips) ? trips : []).slice(0, 3)))
      .catch(() => {})
      .finally(() => setLoadingTrips(false));
  }, [user?.id]);

  return (
    <div className="page-container space-y-12 max-w-6xl">
      
      {/* Page Header / Welcome */}
      <header className="relative py-12 px-8 rounded-[2rem] bg-zinc-900 border border-border/40 overflow-hidden shadow-2xl">
        {/* Abstract background decorative elements */}
        <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-brand-500/5 blur-[120px] rounded-full -translate-y-1/2 translate-x-1/2 pointer-events-none"></div>
        <div className="absolute bottom-0 left-0 w-[300px] h-[300px] bg-accent-500/5 blur-[100px] rounded-full translate-y-1/2 -translate-x-1/2 pointer-events-none"></div>
        
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-8">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand-500/10 border border-brand-500/20">
              <span className="w-1.5 h-1.5 rounded-full bg-brand-500 animate-pulse"></span>
              <span className="text-[10px] font-bold text-brand-400 uppercase tracking-widest">Active Session</span>
            </div>
            <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-zinc-100">
              Welcome back, <span className="text-gradient">{user?.firstName}</span>
            </h1>
            <p className="text-zinc-500 text-lg font-medium max-w-xl leading-relaxed">
              Your AI-powered travel orchestrator is ready. Where shall we explore next?
            </p>
          </div>
          
          <div className="flex flex-col sm:flex-row gap-4 shrink-0">
            <Link to="/trip/create" className="btn-primary py-4 px-8 text-sm font-bold uppercase tracking-widest shadow-2xl group">
              <span className="flex items-center gap-2">
                Plan New Trip
                <svg className="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
              </span>
            </Link>
            <Link to="/trip/history" className="btn-secondary py-4 px-8 text-sm font-bold uppercase tracking-widest">
              My Portfolio
            </Link>
          </div>
        </div>
      </header>

      {/* Stats Quick Overview */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {[
          { label: 'Total Expeditions', value: loadingTrips ? '...' : (recentTrips.length || 0), icon: '🗺️', color: 'brand' },
          { label: 'Travel Intelligence', value: 'Pro v1.2', icon: '✦', color: 'accent' },
          { label: 'Saved Insights', value: '12 Guides', icon: '📖', color: 'zinc' },
        ].map((stat, i) => (
          <div key={i} className="card-premium p-6 flex items-center justify-between group">
            <div className="space-y-1">
              <p className="text-xs font-bold text-zinc-500 uppercase tracking-[0.2em]">{stat.label}</p>
              <p className="text-3xl font-black text-zinc-100">{stat.value}</p>
            </div>
            <div className={`w-14 h-14 rounded-2xl flex items-center justify-center text-2xl transition-all duration-300 ${
              stat.color === 'brand' ? 'bg-brand-500/10 text-brand-400 group-hover:bg-brand-500/20' : 
              stat.color === 'accent' ? 'bg-accent-500/10 text-accent-400 group-hover:bg-accent-500/20' :
              'bg-zinc-800 text-zinc-400 group-hover:bg-zinc-700'
            }`}>
              {stat.icon}
            </div>
          </div>
        ))}
      </section>

      {/* Recent Trips & Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-10">
        
        {/* Trips List */}
        <div className="lg:col-span-2 space-y-6">
          <div className="flex items-center justify-between px-2">
            <h2 className="section-title-premium">Recent Expeditions</h2>
            <Link to="/trip/history" className="text-xs font-bold text-brand-400 uppercase tracking-widest hover:text-brand-300 transition-colors">
              View Library →
            </Link>
          </div>

          {loadingTrips ? (
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
              {[1, 2, 3].map(n => <SkeletonCard key={n} />)}
            </div>
          ) : recentTrips.length === 0 ? (
            <div className="card-premium p-12 text-center border-dashed border-border/60 bg-transparent">
              <EmptyState 
                icon="🌍"
                title="Your journey awaits"
                description="You haven't planned any trips yet. Let our AI assistant build your first itinerary in seconds."
                action={<Link to="/trip/create" className="btn-primary px-8">Forge First Trip</Link>}
              />
            </div>
          ) : (
            <div className="space-y-4">
              {recentTrips.map((trip) => (
                <Link
                  key={trip.tripId}
                  to={`/trip/${trip.tripId}`}
                  className="card-premium-hover flex items-center p-5 group"
                >
                  <div className="w-12 h-12 rounded-xl bg-zinc-800 border border-border flex items-center justify-center text-xl shrink-0 group-hover:border-brand-500/30 group-hover:bg-brand-500/5 transition-all">
                    📍
                  </div>
                  <div className="ml-5 flex-1 min-w-0">
                    <div className="flex items-center gap-3">
                      <h3 className="font-bold text-zinc-100 group-hover:text-brand-400 transition-colors truncate">{trip.destination}</h3>
                      <Badge variant={trip.status === 'PLANNED' ? 'brand' : 'muted'}>{trip.status}</Badge>
                    </div>
                    <p className="text-xs font-medium text-zinc-500 mt-1 uppercase tracking-wider">
                      {formatDate(trip.startDate)} — {formatDate(trip.endDate)}
                    </p>
                  </div>
                  <div className="text-right ml-4 shrink-0">
                    <p className="text-sm font-bold text-zinc-200">{formatCurrency(trip.totalBudget, trip.currency || 'INR')}</p>
                    <p className="text-[10px] font-bold text-zinc-600 uppercase tracking-tighter mt-0.5">{trip.travelers} Travelers</p>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Sidebar Actions / Insights */}
        <div className="space-y-6">
          <h2 className="section-title-premium px-2">Quick Actions</h2>
          <div className="card-premium p-6 space-y-4">
            <Link to="/trip/create" className="flex items-center gap-4 p-4 rounded-xl bg-zinc-900 border border-border/40 hover:border-brand-500/40 hover:bg-zinc-800 transition-all group">
              <div className="w-10 h-10 rounded-lg bg-brand-500/10 flex items-center justify-center text-brand-400">✦</div>
              <div className="flex-1">
                <p className="text-xs font-bold text-zinc-100">Plan New Trip</p>
                <p className="text-[10px] text-zinc-500 mt-0.5">Start fresh with AI</p>
              </div>
            </Link>
            <button className="w-full flex items-center gap-4 p-4 rounded-xl bg-zinc-900 border border-border/40 hover:border-zinc-700 hover:bg-zinc-800 transition-all text-left">
              <div className="w-10 h-10 rounded-lg bg-zinc-800 flex items-center justify-center text-zinc-500">📥</div>
              <div className="flex-1">
                <p className="text-xs font-bold text-zinc-100">Import Booking</p>
                <p className="text-[10px] text-zinc-500 mt-0.5">Sync from email</p>
              </div>
            </button>
          </div>

          <div className="card-premium p-6 bg-gradient-to-br from-brand-500/10 via-transparent to-transparent border-brand-500/20">
            <div className="flex items-center gap-2 text-brand-400 mb-3">
              <span className="text-lg">✦</span>
              <p className="text-[10px] font-bold uppercase tracking-[0.2em]">Travel Insight</p>
            </div>
            <p className="text-sm font-semibold text-zinc-200 leading-relaxed italic">
              "{tip}"
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
