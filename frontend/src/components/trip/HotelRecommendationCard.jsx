import React from 'react';
import { formatCurrency } from '../../utils/formatters.js';

const CATEGORY_COLORS = {
  BUDGET:   'badge-green',
  STANDARD: 'badge-cyan',
  LUXURY:   'badge-yellow',
};

export default function HotelRecommendationCard({ hotel, onChangeHotel, isSelected = true }) {
  if (!hotel) return null;

  const stars = Math.round(hotel.rating || 0);

  return (
    <div className={`card p-5 animate-slide-up ${isSelected ? 'border-brand-500/40 shadow-glow' : ''}`}>
      {isSelected && (
        <div className="flex items-center gap-2 mb-3">
          <span className="w-2 h-2 rounded-full bg-brand-400 animate-pulse" />
          <span className="text-xs font-medium text-brand-400">Selected Hotel</span>
        </div>
      )}

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex-1 min-w-0">
          <h3 className="text-lg font-semibold text-slate-100 truncate">{hotel.name}</h3>
          <p className="text-sm text-slate-400 mt-0.5">{hotel.destination}</p>
        </div>
        <div className="text-right shrink-0">
          <p className="text-xl font-bold text-brand-400">{formatCurrency(hotel.pricePerNight)}</p>
          <p className="text-xs text-slate-500">per night</p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3 mt-4">
        {/* Rating */}
        <div className="flex items-center gap-1.5 bg-navy-900 rounded-lg px-3 py-1.5">
          <span className="text-yellow-400">{'★'.repeat(stars)}{'☆'.repeat(5 - stars)}</span>
          <span className="text-sm font-semibold text-slate-200">{hotel.rating?.toFixed(1)}</span>
        </div>

        {/* Distance */}
        <div className="flex items-center gap-1.5 bg-navy-900 rounded-lg px-3 py-1.5">
          <span className="text-slate-400 text-sm">📍</span>
          <span className="text-sm text-slate-300">{hotel.distanceFromCenterKm?.toFixed(1)} km</span>
        </div>

        {/* Category */}
        <span className={CATEGORY_COLORS[hotel.category] || 'badge-gray'}>
          {hotel.category}
        </span>

        {/* Relevance score */}
        {hotel.relevanceScore != null && (
          <span className="badge-gray">
            Score: {(hotel.relevanceScore * 100).toFixed(0)}%
          </span>
        )}
      </div>

      {/* Amenities */}
      {hotel.amenities?.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mt-3">
          {hotel.amenities.slice(0, 6).map((a) => (
            <span key={a} className="text-xs bg-navy-900 text-slate-400 px-2 py-0.5 rounded-md capitalize">
              {a.replace(/_/g, ' ')}
            </span>
          ))}
          {hotel.amenities.length > 6 && (
            <span className="text-xs text-slate-500">+{hotel.amenities.length - 6} more</span>
          )}
        </div>
      )}

      {/* Change hotel button */}
      {isSelected && onChangeHotel && (
        <div className="mt-4 pt-4 border-t border-navy-700">
          <button onClick={onChangeHotel} className="btn-secondary w-full">
            🔄 Change Hotel
          </button>
        </div>
      )}
    </div>
  );
}
