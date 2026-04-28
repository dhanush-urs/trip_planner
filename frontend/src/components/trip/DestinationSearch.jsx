import React, { useState, useEffect, useRef, useCallback } from 'react';
import { locationApi } from '../../api/tripApi.js';

/**
 * Global fallback destinations shown when the provider returns no results.
 * Diverse worldwide selection — NOT a fixed primary list.
 * Only shown as last resort when Nominatim fails or returns empty.
 */
const GLOBAL_FALLBACK_DESTINATIONS = [
  { id: 'fallback-dubai',     displayName: 'Dubai, United Arab Emirates',    primaryText: 'Dubai',     secondaryText: 'United Arab Emirates', city: 'Dubai',     country: 'United Arab Emirates', countryCode: 'AE', lat: 25.2048, lng: 55.2708, type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-paris',     displayName: 'Paris, Ile-de-France, France',   primaryText: 'Paris',     secondaryText: 'Ile-de-France, France', city: 'Paris',     country: 'France',               countryCode: 'FR', lat: 48.8566, lng: 2.3522,  type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-tokyo',     displayName: 'Tokyo, Japan',                   primaryText: 'Tokyo',     secondaryText: 'Japan',                city: 'Tokyo',     country: 'Japan',                countryCode: 'JP', lat: 35.6762, lng: 139.6503,type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-london',    displayName: 'London, England, United Kingdom',primaryText: 'London',    secondaryText: 'England, United Kingdom',city: 'London',   country: 'United Kingdom',       countryCode: 'GB', lat: 51.5074, lng: -0.1278, type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-singapore', displayName: 'Singapore',                      primaryText: 'Singapore', secondaryText: 'Singapore',            city: 'Singapore', country: 'Singapore',            countryCode: 'SG', lat: 1.3521,  lng: 103.8198,type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-bali',      displayName: 'Bali, Indonesia',                primaryText: 'Bali',      secondaryText: 'Indonesia',            city: 'Bali',      country: 'Indonesia',            countryCode: 'ID', lat: -8.3405, lng: 115.0920,type: 'island',sourceProvider: 'FALLBACK' },
  { id: 'fallback-barcelona', displayName: 'Barcelona, Catalonia, Spain',    primaryText: 'Barcelona', secondaryText: 'Catalonia, Spain',     city: 'Barcelona', country: 'Spain',                countryCode: 'ES', lat: 41.3851, lng: 2.1734,  type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-sydney',    displayName: 'Sydney, New South Wales, Australia',primaryText:'Sydney',  secondaryText: 'New South Wales, Australia',city:'Sydney', country: 'Australia',           countryCode: 'AU', lat: -33.8688,lng: 151.2093,type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-newyork',   displayName: 'New York, New York, United States',primaryText:'New York', secondaryText: 'New York, United States',city:'New York', country: 'United States',        countryCode: 'US', lat: 40.7128, lng: -74.0060,type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-seoul',     displayName: 'Seoul, South Korea',             primaryText: 'Seoul',     secondaryText: 'South Korea',          city: 'Seoul',     country: 'South Korea',          countryCode: 'KR', lat: 37.5665, lng: 126.9780,type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-istanbul',  displayName: 'Istanbul, Turkey',               primaryText: 'Istanbul',  secondaryText: 'Turkey',               city: 'Istanbul',  country: 'Turkey',               countryCode: 'TR', lat: 41.0082, lng: 28.9784, type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-zurich',    displayName: 'Zurich, Switzerland',            primaryText: 'Zurich',    secondaryText: 'Switzerland',          city: 'Zurich',    country: 'Switzerland',          countryCode: 'CH', lat: 47.3769, lng: 8.5417,  type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-goa',       displayName: 'Goa, India',                     primaryText: 'Goa',       secondaryText: 'India',                city: 'Goa',       country: 'India',                countryCode: 'IN', lat: 15.2993, lng: 74.1240, type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-kyoto',     displayName: 'Kyoto, Kyoto Prefecture, Japan', primaryText: 'Kyoto',     secondaryText: 'Kyoto Prefecture, Japan',city:'Kyoto',    country: 'Japan',                countryCode: 'JP', lat: 35.0116, lng: 135.7681,type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-bangkok',   displayName: 'Bangkok, Thailand',              primaryText: 'Bangkok',   secondaryText: 'Thailand',             city: 'Bangkok',   country: 'Thailand',             countryCode: 'TH', lat: 13.7563, lng: 100.5018,type: 'city', sourceProvider: 'FALLBACK' },
  { id: 'fallback-rome',      displayName: 'Rome, Lazio, Italy',             primaryText: 'Rome',      secondaryText: 'Lazio, Italy',         city: 'Rome',      country: 'Italy',                countryCode: 'IT', lat: 41.9028, lng: 12.4964, type: 'city', sourceProvider: 'FALLBACK' },
];

/**
 * Filter the global fallback list by query prefix (case-insensitive).
 * Returns up to 6 matches.
 */
function filterFallbacks(query) {
  const q = query.toLowerCase().trim();
  return GLOBAL_FALLBACK_DESTINATIONS
    .filter(d =>
      d.primaryText.toLowerCase().startsWith(q) ||
      d.displayName.toLowerCase().includes(q) ||
      (d.country && d.country.toLowerCase().startsWith(q))
    )
    .slice(0, 6);
}

export default function DestinationSearch({ value, onChange, error }) {
  const [query, setQuery]             = useState(value || '');
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading]         = useState(false);
  const [open, setOpen]               = useState(false);
  const [activeIdx, setActiveIdx]     = useState(-1);
  const [selected, setSelected]       = useState(!!value);
  const [usingFallback, setUsingFallback] = useState(false);

  const debounceRef  = useRef(null);
  const containerRef = useRef(null);
  const inputRef     = useRef(null);

  // ── Sync external value reset ─────────────────────────────────────────────
  useEffect(() => {
    if (value === '') {
      setQuery('');
      setSuggestions([]);
      setSelected(false);
      setOpen(false);
      setUsingFallback(false);
    }
  }, [value]);

  // ── Close on outside click ────────────────────────────────────────────────
  useEffect(() => {
    const handler = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
        setActiveIdx(-1);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // ── Debounced search ──────────────────────────────────────────────────────
  const search = useCallback((q) => {
    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (!q || q.trim().length < 3) {
      setSuggestions([]);
      setOpen(false);
      setLoading(false);
      setUsingFallback(false);
      return;
    }

    setLoading(true);
    debounceRef.current = setTimeout(async () => {
      try {
        const results = await locationApi.searchDestinations(q.trim());
        if (results && results.length > 0) {
          setSuggestions(results);
          setUsingFallback(false);
        } else {
          // Provider returned empty — use filtered global fallback list
          const fallbacks = filterFallbacks(q.trim());
          setSuggestions(fallbacks);
          setUsingFallback(fallbacks.length > 0);
        }
        setOpen(true);
        setActiveIdx(-1);
      } catch {
        // Network error — use filtered global fallback list
        const fallbacks = filterFallbacks(q.trim());
        setSuggestions(fallbacks);
        setUsingFallback(fallbacks.length > 0);
        setOpen(true);
      } finally {
        setLoading(false);
      }
    }, 400);
  }, []);

  // ── Input change ──────────────────────────────────────────────────────────
  const handleChange = (e) => {
    const val = e.target.value;
    setQuery(val);
    setSelected(false);
    onChange(val);
    search(val);
  };

  // ── Select a suggestion ───────────────────────────────────────────────────
  const selectSuggestion = (suggestion) => {
    // Use primaryText as the destination string (clean city/region name)
    const label = suggestion.primaryText || suggestion.city || suggestion.displayName || '';
    setQuery(label);
    setSelected(true);
    setOpen(false);
    setSuggestions([]);
    setActiveIdx(-1);
    setUsingFallback(false);
    // Pass full suggestion as second arg — lat/lng available for downstream use
    onChange(label, suggestion);
    inputRef.current?.blur();
  };

  // ── Keyboard navigation ───────────────────────────────────────────────────
  const handleKeyDown = (e) => {
    if (!open || suggestions.length === 0) {
      if (e.key === 'Enter') setOpen(false);
      return;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIdx((i) => Math.min(i + 1, suggestions.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIdx((i) => Math.max(i - 1, -1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (activeIdx >= 0 && activeIdx < suggestions.length) {
        selectSuggestion(suggestions[activeIdx]);
      } else {
        setOpen(false);
      }
    } else if (e.key === 'Escape') {
      setOpen(false);
      setActiveIdx(-1);
    }
  };

  // ── Clear ─────────────────────────────────────────────────────────────────
  const handleClear = () => {
    setQuery('');
    setSuggestions([]);
    setSelected(false);
    setOpen(false);
    setUsingFallback(false);
    onChange('');
    inputRef.current?.focus();
  };

  const showDropdown = open && query.trim().length >= 3;

  return (
    <div ref={containerRef} className="relative group">
      <div className="relative">
        {/* Search icon */}
        <span className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500 pointer-events-none transition-colors group-focus-within:text-brand-400">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </span>

        <input
          ref={inputRef}
          type="text"
          autoComplete="off"
          spellCheck="false"
          value={query}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          onFocus={() => { if (suggestions.length > 0) setOpen(true); }}
          placeholder="Search any destination — Dubai, Paris, Tokyo, Bali…"
          aria-label="Destination"
          aria-autocomplete="list"
          aria-expanded={showDropdown}
          className={`input-premium pl-12 pr-12 ${error ? 'border-danger/50' : ''} ${
            selected ? 'border-brand-500/40 bg-brand-500/5' : ''
          }`}
        />

        {/* Right: spinner or clear */}
        <span className="absolute right-4 top-1/2 -translate-y-1/2 flex items-center">
          {loading ? (
            <svg className="animate-spin h-4 w-4 text-brand-500" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
          ) : query ? (
            <button type="button" onClick={handleClear} className="text-zinc-500 hover:text-zinc-200 transition-colors">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          ) : null}
        </span>
      </div>

      {/* Suggestions dropdown */}
      {showDropdown && (
        <div className="absolute z-[60] w-full mt-2 bg-zinc-900/95 backdrop-blur-xl border border-border/60 rounded-2xl shadow-2xl overflow-hidden">
          {/* Fallback indicator */}
          {usingFallback && (
            <div className="px-5 py-2 border-b border-zinc-800 flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
              <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest">
                Showing popular destinations — type more for exact match
              </span>
            </div>
          )}

          <ul role="listbox" className="max-h-72 overflow-y-auto py-2">
            {suggestions.length > 0 ? (
              suggestions.map((s, idx) => {
                const isActive = idx === activeIdx;
                // Use primaryText as the main label, secondaryText as context
                const primary   = s.primaryText || s.city || s.displayName || '';
                const secondary = s.secondaryText || [s.state, s.country].filter(Boolean).join(', ') || '';
                return (
                  <li
                    key={s.id || `${s.lat}-${s.lng}-${idx}`}
                    role="option"
                    aria-selected={isActive}
                    onMouseDown={(e) => { e.preventDefault(); selectSuggestion(s); }}
                    onMouseEnter={() => setActiveIdx(idx)}
                    className={`flex items-start gap-4 px-5 py-4 cursor-pointer transition-all duration-200 ${
                      isActive
                        ? 'bg-brand-500/10 text-zinc-100'
                        : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-zinc-200'
                    }`}
                  >
                    <div className={`mt-0.5 p-1.5 rounded-lg border transition-colors shrink-0 ${
                      isActive
                        ? 'bg-brand-500/20 border-brand-500/30 text-brand-400'
                        : 'bg-zinc-800 border-border text-zinc-500'
                    }`}>
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-bold truncate">{primary}</p>
                      {secondary && (
                        <p className={`text-[11px] font-medium mt-0.5 truncate ${
                          isActive ? 'text-brand-400/80' : 'text-zinc-500'
                        }`}>
                          {secondary}
                        </p>
                      )}
                    </div>
                    {/* Type badge */}
                    {s.type && (
                      <span className={`shrink-0 text-[9px] font-black uppercase tracking-widest px-2 py-1 rounded-lg ${
                        isActive ? 'bg-brand-500/20 text-brand-400' : 'bg-zinc-800 text-zinc-600'
                      }`}>
                        {s.type}
                      </span>
                    )}
                  </li>
                );
              })
            ) : (
              <li className="px-5 py-6 text-center">
                <p className="text-sm font-medium text-zinc-500">
                  No matches found for &ldquo;{query}&rdquo;
                </p>
                <p className="text-[10px] text-zinc-600 mt-1">
                  You can still type any destination and plan your trip
                </p>
              </li>
            )}
          </ul>
        </div>
      )}
    </div>
  );
}
