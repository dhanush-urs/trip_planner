import React from 'react';
import { formatCurrency } from '../../utils/formatters.js';

/**
 * HotelRecommendationCard — renders a single hotel recommendation.
 *
 * Truthfulness contract (Phase 10E):
 *   hotel.sourceType   — LIVE | LIVE_NO_RATE | BASIC_PLACE_DATA | DATASET | SYNTHETIC
 *   hotel.priceType    — LIVE_PRICE | ESTIMATED_PRICE | DATASET_PRICE | NO_PRICE
 *   hotel.providerName — AMADEUS | OVERPASS_OSM | GEOAPIFY | CSV | SYNTHETIC
 *
 * Badge and price label are derived from these fields, never from assumptions.
 */

/** Source badge config — honest labels only */
function getSourceBadge(hotel) {
  const st = hotel.sourceType || '';
  switch (st) {
    case 'LIVE':
      return { label: '✦ Live Hotel', cls: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' };
    case 'LIVE_NO_RATE':
      return { label: '✦ Live Hotel · No Live Rate', cls: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' };
    case 'BASIC_PLACE_DATA':
      return { label: '✦ Real Place Match', cls: 'bg-sky-500/10 border-sky-500/20 text-sky-400' };
    case 'DATASET':
      return { label: '✦ Dataset Match', cls: 'bg-zinc-800/80 border-zinc-700 text-zinc-400' };
    case 'SYNTHETIC':
      return { label: '✦ Smart Fallback', cls: 'bg-amber-500/10 border-amber-500/20 text-amber-400' };
    default:
      // Legacy fallback using sourceProvider
      if (hotel.sourceProvider === 'TRIPFORGE_FALLBACK') return { label: '✦ Smart Fallback', cls: 'bg-amber-500/10 border-amber-500/20 text-amber-400' };
      if (hotel.sourceProvider === 'csv_dataset') return { label: '✦ Dataset Match', cls: 'bg-zinc-800/80 border-zinc-700 text-zinc-400' };
      if (hotel.sourceProvider) return { label: '✦ Real Place Match', cls: 'bg-sky-500/10 border-sky-500/20 text-sky-400' };
      return null;
  }
}

/** Price label — honest labels only */
function getPriceLabel(hotel) {
  const pt = hotel.priceType || '';
  switch (pt) {
    case 'LIVE_PRICE':      return 'Nightly Rate';
    case 'ESTIMATED_PRICE': return 'Est. Nightly Rate';
    case 'DATASET_PRICE':   return 'Sample Rate';
    case 'NO_PRICE':        return 'Rate unavailable';
    default:
      // Legacy: if from OSM/overpass, price is always estimated
      if (hotel.sourceProvider?.includes('overpass') || hotel.sourceProvider?.includes('osm')) {
        return 'Est. Nightly Rate';
      }
      if (hotel.sourceProvider === 'csv_dataset') return 'Sample Rate';
      return 'Est. Nightly Rate';
  }
}

/** Helper text shown below the price */
function getHelperText(hotel) {
  const st = hotel.sourceType || '';
  const pn = (hotel.providerName || '').toUpperCase();
  switch (st) {
    case 'LIVE':             return 'Live pricing from provider';
    case 'LIVE_NO_RATE':     return 'Real hotel · live pricing unavailable';
    case 'BASIC_PLACE_DATA':
      if (pn === 'GEOAPIFY')    return 'Real nearby hotel from Geoapify · rate estimated';
      if (pn === 'OVERPASS_OSM') return 'Real nearby hotel from OpenStreetMap · rate estimated';
      if (pn === 'OPENTRIPMAP') return 'Real nearby hotel from OpenTripMap · rate estimated';
      if (pn === 'FOURSQUARE')  return 'Real nearby hotel from Foursquare · rate estimated';
      return 'Real nearby lodging · rate estimated';
    case 'DATASET':          return 'Matched from local dataset';
    case 'SYNTHETIC':        return 'Generated recommendation';
    default:                 return null;
  }
}

export default function HotelRecommendationCard({ hotel, isSelected, currency, onChangeHotel }) {
  if (!hotel) return null;

  const resolvedCurrency = currency || hotel.currencyCode || 'INR';
  const location = hotel.areaName || hotel.destination || null;
  const sourceBadge = getSourceBadge(hotel);
  const priceLabel  = getPriceLabel(hotel);
  const helperText  = getHelperText(hotel);
  const rating = hotel.rating ? Number(hotel.rating).toFixed(1) : null;
  const amenities = (hotel.amenities?.length > 0 ? hotel.amenities : ['WiFi', 'Reception'])
    .slice(0, 4)
    .map(a => typeof a === 'string' ? a.replace(/_/g, ' ') : a);

  const showPrice = hotel.priceType !== 'NO_PRICE' && hotel.pricePerNight > 0;

  return (
    <div className={`group relative rounded-3xl border transition-all duration-500 overflow-hidden ${
      isSelected
        ? 'bg-[#121214] border-brand-500/30 shadow-2xl'
        : 'bg-zinc-900/30 border-zinc-800/60 hover:border-zinc-700 hover:bg-zinc-900/50'
    }`}>

      {/* Image / visual header */}
      <div className="h-44 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-zinc-800 to-zinc-950"></div>
        <div className="absolute inset-0 flex items-center justify-center text-zinc-900 text-5xl opacity-20 select-none group-hover:scale-105 transition-transform duration-700">
          🏨
        </div>

        {/* Top-right: selected badge */}
        {isSelected && (
          <div className="absolute top-4 right-4 px-3 py-1 bg-brand-500 text-zinc-950 text-[10px] font-bold uppercase tracking-wider rounded-full z-20">
            Top Pick
          </div>
        )}

        {/* Top-left: source badge — truthful */}
        {sourceBadge && (
          <div className={`absolute top-4 left-4 z-20 px-3 py-1 rounded-full text-[9px] font-bold uppercase tracking-wider border ${sourceBadge.cls}`}>
            {sourceBadge.label}
          </div>
        )}

        {/* Bottom-left: rating + distance */}
        {rating && (
          <div className="absolute bottom-4 left-4 z-20 flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-zinc-950/70 backdrop-blur-sm border border-white/5">
            <span className="text-amber-400 text-xs">★</span>
            <span className="text-zinc-100 text-xs font-bold">{rating}</span>
            {hotel.distanceFromCenterKm > 0 && (
              <>
                <span className="h-3 w-px bg-white/10 mx-1"></span>
                <span className="text-zinc-500 text-[10px]">{hotel.distanceFromCenterKm} km from centre</span>
              </>
            )}
          </div>
        )}

        <div className="absolute inset-0 bg-gradient-to-t from-[#121214] via-transparent to-transparent"></div>
      </div>

      {/* Content */}
      <div className="p-6 space-y-5">

        {/* Name + location */}
        <div className="space-y-1.5">
          <h3 className="text-lg font-bold text-zinc-100 leading-tight group-hover:text-brand-400 transition-colors">
            {hotel.name}
          </h3>
          {location && (
            <div className="flex items-center gap-1.5 text-xs text-zinc-500">
              <svg className="w-3 h-3 text-brand-500/50 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <span>{location}</span>
            </div>
          )}
        </div>

        {/* Price + change button */}
        <div className="flex items-center justify-between pt-2 border-t border-white/5">
          <div>
            <p className="text-[10px] font-bold text-zinc-600 uppercase tracking-widest mb-1">
              {priceLabel}
            </p>
            {showPrice ? (
              <div className="flex items-baseline gap-1.5">
                <span className="text-xl font-black text-zinc-100">
                  {formatCurrency(hotel.pricePerNight, resolvedCurrency)}
                </span>
                <span className="text-[10px] font-bold text-zinc-600 uppercase">/night</span>
              </div>
            ) : (
              <span className="text-sm text-zinc-500 italic">Not available</span>
            )}
            {/* Helper text — honest context */}
            {helperText && (
              <p className="text-[9px] text-zinc-600 mt-0.5">{helperText}</p>
            )}
          </div>

          {isSelected && onChangeHotel && (
            <button
              onClick={onChangeHotel}
              className="px-4 py-2 rounded-xl bg-zinc-900 border border-zinc-800 hover:border-brand-500/30 text-xs font-semibold text-zinc-400 hover:text-brand-400 transition-all"
            >
              Change Hotel
            </button>
          )}
        </div>

        {/* Amenities */}
        <div className="grid grid-cols-2 gap-2">
          {amenities.map((amenity, i) => (
            <div key={i} className="flex items-center gap-2 px-3 py-2 rounded-xl bg-zinc-950/50 border border-white/5">
              <div className="w-1.5 h-1.5 rounded-full bg-brand-500/40 shrink-0"></div>
              <span className="text-[10px] font-medium text-zinc-500 truncate capitalize">{amenity}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
