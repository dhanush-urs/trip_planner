import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';
import { tripApi } from '../api/tripApi.js';
import { formatDate, formatCurrency, calcDuration } from '../utils/formatters.js';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import ErrorAlert from '../components/common/ErrorAlert.jsx';
import Badge from '../components/common/Badge.jsx';

const STATUS_VARIANT = { PLANNED: 'cyan', ACTIVE: 'green', COMPLETED: 'gray', CANCELLED: 'red' };

export default function TripHistoryPage() {
  const { user } = useAuth();
  const [trips, setTrips]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]   = useState('');

  useEffect(() => {
    if (!user?.id) return;
    // tripApi.getUserTrips() returns TripSummaryDto[] directly ([] if empty)
    tripApi.getUserTrips(user.id)
      .then((trips) => setTrips(Array.isArray(trips) ? trips : []))
      .catch(() => setError('Failed to load trip history.'))
      .finally(() => setLoading(false));
  }, [user?.id]);

  if (loading) return <LoadingSpinner text="Loading your trips…" />;

  return (
    <div className="animate-fade-in">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-100">My Trips</h1>
          <p className="text-slate-400 text-sm mt-0.5">{trips.length} trip{trips.length !== 1 ? 's' : ''} planned</p>
        </div>
        <Link to="/trip/create" className="btn-primary">+ Plan New Trip</Link>
      </div>

      <ErrorAlert message={error} />

      {trips.length === 0 ? (
        <EmptyState
          icon="🗺️"
          title="No trips yet"
          description="Start planning your first AI-powered trip."
          action={<Link to="/trip/create" className="btn-primary">Plan Your First Trip</Link>}
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {trips.map((trip) => {
            const duration = trip.durationDays || calcDuration(trip.startDate, trip.endDate);
            return (
              <Link
                key={trip.tripId}
                to={`/trip/${trip.tripId}`}
                className="card-hover p-5 block group"
              >
                {/* Destination header */}
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h3 className="font-semibold text-slate-100 group-hover:text-brand-300
                                   transition-colors text-lg">
                      {trip.destination}
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {formatDate(trip.startDate)} → {formatDate(trip.endDate)}
                    </p>
                  </div>
                  <Badge variant={STATUS_VARIANT[trip.status] || 'gray'}>
                    {trip.status}
                  </Badge>
                </div>

                {/* Stats */}
                <div className="grid grid-cols-3 gap-2 mt-4">
                  <div className="bg-navy-900 rounded-lg p-2 text-center">
                    <p className="text-sm font-bold text-brand-400">{duration}d</p>
                    <p className="text-xs text-slate-600">Days</p>
                  </div>
                  <div className="bg-navy-900 rounded-lg p-2 text-center">
                    <p className="text-sm font-bold text-slate-200">{trip.travelers}</p>
                    <p className="text-xs text-slate-600">People</p>
                  </div>
                  <div className="bg-navy-900 rounded-lg p-2 text-center">
                    <p className="text-xs font-bold text-slate-200 truncate">
                      {formatCurrency(trip.totalBudget)}
                    </p>
                    <p className="text-xs text-slate-600">Budget</p>
                  </div>
                </div>

                <p className="text-xs text-brand-400/70 mt-3 text-right">
                  View details →
                </p>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
