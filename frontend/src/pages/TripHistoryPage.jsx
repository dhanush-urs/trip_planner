import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';
import { tripApi } from '../api/tripApi.js';
import { formatDate, formatCurrency, calcDuration } from '../utils/formatters.js';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import ErrorAlert from '../components/common/ErrorAlert.jsx';
import Badge from '../components/common/Badge.jsx';
import { SkeletonCard } from '../components/common/SkeletonLoader.jsx';

const STATUS_VARIANT = { 
  PLANNED: 'brand', 
  ACTIVE: 'success', 
  COMPLETED: 'muted', 
  CANCELLED: 'danger' 
};

export default function TripHistoryPage() {
  const { user } = useAuth();
  const [trips, setTrips]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]   = useState('');

  useEffect(() => {
    if (!user?.id) return;
    tripApi.getUserTrips(user.id)
      .then((trips) => setTrips(Array.isArray(trips) ? trips : []))
      .catch(() => setError('Failed to load trip history.'))
      .finally(() => setLoading(false));
  }, [user?.id]);

  // No longer returning LoadingSpinner immediately, will show header + skeletons
  // if (loading) return <LoadingSpinner text="Consulting your trip library..." />;

  return (
    <div className="page-container max-w-6xl space-y-10">
      
      {/* Header */}
      <header className="flex flex-col md:flex-row md:items-end justify-between gap-6 px-2">
        <div className="space-y-2">
          <h1 className="text-4xl font-extrabold tracking-tight text-zinc-100">
            Trip <span className="text-gradient">Portfolio</span>
          </h1>
          <p className="text-zinc-500 font-medium tracking-wide uppercase text-[10px]">
            {trips.length} Saved Expeditions &bull; Orchestrated via TripForge
          </p>
        </div>
        <Link to="/trip/create" className="btn-primary py-3.5 px-8 text-sm font-bold uppercase tracking-widest shadow-xl group">
          <span className="flex items-center gap-2">
            Plan New Trip
            <svg className="w-4 h-4 group-hover:rotate-12 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" /></svg>
          </span>
        </Link>
      </header>

      <ErrorAlert message={error} />

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3, 4, 5, 6].map(n => <SkeletonCard key={n} />)}
        </div>
      ) : trips.length === 0 ? (
        <div className="card-premium p-20 border-dashed border-border/60">
          <EmptyState
            icon="🗺️"
            title="Portfolio Empty"
            description="Your personal library of AI-planned expeditions is currently empty. Ready to start your first plan?"
            action={<Link to="/trip/create" className="btn-primary px-10">Start Planning</Link>}
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {trips.map((trip) => {
            const duration = trip.durationDays || calcDuration(trip.startDate, trip.endDate);
            return (
              <Link
                key={trip.tripId}
                to={`/trip/${trip.tripId}`}
                className="card-premium-hover flex flex-col group h-full"
              >
                {/* Visual Header / Destination */}
                <div className="p-6 pb-4 relative overflow-hidden flex-1">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-brand-500/5 blur-[40px] rounded-full pointer-events-none group-hover:bg-brand-500/10 transition-colors"></div>
                  
                  <div className="flex justify-between items-start mb-6">
                    <div className="w-12 h-12 rounded-xl bg-zinc-900 border border-border flex items-center justify-center text-xl shadow-inner group-hover:scale-110 transition-transform duration-500">
                      🌍
                    </div>
                    <Badge variant={STATUS_VARIANT[trip.status] || 'muted'}>
                      {trip.status}
                    </Badge>
                  </div>

                  <h3 className="text-xl font-bold text-zinc-100 group-hover:text-brand-400 transition-colors mb-1 truncate">
                    {trip.destination}
                  </h3>
                  <div className="flex items-center gap-2 text-[11px] font-bold text-zinc-500 uppercase tracking-widest">
                    <span>{formatDate(trip.startDate)}</span>
                    <span className="text-zinc-700">•</span>
                    <span>{formatDate(trip.endDate)}</span>
                  </div>
                </div>

                {/* Stats Grid */}
                <div className="px-6 py-5 bg-zinc-900/50 border-t border-border/40 grid grid-cols-3 gap-4">
                  <div className="space-y-1">
                    <p className="text-[9px] font-bold text-zinc-600 uppercase tracking-tighter">Duration</p>
                    <p className="text-sm font-bold text-zinc-200">{duration} Days</p>
                  </div>
                  <div className="space-y-1">
                    <p className="text-[9px] font-bold text-zinc-600 uppercase tracking-tighter">Budget</p>
                    <p className="text-sm font-bold text-zinc-200 truncate">{formatCurrency(trip.totalBudget, trip.currency || 'INR')}</p>
                  </div>
                  <div className="space-y-1">
                    <p className="text-[9px] font-bold text-zinc-600 uppercase tracking-tighter">Group</p>
                    <p className="text-sm font-bold text-zinc-200">{trip.travelers} Pax</p>
                  </div>
                </div>

                {/* Footer Action */}
                <div className="px-6 py-4 border-t border-border/30 flex items-center justify-between text-[10px] font-bold uppercase tracking-widest text-zinc-500 group-hover:text-brand-400 transition-colors">
                  <span>View Trip Details</span>
                  <svg className="w-3.5 h-3.5 transform group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
                </div>
              </Link>
            );
          })}
        </div>
      )}

      {/* Stats Footer or similar can be added if needed */}
      <footer className="pt-10 border-t border-border/40 text-center">
        <p className="text-[10px] font-bold text-zinc-600 uppercase tracking-[0.3em]">
          All data encrypted &bull; TripForge Operations v1.2
        </p>
      </footer>
    </div>
  );
}
