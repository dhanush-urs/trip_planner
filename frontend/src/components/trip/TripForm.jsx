import React, { useState } from 'react';
import { DESTINATIONS, INTERESTS, HOTEL_PREFERENCES } from '../../utils/constants.js';
import ErrorAlert from '../common/ErrorAlert.jsx';

const today = new Date().toISOString().split('T')[0];

const INITIAL = {
  destination:     '',
  startDate:       '',
  endDate:         '',
  totalBudget:     '',
  travelers:       2,
  interests:       [],
  hotelPreference: 'STANDARD',
};

export default function TripForm({ onSubmit, loading }) {
  const [form, setForm]     = useState(INITIAL);
  const [errors, setErrors] = useState({});

  const set = (key, val) => {
    setForm((f) => ({ ...f, [key]: val }));
    setErrors((e) => ({ ...e, [key]: '' }));
  };

  const toggleInterest = (val) => {
    setForm((f) => ({
      ...f,
      interests: f.interests.includes(val)
        ? f.interests.filter((i) => i !== val)
        : [...f.interests, val],
    }));
  };

  const validate = () => {
    const e = {};
    if (!form.destination)  e.destination  = 'Please select a destination.';
    if (!form.startDate)    e.startDate    = 'Start date is required.';
    if (!form.endDate)      e.endDate      = 'End date is required.';
    if (form.startDate && form.endDate && form.endDate <= form.startDate)
      e.endDate = 'End date must be after start date.';
    if (!form.totalBudget || Number(form.totalBudget) < 1000)
      e.totalBudget = 'Budget must be at least ₹1,000.';
    if (!form.travelers || form.travelers < 1)
      e.travelers = 'At least 1 traveler required.';
    return e;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    onSubmit({
      destination:     form.destination,
      startDate:       form.startDate,
      endDate:         form.endDate,
      totalBudget:     Number(form.totalBudget),
      travelers:       Number(form.travelers),
      interests:       form.interests,
      hotelPreference: form.hotelPreference,
    });
  };

  // Compute duration for display
  const duration = form.startDate && form.endDate
    ? Math.max(0, Math.round((new Date(form.endDate) - new Date(form.startDate)) / 86400000))
    : 0;

  return (
    <form onSubmit={handleSubmit} className="space-y-6">

      {/* Destination */}
      <div>
        <label className="label">Destination *</label>
        <select
          value={form.destination}
          onChange={(e) => set('destination', e.target.value)}
          className={`input ${errors.destination ? 'input-error' : ''}`}
        >
          <option value="">Select a destination…</option>
          {DESTINATIONS.map((d) => <option key={d} value={d}>{d}</option>)}
        </select>
        {errors.destination && <p className="field-error">{errors.destination}</p>}
      </div>

      {/* Dates */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="label">Start Date *</label>
          <input
            type="date" min={today} value={form.startDate}
            onChange={(e) => set('startDate', e.target.value)}
            className={`input ${errors.startDate ? 'input-error' : ''}`}
          />
          {errors.startDate && <p className="field-error">{errors.startDate}</p>}
        </div>
        <div>
          <label className="label">End Date *</label>
          <input
            type="date" min={form.startDate || today} value={form.endDate}
            onChange={(e) => set('endDate', e.target.value)}
            className={`input ${errors.endDate ? 'input-error' : ''}`}
          />
          {errors.endDate && <p className="field-error">{errors.endDate}</p>}
          {duration > 0 && (
            <p className="text-xs text-brand-400 mt-1">📅 {duration} day{duration !== 1 ? 's' : ''}</p>
          )}
        </div>
      </div>

      {/* Budget & Travelers */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="label">Total Budget (₹) *</label>
          <input
            type="number" min="1000" step="500" placeholder="e.g. 50000"
            value={form.totalBudget}
            onChange={(e) => set('totalBudget', e.target.value)}
            className={`input ${errors.totalBudget ? 'input-error' : ''}`}
          />
          {errors.totalBudget && <p className="field-error">{errors.totalBudget}</p>}
        </div>
        <div>
          <label className="label">Number of Travelers *</label>
          <input
            type="number" min="1" max="20" value={form.travelers}
            onChange={(e) => set('travelers', e.target.value)}
            className={`input ${errors.travelers ? 'input-error' : ''}`}
          />
          {errors.travelers && <p className="field-error">{errors.travelers}</p>}
        </div>
      </div>

      {/* Interests */}
      <div>
        <label className="label">Interests</label>
        <div className="flex flex-wrap gap-2">
          {INTERESTS.map((i) => (
            <button
              key={i.value}
              type="button"
              onClick={() => toggleInterest(i.value)}
              className={form.interests.includes(i.value) ? 'chip-active' : 'chip-inactive'}
            >
              {i.label}
            </button>
          ))}
        </div>
      </div>

      {/* Hotel Preference */}
      <div>
        <label className="label">Hotel Preference</label>
        <div className="grid grid-cols-3 gap-3">
          {HOTEL_PREFERENCES.map((p) => (
            <button
              key={p.value}
              type="button"
              onClick={() => set('hotelPreference', p.value)}
              className={`p-4 rounded-xl border text-left transition-all ${
                form.hotelPreference === p.value
                  ? 'border-brand-500 bg-brand-500/10'
                  : 'border-navy-600 bg-navy-900 hover:border-brand-500/40'
              }`}
            >
              <p className={`font-semibold text-sm ${
                form.hotelPreference === p.value ? 'text-brand-300' : 'text-slate-300'
              }`}>{p.label}</p>
              <p className="text-xs text-slate-500 mt-0.5">{p.desc}</p>
            </button>
          ))}
        </div>
      </div>

      {/* Submit */}
      <button type="submit" disabled={loading} className="btn-primary btn-lg w-full">
        {loading ? (
          <span className="flex items-center gap-2">
            <span className="w-4 h-4 border-2 border-navy-900/40 border-t-navy-900
                             rounded-full animate-spin" />
            Planning your trip…
          </span>
        ) : (
          '✦ Generate My Trip Plan'
        )}
      </button>
    </form>
  );
}
