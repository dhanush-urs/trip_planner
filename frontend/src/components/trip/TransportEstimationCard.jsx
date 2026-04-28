import React, { useState } from 'react';
import { formatCurrency, resolveTripCurrency } from '../../utils/formatters.js';

/**
 * TransportEstimationCard — shows estimated travel cost from origin to destination.
 *
 * This card provides a transparent, honest transport cost estimate.
 * All prices are clearly labeled as "Estimated" since no live fare provider
 * is currently integrated. AviationStack provides route/schedule data only,
 * not live ticket prices.
 *
 * Data model:
 *   trip.origin        — { city, countryCode } (optional, from form)
 *   trip.destination   — destination city name
 *   trip.destinationLat/Lng — coordinates
 *   trip.currency      — for formatting
 *   trip.durationDays  — trip length
 *   trip.travelers     — number of travelers
 *
 * Transport estimation logic (heuristic, clearly labeled):
 *   - Flight: distance-based fare estimate + airport transfer
 *   - Train: distance-based fare estimate + station transfer (if route plausible)
 *   - Cheapest mode highlighted
 */

// ── Heuristic fare estimation ─────────────────────────────────────────────────

/**
 * Estimate flight fare based on approximate distance.
 * Returns per-person one-way estimate in USD.
 * Clearly labeled as estimated — not from a live fare provider.
 */
function estimateFlightFare(distanceKm) {
  if (distanceKm < 200)  return 60;    // very short hop
  if (distanceKm < 500)  return 100;
  if (distanceKm < 1000) return 160;
  if (distanceKm < 2000) return 250;
  if (distanceKm < 4000) return 380;
  if (distanceKm < 8000) return 550;
  return 750;  // long-haul
}

/**
 * Estimate train fare based on distance.
 * Returns null if route is implausible (e.g. cross-ocean).
 */
function estimateTrainFare(distanceKm) {
  if (distanceKm > 1500) return null;  // train not practical for very long distances
  if (distanceKm < 50)   return 10;
  if (distanceKm < 200)  return 25;
  if (distanceKm < 500)  return 55;
  if (distanceKm < 800)  return 90;
  if (distanceKm < 1200) return 130;
  return 180;
}

/** Estimate airport/station to city-center transfer cost in USD */
function estimateLocalTransfer(distanceKm) {
  if (distanceKm < 300) return 15;   // nearby city — short transfer
  if (distanceKm < 800) return 25;
  return 35;  // major hub airport
}

/** Haversine distance between two lat/lng points in km */
function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat/2)**2 +
            Math.cos(lat1 * Math.PI/180) * Math.cos(lat2 * Math.PI/180) * Math.sin(dLng/2)**2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
}

// ── Common origin coordinates for major cities ────────────────────────────────
const KNOWN_ORIGINS = {
  'mumbai':    { lat: 19.0760, lng: 72.8777, label: 'Mumbai, India' },
  'delhi':     { lat: 28.6139, lng: 77.2090, label: 'New Delhi, India' },
  'bangalore': { lat: 12.9716, lng: 77.5946, label: 'Bangalore, India' },
  'chennai':   { lat: 13.0827, lng: 80.2707, label: 'Chennai, India' },
  'kolkata':   { lat: 22.5726, lng: 88.3639, label: 'Kolkata, India' },
  'hyderabad': { lat: 17.3850, lng: 78.4867, label: 'Hyderabad, India' },
  'london':    { lat: 51.5074, lng: -0.1278, label: 'London, UK' },
  'new york':  { lat: 40.7128, lng: -74.0060, label: 'New York, USA' },
  'paris':     { lat: 48.8566, lng: 2.3522,  label: 'Paris, France' },
  'dubai':     { lat: 25.2048, lng: 55.2708, label: 'Dubai, UAE' },
  'singapore': { lat: 1.3521,  lng: 103.8198, label: 'Singapore' },
  'tokyo':     { lat: 35.6762, lng: 139.6503, label: 'Tokyo, Japan' },
  'sydney':    { lat: -33.8688, lng: 151.2093, label: 'Sydney, Australia' },
};

