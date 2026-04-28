import React, { useState, useEffect } from 'react';
import { useLocation, useParams, useNavigate, Link } from 'react-router-dom';
import { tripApi } from '../api/tripApi.js';
import { normalizeTrip } from '../utils/formatters.js';
import TripSummaryCard from '../components/trip/TripSummaryCard.jsx';
import HotelRecommendationCard from '../components/trip/HotelRecommendationCard.jsx';
import ItineraryCard from '../components/trip/ItineraryCard.jsx';
import BudgetBreakdownCard from '../components/trip/BudgetBreakdownCard.jsx';
import SplitExpenseCard from '../components/trip/SplitExpenseCard.jsx';
import PaymentsSection from '../components/trip/PaymentsSection.jsx';
import HotelChangeModal from '../components/trip/HotelChangeModal.jsx';
import ErrorAlert from '../components/common/ErrorAlert.jsx';
import SkeletonLoader, { SkeletonItinerary, SkeletonCard } from '../components/common/SkeletonLoader.jsx';

export default function TripResultPage() {
  const location = useLocation();
  const { id }   = useParams();
  const navigate = useNavigate();

  const [trip, setTrip]           = useState(() => {
    try {
      const st = location.state?.trip;
      if (st && typeof st === 'object') {
        console.log('[TripForge][TripDetails] Loaded from nav state, tripId:', st.tripId);
        return st;
      }
    } catch (e) { console.error('[TripForge][TripDetails] Nav state error:', e); }
    return null;
  });
  const [loading, setLoading]     = useState(!location.state?.trip);
  const [error, setError]         = useState('');
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    if (!location.state?.trip && id) {
      console.log('[TripForge][TripDetails] Fetching trip by id:', id);
      setLoading(true);
      tripApi.getTrip(id)
        .then((tripData) => {
          console.log('[TripForge][TripDetails] Fetched, normalizing...');
          setTrip(normalizeTrip(tripData));
        })
        .catch((err) => {
          console.error('[TripForge][TripDetails] Fetch failed:', err);
          setError('Could not load trip details. Please try again.');
        })
        .finally(() => setLoading(false));
    }
  }, [id]);

  const handleHotelChanged = (updatedTripData) => {
    if (updatedTripData) setTrip(normalizeTrip(updatedTripData));
  };

  // ── Never render a blank page ─────────────────────────────────────────────
  if (loading) return (
    <div className="page-container max-w-[1440px] space-y-10 pb-20 pt-10">
      <div className="h-80 w-full rounded-3xl bg-zinc-900/50 animate-pulse" />
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
        <div className="lg:col-span-8 space-y-6">
          {[1,2,3].map(i => <div key={i} className="h-40 rounded-3xl bg-zinc-900/50 animate-pulse" />)}
        </div>
        <div className="lg:col-span-4 space-y-6">
          <div className="h-64 rounded-3xl bg-zinc-900/50 animate-pulse" />
          <div className="h-48 rounded-3xl bg-zinc-900/50 animate-pulse" />
        </div>
      </div>
    </div>
  );

  if (error) return (
    <div className="page-container max-w-2xl py-20">
      <div className="card-premium p-10 border-red-500/20 bg-red-500/5 rounded-3xl space-y-6 text-center">
        <p className="text-4xl">⚠️</p>
        <div>
          <p className="text-base font-bold text-zinc-100">Could not load trip details</p>
          <p className="text-sm text-zinc-500 mt-1">{error}</p>
        </div>
        <div className="flex gap-3 justify-center flex-wrap">
          <Link to="/trip/history" className="px-6 py-2.5 rounded-xl bg-brand-500 text-zinc-950 text-sm font-bold hover:bg-brand-400 transition-all">My Trips</Link>
          <Link to="/dashboard" className="px-6 py-2.5 rounded-xl bg-zinc-900 border border-zinc-800 text-sm font-semibold text-zinc-300 hover:text-zinc-100 transition-all">Dashboard</Link>
        </div>
      </div>
    </div>
  );

  if (!trip) return (
    <div className="page-container max-w-2xl py-20">
      <div className="card-premium p-10 rounded-3xl space-y-6 text-center">
        <p className="text-4xl">🗺️</p>
        <div>
          <p className="text-base font-bold text-zinc-100">Trip created, but details are unavailable</p>
          <p className="text-sm text-zinc-500 mt-1">Your trip was saved. Check My Trips to find it.</p>
        </div>
        <div className="flex gap-3 justify-center flex-wrap">
          <Link to="/trip/history" className="px-6 py-2.5 rounded-xl bg-brand-500 text-zinc-950 text-sm font-bold hover:bg-brand-400 transition-all">My Trips</Link>
          <Link to="/trip/create" className="px-6 py-2.5 rounded-xl bg-zinc-900 border border-zinc-800 text-sm font-semibold text-zinc-300 hover:text-zinc-100 transition-all">Plan New Trip</Link>
        </div>
      </div>
    </div>
  );

  return (
    <div className="page-container max-w-[1440px] space-y-10 pb-20">

      <nav className="flex items-center justify-between px-2">
        <button
          onClick={() => navigate(-1)}
          className="group flex items-center gap-3 text-sm font-semibold text-zinc-500 hover:text-brand-400 transition-all"
        >
          <div className="w-10 h-10 rounded-full border border-zinc-800 flex items-center justify-center group-hover:border-brand-500/40 transition-all">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M15 19l-7-7 7-7" />
            </svg>
          </div>
          Back
        </button>
        <div className="flex gap-4">
          <Link to="/trip/create" className="text-sm font-semibold text-zinc-500 hover:text-brand-400 transition-all py-2">
            + Plan New Trip
          </Link>
          <Link to="/trip/history" className="px-5 py-2 bg-zinc-900 border border-zinc-800 rounded-xl text-sm font-semibold text-zinc-400 hover:text-zinc-100 transition-all">
            My Trips
          </Link>
        </div>
      </nav>

      {loading ? (
        <SkeletonLoader variant="rect" className="h-80 w-full rounded-3xl" />
      ) : (
        <TripSummaryCard trip={trip} />
      )}

      {!loading && trip?.providerMode === 'FALLBACK' && (
        <div className="flex items-start gap-4 p-5 rounded-2xl bg-amber-500/5 border border-amber-500/15">
          <span className="text-amber-400 text-lg shrink-0">✦</span>
          <div>
            <p className="text-sm font-semibold text-amber-400">Smart defaults applied</p>
            <p className="text-xs text-zinc-500 mt-0.5 leading-relaxed">
              Live provider data was temporarily unavailable. TripForge generated a curated plan based on your destination, interests, and budget.
            </p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">

        <div className="lg:col-span-8 space-y-8">
          <div>
            <h2 className="text-2xl font-bold text-zinc-100">Day-by-Day Itinerary</h2>
            <p className="text-xs text-zinc-500 mt-1">{trip?.durationDays ?? 0} days · {trip?.destination}</p>
          </div>

          {loading ? (
            <div className="space-y-6"><SkeletonItinerary /><SkeletonItinerary /></div>
          ) : trip?.itinerary?.length > 0 ? (
            <div className="space-y-6">
              {trip.itinerary.map((day) => (
                <ItineraryCard key={day.dayNumber} day={day} />
              ))}
            </div>
          ) : (
            <div className="card-premium p-12 text-center rounded-3xl border-dashed border-zinc-800">
              <p className="text-4xl mb-4">🗺️</p>
              <p className="text-base font-semibold text-zinc-300">No itinerary available</p>
              <p className="text-sm text-zinc-500 mt-2">Try creating a new trip — itinerary generation is always available.</p>
            </div>
          )}

          {!loading && (trip?.aiHeadline || trip?.aiSummary) && (
            <section className="space-y-4 pt-6">
              <h3 className="text-lg font-bold text-zinc-100">AI Travel Insights</h3>
              <div className="card-premium p-8 bg-[#0a0a0b] border-brand-500/20">
                <div className="space-y-4">
                  <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-brand-500/10 border border-brand-500/20">
                    <span className="w-1.5 h-1.5 rounded-full bg-brand-500 animate-pulse"></span>
                    <span className="text-[10px] font-bold text-brand-400 uppercase tracking-widest">AI Generated</span>
                  </div>
                  {trip.aiHeadline && <h4 className="text-xl font-bold text-zinc-100 leading-snug">{trip.aiHeadline}</h4>}
                  {trip.aiSummary && <p className="text-sm text-zinc-400 leading-relaxed italic">"{trip.aiSummary}"</p>}
                </div>
              </div>
            </section>
          )}
        </div>

        <div className="lg:col-span-4 space-y-8">
          <section className="space-y-4">
            <h3 className="text-lg font-bold text-zinc-100">Recommended Hotel</h3>
            {loading ? (
              <SkeletonCard />
            ) : trip?.selectedHotel ? (
              <HotelRecommendationCard hotel={trip.selectedHotel} isSelected currency={trip.currency} onChangeHotel={() => setShowModal(true)} />
            ) : (
              <div className="card-premium p-10 text-center rounded-3xl border-dashed border-zinc-800">
                <p className="text-3xl mb-3">🏨</p>
                <p className="text-sm font-semibold text-zinc-400">No hotel recommendation yet</p>
              </div>
            )}
          </section>

          {!loading && trip?.alternativeHotels?.length > 0 && (
            <section className="space-y-4">
              <h3 className="text-lg font-bold text-zinc-100">Alternative Stays</h3>
              <div className="space-y-4">
                {trip.alternativeHotels.map((hotel, i) => (
                  <HotelRecommendationCard key={hotel.id ?? i} hotel={hotel} isSelected={false} currency={trip.currency} />
                ))}
              </div>
            </section>
          )}

          <section className="space-y-4">
            <h3 className="text-lg font-bold text-zinc-100">Budget Overview</h3>
            {loading ? (
              <div className="space-y-6">
                <SkeletonLoader variant="rect" className="h-64 w-full rounded-3xl" />
                <SkeletonLoader variant="rect" className="h-80 w-full rounded-3xl" />
              </div>
            ) : (
              <div className="space-y-6">
                {trip?.budgetBreakdown && <BudgetBreakdownCard budget={trip.budgetBreakdown} />}
                {trip && <TransportEstimationCard trip={trip} />}
                {(trip?.splitResult || trip?.travelers) && <SplitExpenseCard split={trip.splitResult} trip={trip} />}
                <PaymentsSection trip={trip} />
              </div>
            )}
          </section>
        </div>
      </div>

      {showModal && (
        <HotelChangeModal trip={trip} currentHotel={trip.selectedHotel} onClose={() => setShowModal(false)} onHotelChanged={handleHotelChanged} />
      )}
    </div>
  );
}
