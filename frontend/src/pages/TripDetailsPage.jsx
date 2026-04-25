import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { tripApi } from '../api/tripApi.js';
import { normalizeTrip } from '../utils/formatters.js';
import TripSummaryCard from '../components/trip/TripSummaryCard.jsx';
import HotelRecommendationCard from '../components/trip/HotelRecommendationCard.jsx';
import ItineraryCard from '../components/trip/ItineraryCard.jsx';
import BudgetBreakdownCard from '../components/trip/BudgetBreakdownCard.jsx';
import SplitExpenseCard from '../components/trip/SplitExpenseCard.jsx';
import HotelChangeModal from '../components/trip/HotelChangeModal.jsx';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import ErrorAlert from '../components/common/ErrorAlert.jsx';

export default function TripDetailsPage() {
  const { id }   = useParams();
  const navigate = useNavigate();

  const [trip, setTrip]           = useState(null);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    if (!id) return;
    // tripApi.getTrip() returns TripResponse payload directly
    tripApi.getTrip(id)
      .then((tripData) => setTrip(normalizeTrip(tripData)))
      .catch(() => setError('Failed to load trip details.'))
      .finally(() => setLoading(false));
  }, [id]);

  const handleHotelChanged = (updatedData) => {
    // updatedData is TripResponse payload (already unwrapped by tripApi.replanTrip)
    if (updatedData) setTrip(normalizeTrip(updatedData));
  };

  if (loading) return <LoadingSpinner text="Loading trip details…" />;

  if (error) return (
    <div className="max-w-2xl mx-auto mt-8 space-y-4">
      <ErrorAlert message={error} />
      <button onClick={() => navigate(-1)} className="btn-secondary">← Go Back</button>
    </div>
  );

  if (!trip) return null;

  return (
    <div className="space-y-6 animate-fade-in">

      {/* Top bar */}
      <div className="flex items-center justify-between">
        <button onClick={() => navigate('/trip/history')} className="btn-ghost btn-sm">
          ← My Trips
        </button>
        <Link to="/trip/create" className="btn-primary btn-sm">+ Plan New Trip</Link>
      </div>

      {/* Summary */}
      <TripSummaryCard trip={trip} />

      {/* Content grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Sidebar */}
        <div className="space-y-6">
          <div>
            <h2 className="section-title mb-3"><span>🏨</span> Hotel</h2>
            {trip.selectedHotel ? (
              <HotelRecommendationCard
                hotel={trip.selectedHotel}
                isSelected
                onChangeHotel={() => setShowModal(true)}
              />
            ) : (
              <div className="card p-5 text-center text-slate-500 text-sm">
                No hotel data available.
              </div>
            )}
          </div>

          {trip.budgetBreakdown && <BudgetBreakdownCard budget={trip.budgetBreakdown} />}
          {trip.splitResult     && <SplitExpenseCard    split={trip.splitResult}     />}
        </div>

        {/* Itinerary */}
        <div className="lg:col-span-2">
          <h2 className="section-title mb-3"><span>📅</span> Itinerary</h2>
          {trip.itinerary?.length > 0 ? (
            <div className="space-y-4">
              {trip.itinerary.map((day) => (
                <ItineraryCard key={day.dayNumber} day={day} />
              ))}
            </div>
          ) : (
            <div className="card p-8 text-center text-slate-500 text-sm">
              No itinerary data available.
            </div>
          )}
        </div>
      </div>

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