function resolveOrigin(originName) {
  if (!originName) return null;
  const key = originName.toLowerCase().trim();
  return KNOWN_ORIGINS[key] || null;
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function TransportEstimationCard({ trip }) {
  const [expanded, setExpanded] = useState(false);

  if (!trip) return null;

  const currency = resolveTripCurrency(trip);
  const fmt = (v) => formatCurrency(v ?? 0, currency);
  const travelers = trip.travelers || 1;

  // Destination coordinates
  const destLat = trip.destinationLat ?? null;
  const destLng = trip.destinationLng ?? null;
  const destName = trip.destination || 'Destination';

  // Origin — from trip.origin or a common default
  const originData = resolveOrigin(trip.origin?.city || trip.originCity || '');
  const originLat = trip.origin?.lat ?? originData?.lat ?? null;
  const originLng = trip.origin?.lng ?? originData?.lng ?? null;
  const originLabel = trip.origin?.city || originData?.label || null;

  // Can't estimate without both coordinates
  if (!destLat || !destLng || !originLat || !originLng) {
    // Show a minimal card prompting origin entry
    return (
      <div className="card-premium p-6 space-y-4 bg-[#121214] rounded-3xl border border-white/5">
        <div className="flex items-center gap-3">
          <span className="text-xl">✈️</span>
          <div>
            <h3 className="text-sm font-bold text-zinc-100">Travel to {destName}</h3>
            <p className="text-[10px] text-zinc-500 mt-0.5">Transport cost estimate</p>
          </div>
        </div>
        <p className="text-xs text-zinc-500 leading-relaxed">
          Add your departure city when planning a trip to include transport costs in your budget estimate.
        </p>
      </div>
    );
  }

  // Calculate distance
  const distKm = Math.round(haversineKm(originLat, originLng, destLat, destLng));

  // Flight estimate
  const flightFarePerPerson = estimateFlightFare(distKm);
  const flightTransfer = estimateLocalTransfer(distKm);
  const flightTotalPerPerson = flightFarePerPerson + flightTransfer;
  const flightTotalAll = flightTotalPerPerson * travelers;

  // Train estimate (null if not practical)
  const trainFarePerPerson = estimateTrainFare(distKm);
  const trainTransfer = trainFarePerPerson != null ? estimateLocalTransfer(distKm) * 0.6 : null;
  const trainTotalPerPerson = trainFarePerPerson != null ? trainFarePerPerson + trainTransfer : null;
  const trainTotalAll = trainTotalPerPerson != null ? trainTotalPerPerson * travelers : null;

  // Cheapest mode
  const cheapestMode = trainTotalAll != null && trainTotalAll < flightTotalAll ? 'TRAIN' : 'FLIGHT';
  const cheapestTotal = cheapestMode === 'TRAIN' ? trainTotalAll : flightTotalAll;

  // Duration estimates (rough)
  const flightDurationH = Math.max(1, Math.round(distKm / 800 + 1.5));  // ~800 km/h + 1.5h overhead
  const trainDurationH  = trainFarePerPerson != null ? Math.round(distKm / 120) : null;  // ~120 km/h avg

  return (
    <div className="card-premium p-6 space-y-5 bg-[#121214] relative overflow-hidden rounded-3xl">
      <div className="absolute top-0 right-0 w-32 h-32 bg-sky-500/5 blur-[50px] rounded-full pointer-events-none"></div>

      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-xl">✈️</span>
          <div>
            <h3 className="text-sm font-bold text-zinc-100">Travel to {destName}</h3>
            <p className="text-[10px] text-zinc-500 mt-0.5">
              {originLabel ? `From ${originLabel}` : 'Origin to destination'} · ~{distKm.toLocaleString()} km
            </p>
          </div>
        </div>
        <button
          onClick={() => setExpanded(!expanded)}
          className="text-[10px] text-zinc-500 hover:text-brand-400 transition-colors font-semibold"
        >
          {expanded ? 'Less' : 'Details'}
        </button>
      </div>

      {/* Cheapest mode highlight */}
      <div className={`flex items-center justify-between p-4 rounded-2xl border ${
        cheapestMode === 'TRAIN'
          ? 'bg-emerald-500/5 border-emerald-500/15'
          : 'bg-sky-500/5 border-sky-500/15'
      }`}>
        <div className="flex items-center gap-3">
          <span className="text-lg">{cheapestMode === 'TRAIN' ? '🚄' : '✈️'}</span>
          <div>
            <p className="text-xs font-bold text-zinc-200">
              {cheapestMode === 'TRAIN' ? 'Train recommended' : 'Flight recommended'}
            </p>
            <p className="text-[10px] text-zinc-500">
              Cheapest option · Estimated fare
            </p>
          </div>
        </div>
        <div className="text-right">
          <p className="text-sm font-black text-zinc-100">{fmt(cheapestTotal)}</p>
          <p className="text-[10px] text-zinc-600">
            {travelers > 1 ? `${travelers} travelers` : 'per person'}
          </p>
        </div>
      </div>

      {/* Expanded comparison */}
      {expanded && (
        <div className="space-y-3 pt-1">
          {/* Flight option */}
          <div className="flex items-start justify-between p-4 rounded-2xl bg-zinc-950/50 border border-white/5">
            <div className="flex items-start gap-3">
              <span className="text-base mt-0.5">✈️</span>
              <div className="space-y-1">
                <p className="text-xs font-semibold text-zinc-200">Flight</p>
                <p className="text-[10px] text-zinc-500">~{flightDurationH}h · Fare + airport transfer</p>
                <p className="text-[9px] text-amber-500/70">Estimated fare · not from live provider</p>
              </div>
            </div>
            <div className="text-right shrink-0">
              <p className="text-sm font-bold text-zinc-100">{fmt(flightTotalAll)}</p>
              <p className="text-[10px] text-zinc-600">{fmt(flightFarePerPerson)}/person + {fmt(flightTransfer)} transfer</p>
            </div>
          </div>

          {/* Train option */}
          {trainFarePerPerson != null ? (
            <div className="flex items-start justify-between p-4 rounded-2xl bg-zinc-950/50 border border-white/5">
              <div className="flex items-start gap-3">
                <span className="text-base mt-0.5">🚄</span>
                <div className="space-y-1">
                  <p className="text-xs font-semibold text-zinc-200">Train</p>
                  <p className="text-[10px] text-zinc-500">~{trainDurationH}h · Fare + station transfer</p>
                  <p className="text-[9px] text-amber-500/70">Estimated fare · not from live provider</p>
                </div>
              </div>
              <div className="text-right shrink-0">
                <p className="text-sm font-bold text-zinc-100">{fmt(trainTotalAll)}</p>
                <p className="text-[10px] text-zinc-600">{fmt(trainFarePerPerson)}/person + {fmt(trainTransfer)} transfer</p>
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-3 p-4 rounded-2xl bg-zinc-950/50 border border-white/5 opacity-50">
              <span className="text-base">🚄</span>
              <p className="text-xs text-zinc-500">Train not practical for this distance ({distKm.toLocaleString()} km)</p>
            </div>
          )}

          {/* Disclaimer */}
          <p className="text-[9px] text-zinc-600 leading-relaxed pt-1">
            All fares are estimates based on distance. Actual prices vary by airline, season, and booking time.
            No live fare provider is currently connected.
          </p>
        </div>
      )}

      {/* Included in budget notice */}
      <div className="flex items-center justify-between pt-3 border-t border-white/5">
        <span className="text-[10px] text-zinc-500 uppercase tracking-wider">Included in budget estimate</span>
        <span className="text-xs font-semibold text-zinc-300">{fmt(cheapestTotal)}</span>
      </div>
    </div>
  );
}
