import React, { useState } from 'react';
import { INTERESTS, HOTEL_PREFERENCES, CURRENCIES } from '../../utils/constants.js';
import DestinationSearch from './DestinationSearch.jsx';

const today = new Date().toISOString().split('T')[0];

const INITIAL = {
  destination:     '',
  startDate:       '',
  endDate:         '',
  totalBudget:     '',
  currency:        'INR',
  travelers:       2,
  interests:       [],
  hotelPreference: 'STANDARD',
};

export default function TripForm({ onSubmit, loading }) {
  const [form, setForm]     = useState(INITIAL);
  const [errors, setErrors] = useState({});
  const [destinationMeta, setDestinationMeta] = useState(null);

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

  const handleDestinationChange = (text, meta) => {
    set('destination', text);
    setDestinationMeta(meta || null);
  };

  const validate = () => {
    const e = {};
    if (!form.destination || !form.destination.trim())
      e.destination = 'Please enter a destination.';
    if (!form.startDate)    e.startDate    = 'Start date is required.';
    if (!form.endDate)      e.endDate      = 'End date is required.';
    if (form.startDate && form.endDate && form.endDate <= form.startDate)
      e.endDate = 'End date must be after start date.';
    if (!form.totalBudget || Number(form.totalBudget) <= 0)
      e.totalBudget = 'Please enter a valid budget amount.';
    if (!form.travelers || form.travelers < 1)
      e.travelers = 'At least 1 traveler required.';
    return e;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    onSubmit({
      destination:     form.destination.trim(),
      startDate:       form.startDate,
      endDate:         form.endDate,
      totalBudget:     Number(form.totalBudget),
      currency:        form.currency || 'INR',
      travelers:       Number(form.travelers),
      interests:       form.interests,
      hotelPreference: form.hotelPreference,
      // Pass destination coordinates for global hotel search (Overpass OSM)
      ...(destinationMeta?.lat != null ? {
        destinationLat: destinationMeta.lat,
        destinationLng: destinationMeta.lng,
      } : {}),
    });
  };

  const duration = form.startDate && form.endDate
    ? Math.max(0, Math.round((new Date(form.endDate) - new Date(form.startDate)) / 86400000))
    : 0;

  return (
    <form onSubmit={handleSubmit} className="space-y-12">
      
      {/* Step 1: Destination & Dates */}
      <section className="section-panel">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-8 h-8 rounded-full bg-zinc-800 border border-border flex items-center justify-center text-xs font-bold text-zinc-400">01</div>
          <h2 className="section-title-premium">Where and When?</h2>
        </div>
        
        <div className="grid grid-cols-1 gap-6">
          <div className="space-y-2">
            <label className="input-group-label">Destination</label>
            <DestinationSearch
              value={form.destination}
              onChange={handleDestinationChange}
              error={errors.destination}
            />
            {errors.destination && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.destination}</p>}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div className="space-y-2">
              <label className="input-group-label">Start Date</label>
              <div className="relative">
                <input
                  type="date" min={today} value={form.startDate}
                  onChange={(e) => set('startDate', e.target.value)}
                  className={`input-premium appearance-none ${errors.startDate ? 'border-danger/50' : ''}`}
                />
              </div>
              {errors.startDate && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.startDate}</p>}
            </div>
            <div className="space-y-2">
              <label className="input-group-label">End Date</label>
              <div className="relative">
                <input
                  type="date" min={form.startDate || today} value={form.endDate}
                  onChange={(e) => set('endDate', e.target.value)}
                  className={`input-premium appearance-none ${errors.endDate ? 'border-danger/50' : ''}`}
                />
                {duration > 0 && (
                  <div className="absolute right-3 top-3 px-2 py-0.5 rounded-md bg-brand-500/10 border border-brand-500/20 text-[10px] font-bold text-brand-400 uppercase tracking-wider">
                    {duration} Nights
                  </div>
                )}
              </div>
              {errors.endDate && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.endDate}</p>}
            </div>
          </div>
        </div>
      </section>

      {/* Step 2: Budget & Travelers */}
      <section className="section-panel">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-8 h-8 rounded-full bg-zinc-800 border border-border flex items-center justify-center text-xs font-bold text-zinc-400">02</div>
          <h2 className="section-title-premium">Group & Budget</h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-8">
          <div className="space-y-2">
            <label className="input-group-label">Total Budget</label>
            <div className="flex gap-3">
              <select
                value={form.currency}
                onChange={(e) => set('currency', e.target.value)}
                className="input-premium w-32 shrink-0 bg-surface-elevated font-semibold text-center"
              >
                {CURRENCIES.map((c) => (
                  <option key={c.value} value={c.value}>{c.symbol} {c.value}</option>
                ))}
              </select>
              <input
                type="number" min="1" step="1"
                placeholder={form.currency === 'INR' ? 'e.g. 50,000' : 'e.g. 750'}
                value={form.totalBudget}
                onChange={(e) => set('totalBudget', e.target.value)}
                className={`input-premium flex-1 ${errors.totalBudget ? 'border-danger/50' : ''}`}
              />
            </div>
            {errors.totalBudget && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.totalBudget}</p>}
            <p className="text-[10px] font-medium text-zinc-500 uppercase tracking-widest mt-2 ml-1">
              Estimated for entire duration
            </p>
          </div>

          <div className="space-y-2">
            <label className="input-group-label">Travelers</label>
            <div className="relative">
              <span className="absolute left-4 top-3 text-zinc-500">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
              </span>
              <input
                type="number" min="1" max="20" value={form.travelers}
                onChange={(e) => set('travelers', e.target.value)}
                className={`input-premium pl-12 ${errors.travelers ? 'border-danger/50' : ''}`}
              />
            </div>
            {errors.travelers && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.travelers}</p>}
          </div>
        </div>
      </section>

      {/* Step 3: Interests */}
      <section className="section-panel">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-8 h-8 rounded-full bg-zinc-800 border border-border flex items-center justify-center text-xs font-bold text-zinc-400">03</div>
          <h2 className="section-title-premium">Select Interests</h2>
        </div>
        
        <div className="flex flex-wrap gap-3">
          {INTERESTS.map((i) => (
            <button
              key={i.value}
              type="button"
              onClick={() => toggleInterest(i.value)}
              className={`chip-premium ${
                form.interests.includes(i.value) 
                  ? 'chip-premium-active' 
                  : 'chip-premium-inactive'
              }`}
            >
              <span className="text-sm">{i.label}</span>
              {form.interests.includes(i.value) && (
                <svg className="w-3.5 h-3.5 ml-1" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" /></svg>
              )}
            </button>
          ))}
        </div>
      </section>

      {/* Step 4: Hotel Preference */}
      <section className="section-panel">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-8 h-8 rounded-full bg-zinc-800 border border-border flex items-center justify-center text-xs font-bold text-zinc-400">04</div>
          <h2 className="section-title-premium">Accommodation</h2>
        </div>
        
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {HOTEL_PREFERENCES.map((p) => {
            const isSelected = form.hotelPreference === p.value;
            return (
              <button
                key={p.value}
                type="button"
                onClick={() => set('hotelPreference', p.value)}
                className={`relative p-5 rounded-2xl border text-left transition-all duration-300 group overflow-hidden ${
                  isSelected
                    ? 'border-brand-500/50 bg-brand-500/5 shadow-inner'
                    : 'border-border/60 bg-zinc-900/40 hover:border-zinc-700 hover:bg-zinc-900'
                }`}
              >
                {isSelected && (
                  <div className="absolute top-0 right-0 p-2">
                    <div className="w-5 h-5 rounded-full bg-brand-500 flex items-center justify-center">
                      <svg className="w-3 h-3 text-zinc-950" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" /></svg>
                    </div>
                  </div>
                )}
                <p className={`text-xs font-bold uppercase tracking-widest mb-1 ${
                  isSelected ? 'text-brand-400' : 'text-zinc-500'
                }`}>{p.label}</p>
                <p className="text-sm font-semibold text-zinc-200">{p.desc}</p>
                
                {/* Visual indicator for "Standard" as recommended */}
                {p.value === 'STANDARD' && !isSelected && (
                  <span className="mt-3 inline-block text-[10px] font-bold text-zinc-600 uppercase tracking-tighter">Recommended</span>
                )}
              </button>
            );
          })}
        </div>
      </section>

      {/* Footer / Submit */}
      <div className="pt-8 border-t border-border/40">
        <button 
          type="submit" 
          disabled={loading} 
          className="btn-primary w-full py-5 text-base font-bold uppercase tracking-[0.2em] relative overflow-hidden group shadow-2xl"
        >
          {loading ? (
            <span className="flex items-center justify-center gap-4">
              <svg className="animate-spin h-5 w-5 text-zinc-950" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>Orchestrating Itinerary...</span>
            </span>
          ) : (
            <span className="flex items-center justify-center gap-3">
              <span>Generate My Trip Plan</span>
              <svg className="w-5 h-5 group-hover:translate-x-1.5 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
            </span>
          )}
          {/* Subtle shine effect on hover */}
          <div className="absolute inset-0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/20 to-transparent pointer-events-none"></div>
        </button>
        <p className="text-center text-zinc-600 text-[10px] font-bold uppercase tracking-widest mt-6">
          AI Generation typically takes 8-12 seconds
        </p>
      </div>
    </form>
  );
}
