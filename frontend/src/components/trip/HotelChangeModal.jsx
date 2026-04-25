import React, { useState, useEffect } from 'react';
import { tripApi } from '../../api/tripApi.js';
import { normalizeHotel } from '../../utils/formatters.js';
import { HOTEL_CHANGE_REASONS } from '../../utils/constants.js';
import HotelRecommendationCard from './HotelRecommendationCard.jsx';
import LoadingSpinner from '../common/LoadingSpinner.jsx';
import ErrorAlert from '../common/ErrorAlert.jsx';

export default function HotelChangeModal({ trip, currentHotel, onClose, onHotelChanged }) {
  const [reason, setReason]           = useState('');
  const [alternatives, setAlternatives] = useState([]);
  const [loading, setLoading]         = useState(false);
  const [searching, setSearching]     = useState(false);
  const [error, setError]             = useState('');

  // Close on Escape
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  const handleSearch = async () => {
    if (!reason) { setError('Please select a reason for changing the hotel.'); return; }
    setError('');
    setSearching(true);
    try {
      // tripApi.changeHotel() returns HotelDto[] directly ([] if empty)
      const hotelList = await tripApi.changeHotel({
        tripId:         trip.tripId,
        currentHotelId: currentHotel?.id,
        reason,
        destination:    trip.destination,
        budget:         trip.totalBudget,
        durationDays:   trip.durationDays,
        travelers:      trip.travelers,
      });
      const hotels = (Array.isArray(hotelList) ? hotelList : []).map(normalizeHotel);
      setAlternatives(hotels);
      if (hotels.length === 0) setError('No alternatives found for this preference. Try a different reason.');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch alternatives.');
    } finally {
      setSearching(false);
    }
  };

  const handleSelect = async (hotel) => {
    setLoading(true);
    setError('');
    try {
      // tripApi.replanTrip() returns TripResponse payload directly
      const updatedTrip = await tripApi.replanTrip({
        tripId:       trip.tripId,
        newHotelId:   hotel.id,
        changeReason: reason,
      });
      onHotelChanged(updatedTrip);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update hotel.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-navy-800 border border-navy-600 rounded-2xl w-full max-w-2xl
                      max-h-[90vh] overflow-y-auto shadow-card animate-slide-up">

        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-navy-700">
          <div>
            <h2 className="text-lg font-bold text-slate-100">Change Hotel</h2>
            <p className="text-sm text-slate-400 mt-0.5">
              Currently: <span className="text-slate-200">{currentHotel?.name || '—'}</span>
            </p>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg bg-navy-700 hover:bg-navy-600 text-slate-400
                       hover:text-slate-100 transition-colors flex items-center justify-center"
          >
            ✕
          </button>
        </div>

        <div className="p-6 space-y-6">
          <ErrorAlert message={error} onDismiss={() => setError('')} />

          {/* Reason selection */}
          <div>
            <p className="label">Why do you want to change?</p>
            <div className="grid grid-cols-2 gap-3">
              {HOTEL_CHANGE_REASONS.map((r) => (
                <button
                  key={r.value}
                  onClick={() => { setReason(r.value); setAlternatives([]); setError(''); }}
                  className={`p-4 rounded-xl border text-left transition-all ${
                    reason === r.value
                      ? 'border-brand-500 bg-brand-500/10 text-brand-300'
                      : 'border-navy-600 bg-navy-900 text-slate-400 hover:border-brand-500/40'
                  }`}
                >
                  <p className="font-semibold text-sm">{r.label}</p>
                  <p className="text-xs mt-0.5 opacity-70">{r.desc}</p>
                </button>
              ))}
            </div>
          </div>

          {/* Search button */}
          <button
            onClick={handleSearch}
            disabled={!reason || searching}
            className="btn-primary w-full"
          >
            {searching ? 'Searching…' : '🔍 Find Alternatives'}
          </button>

          {/* Results */}
          {searching && <LoadingSpinner text="Finding better options…" />}

          {alternatives.length > 0 && (
            <div>
              <p className="label mb-3">Select a hotel</p>
              <div className="space-y-3">
                {alternatives.map((hotel) => (
                  <div key={hotel.id} className="relative">
                    <HotelRecommendationCard hotel={hotel} isSelected={false} />
                    <div className="mt-2">
                      <button
                        onClick={() => handleSelect(hotel)}
                        disabled={loading}
                        className="btn-primary w-full btn-sm"
                      >
                        {loading ? 'Updating…' : '✓ Select This Hotel'}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
