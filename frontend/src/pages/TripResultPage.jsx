import React, { useState, useEffect } from 'react';
import { useLocation, useParams, useNavigate, Link } from 'react-router-dom';
import { tripApi } from '../api/tripApi.js';
import { normalizeTrip, normalizeHotel } from '../utils/formatters.js';
import TripSummaryCard from '../components/trip/TripSummaryCard.jsx';
import HotelRecommendationCard from '../components/trip/HotelRecommendationCard.jsx';
import ItineraryCard from '../components/trip/ItineraryCard.jsx';
import BudgetBreakdownCard from '../components/trip/BudgetBreakdownCard.jsx';
import SplitExpenseCard from '../components/trip/SplitExpenseCard.jsx';
import HotelChangeModal from '../components/trip/HotelChangeModal.jsx';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import ErrorAlert from '../components/common/ErrorAlert.jsx';

export default function TripResultPage() {
  const location = useLocation();
  const { id }   = useParams();
  const navigate = useNavigate();

  const [trip, setTrip]               = useState(location.state?.trip || null);
  const [loading, setLoading]         = useState(!trip);
  const [error, setError]             = useState('');
  const [showModal, setShowModal]     = useState(false);

  // Fetch if no state (direct URL access)
  useEffect(() => {
    if (!trip && id) {
      setLoading(true);
      // tripApi.getTrip() returns TripResponse payload directly
      tripApi.getTrip(id)
        .then((tripData) => setTrip(normalizeTrip(tripData)))
        .catch(() => setError('Failed to load trip. It may not exist or you may not have access.'))
        .finally(() => setLoading(false));
    }
  }, [id]);

  const handleHotelChanged = (updatedTripData) => {
    // updatedTripData is TripResponse payload (already unwrapped by tripApi.replanTrip)
    if (updatedTripData) setTrip(normalizeTrip(updatedTripData));
  };

  if (loading) return <LoadingSpinner text="Loading your trip plan…" />;
  if (error)   return (
    <div className="max-w-2xl mx-auto mt-8">
      <ErrorAlert message={error} />
      <Link to="/dashboard" className="btn-secondary mt-4 inline-flex">← Back to Dashboard</Link>
    </div>
  );
  if (!trip)   return (
    <div className="text-center py-20 text-slate-500">
      No trip data found. <Link to="/trip/create" className="text-brand-400">Plan a new trip</Link>
    </div>
  );

  return (
    <div className="space-y-6 animate-fade-in">

      {/* Top bar */}
      <div className="flex items-center justify-between">
        <button onClick={() => navigate(-1)} className="btn-ghost btn-sm">
          ← Back
        </button>
        <div className="flex gap-2">
          <Link to="/trip/create" className="btn-secondary btn-sm">Plan Another</Link>
          <Link to="/trip/history" className="btn-ghost btn-sm">My Trips</Link>
        </div>
      </div>

      {/* Trip summary */}
      <TripSummaryCard trip={trip} />

      {/* Main grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Left column: hotel + budget + split */}
        <div className="space-y-6">

          {/* Hotel */}
          <div>
            <h2 className="section-title mb-3"><span>🏨</span> Recommended Hotel</h2>
            {trip.selectedHotel ? (
              <HotelRecommendationCard
                hotel={trip.selectedHotel}
                isSelected
                onChangeHotel={() => setShowModal(true)}
              />
            ) : (
              <div className="card p-6 text-center text-slate-500 text-sm">
                No hotel recommendation available.
              </div>
            )}
          </div>

          {/* Budget */}
          {trip.budgetBreakdown && <BudgetBreakdownCard budget={trip.budgetBreakdown} />}

          {/* Split */}
          {trip.splitResult && <SplitExpenseCard split={trip.splitResult} />}
        </div>

        {/* Right column: itinerary */}
        <div className="lg:col-span-2">
          <h2 className="section-title mb-3"><span>📅</span> Day-wise Itinerary</h2>
          {trip.itinerary?.length > 0 ? (
            <div className="space-y-4">
              {trip.itinerary.map((day) => (
                <ItineraryCard key={day.dayNumber} day={day} />
              ))}
            </div>
          ) : (
            <div className="card p-8 text-center text-slate-500 text-sm">
              Itinerary not available.
            </div>
          )}
        </div>
      </div>

      {/* Alternative hotels */}
      {trip.alternativeHotels?.length > 0 && (
        <div>
          <h2 className="section-title mb-3"><span>🏨</span> Alternative Hotels</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {trip.alternativeHotels.map((hotel) => (
              <HotelRecommendationCard key={hotel.id} hotel={hotel} isSelected={false} />
            ))}
          </div>
        </div>
      )}

      {/* Hotel change modal */}
      {showModal && (
        <HotelChangeModal
          trip={trip}
          currentHotel={trip.selectedHotel}
          onClose={() => setShowModal(false)}
          onHotelChanged={handleHotelChanged}
        />
      )}
    </div>
  );
}
