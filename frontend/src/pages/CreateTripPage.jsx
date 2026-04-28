import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tripApi } from '../api/tripApi.js';
import { normalizeTrip } from '../utils/formatters.js';
import TripForm from '../components/trip/TripForm.jsx';
import ErrorAlert from '../components/common/ErrorAlert.jsx';

export default function CreateTripPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  const handleSubmit = async (formData) => {
    setLoading(true);
    setError('');
    try {
      console.log('[TripForge][Generate] Submitting trip creation...');
      const tripData = await tripApi.createTrip(formData);
      console.log('[TripForge][Generate] Raw response shape:', {
        hasId: !!tripData?.id,
        hasTripId: !!tripData?.tripId,
        keys: tripData ? Object.keys(tripData).slice(0, 8) : [],
      });

      const trip = normalizeTrip(tripData);
      const tripId = trip?.tripId;

      if (!tripId) {
        console.error('[TripForge][Generate] No trip ID in response — cannot navigate');
        setError('Trip was created but no trip ID was returned. Please check My Trips.');
        return;
      }

      console.log('[TripForge][Generate] Navigating to /trip/result with tripId:', tripId);
      navigate('/trip/result', { state: { trip } });
    } catch (err) {
      console.error('[TripForge][Generate] Trip creation failed:', err);
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Failed to create trip. Please try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container max-w-4xl">
      {/* Header Section */}
      <div className="relative mb-12 text-center sm:text-left">
        <div className="absolute -left-4 top-0 w-1 h-12 bg-brand-500 rounded-full hidden sm:block"></div>
        <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight text-zinc-100 mb-4">
          Plan Your <span className="text-gradient">Next Escape</span>
        </h1>
        <p className="text-lg text-zinc-500 max-w-2xl font-medium">
          Our intelligent orchestration engine will craft a bespoke itinerary based on your preferences and budget.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-8">
        <div className="relative">
          {/* Form Card */}
          <div className="card-premium p-6 sm:p-10 relative overflow-visible">
            {/* Background accent for the card */}
            <div className="absolute -top-24 -right-24 w-64 h-64 bg-brand-500/5 blur-[80px] rounded-full pointer-events-none"></div>
            
            <div className="relative z-10">
              <ErrorAlert message={error} onDismiss={() => setError('')} />
              {error && <div className="mb-6" />}
              
              <TripForm onSubmit={handleSubmit} loading={loading} />
            </div>
          </div>
          
          {/* Optional: Sidebar or info cards can go here in a grid layout on desktop */}
        </div>

        {/* Features / Benefits at bottom */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mt-8">
          {[
            { icon: '🌍', title: 'Global Coverage', desc: 'Destinations across 190+ countries supported.' },
            { icon: '⚡', title: 'Real-time Data', desc: 'Live pricing and availability checks.' },
            { icon: '🤖', title: 'AI Driven', desc: 'Personalized recommendations based on interests.' }
          ].map((item, idx) => (
            <div key={idx} className="p-6 rounded-2xl bg-zinc-900/50 border border-border/40 hover:border-border transition-colors group">
              <div className="text-2xl mb-3 group-hover:scale-110 transition-transform inline-block">{item.icon}</div>
              <h3 className="text-sm font-bold text-zinc-200 mb-1 uppercase tracking-wider">{item.title}</h3>
              <p className="text-xs text-zinc-500 leading-relaxed font-medium">{item.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
