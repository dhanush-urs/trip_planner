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
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fade-in"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-zinc-900 border border-border/60 rounded-[2rem] w-full max-w-2xl
                      max-h-[85vh] overflow-hidden shadow-2xl flex flex-col animate-scale-in">

        {/* Header */}
        <div className="flex items-center justify-between p-8 border-b border-border/40 bg-zinc-900/50">
          <div>
            <h2 className="text-xl font-black text-zinc-100 tracking-tight">Change Hotel</h2>
            <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mt-1">
              Refining Selection for <span className="text-brand-400">{trip?.destination}</span>
            </p>
          </div>
          <button
            onClick={onClose}
            className="w-10 h-10 rounded-xl bg-zinc-800 border border-border/60 hover:bg-zinc-700 text-zinc-400
                       hover:text-zinc-100 transition-all flex items-center justify-center"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-8 space-y-8 custom-scrollbar">
          <ErrorAlert message={error} onDismiss={() => setError('')} />

          {/* Reason selection */}
          <div className="space-y-4">
            <label className="input-group-label">Optimization Goal</label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {HOTEL_CHANGE_REASONS.map((r) => {
                const isSelected = reason === r.value;
                return (
                  <button
                    key={r.value}
                    onClick={() => { setReason(r.value); setAlternatives([]); setError(''); }}
                    className={`p-5 rounded-2xl border text-left transition-all duration-300 group ${
                      isSelected
                        ? 'border-brand-500/50 bg-brand-500/5 ring-1 ring-brand-500/20'
                        : 'border-border/60 bg-zinc-900/40 hover:border-zinc-700'
                    }`}
                  >
                    <p className={`text-[10px] font-bold uppercase tracking-widest mb-1 ${isSelected ? 'text-brand-400' : 'text-zinc-500'}`}>{r.label}</p>
                    <p className="text-sm font-semibold text-zinc-200">{r.desc}</p>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Action button */}
          <button
            onClick={handleSearch}
            disabled={!reason || searching}
            className="btn-primary w-full py-5 text-xs font-black uppercase tracking-[0.2em] shadow-xl group relative overflow-hidden"
          >
            {searching ? (
              <span className="flex items-center justify-center gap-3">
                <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                Scanning Alternatives...
              </span>
            ) : (
              <span className="flex items-center justify-center gap-2">
                Scan Infrastructure Alternatives
                <svg className="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
              </span>
            )}
            <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-1000"></div>
          </button>

          {/* Results */}
          {alternatives.length > 0 && (
            <div className="space-y-6 animate-slide-up">
              <div className="flex items-center gap-4">
                <div className="h-px flex-1 bg-border/40"></div>
                <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-[0.3em]">Available Options</p>
                <div className="h-px flex-1 bg-border/40"></div>
              </div>
              
              <div className="space-y-6">
                {alternatives.map((hotel) => (
                  <div key={hotel.id} className="space-y-4">
                    <HotelRecommendationCard hotel={hotel} isSelected={false} currency={trip.currency} />
                    <button
                      onClick={() => handleSelect(hotel)}
                      disabled={loading}
                      className="btn-primary w-full py-3 text-[10px] font-black uppercase tracking-widest bg-zinc-100 text-zinc-950 hover:bg-white"
                    >
                      {loading ? 'Committing...' : 'Commit to this Asset'}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
        
        {/* Footer info */}
        <div className="p-6 bg-zinc-950/50 border-t border-border/40 text-center">
          <p className="text-[9px] font-bold text-zinc-600 uppercase tracking-widest">
            AI Multi-Objective Optimization Enabled
          </p>
        </div>
      </div>
    </div>
  );
}
