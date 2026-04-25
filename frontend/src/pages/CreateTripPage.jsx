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
      // tripApi.createTrip() returns TripResponse payload directly
      const tripData = await tripApi.createTrip(formData);
      const trip = normalizeTrip(tripData);
      navigate('/trip/result', { state: { trip } });
    } catch (err) {
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
    <div className="max-w-2xl mx-auto animate-fade-in">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-100">Plan a New Trip</h1>
        <p className="text-slate-400 text-sm mt-1">
          Fill in the details and our AI will generate a personalized itinerary.
        </p>
      </div>

      <div className="card p-8">
        <ErrorAlert message={error} onDismiss={() => setError('')} />
        {error && <div className="mb-4" />}
        <TripForm onSubmit={handleSubmit} loading={loading} />
      </div>
    </div>
  );
}
