import React, { useState } from 'react';

/**
 * Generate a natural travel day title from theme + day number.
 * Used when the backend doesn't provide a title.
 */
function buildDayTitle(day) {
  // Use theme if it's a real travel theme (not a placeholder)
  if (day.theme && day.theme !== 'Exploration Day' && !day.theme.includes('Tactical')) {
    return day.theme;
  }
  // Generate from day number
  const num = day.dayNumber;
  if (num === 1) return 'Arrival & First Impressions';
  // Use places/activities to infer a title
  const places = day.places || day.activities || [];
  if (places.length > 0) {
    const categories = [...new Set(places.map(p => p.category).filter(Boolean))];
    if (categories.includes('beach')) return `Day ${num} · Beach & Coastal Exploration`;
    if (categories.includes('food')) return `Day ${num} · Food & Local Flavours`;
    if (categories.includes('temple') || categories.includes('culture')) return `Day ${num} · Culture & Heritage`;
    if (categories.includes('shopping')) return `Day ${num} · Shopping & Markets`;
    if (categories.includes('nature')) return `Day ${num} · Nature & Outdoors`;
    if (categories.includes('nightlife')) return `Day ${num} · Nightlife & Entertainment`;
    if (categories.includes('adventure')) return `Day ${num} · Adventure & Thrills`;
    if (categories.includes('sightseeing')) return `Day ${num} · City Sightseeing`;
    if (categories.includes('transit') || categories.includes('leisure')) return `Day ${num} · Leisure & Departure`;
  }
  return `Day ${num} · Exploration`;
}

/**
 * Get a clean activity name from a place object.
 * Backend sends places with .name, .notes, .visitTime, .category.
 */
function getActivityName(place) {
  return place.name || place.activity || place.title || 'Local Activity';
}

/**
 * Get a description for a place — use notes if available.
 */
function getActivityDescription(place) {
  if (place.notes && !place.notes.startsWith('Avg visit:')) return place.notes;
  const parts = [];
  if (place.visitTime) parts.push(place.visitTime);
  if (place.avgVisitHours) parts.push(`~${place.avgVisitHours}h`);
  if (place.category) parts.push(place.category);
  return parts.join(' · ') || null;
}

const TIME_LABELS = ['Morning', 'Afternoon', 'Evening', 'Night'];
const TIME_COLORS = [
  { dot: 'bg-amber-400',  text: 'text-amber-400',  line: 'from-amber-400/30' },
  { dot: 'bg-brand-400',  text: 'text-brand-400',  line: 'from-brand-400/30' },
  { dot: 'bg-violet-400', text: 'text-violet-400', line: 'from-violet-400/30' },
  { dot: 'bg-zinc-400',   text: 'text-zinc-400',   line: 'from-zinc-400/30'  },
];

export default function ItineraryCard({ day }) {
  const [expanded, setExpanded] = useState(true);

  if (!day) return null;

  // Backend sends places; support both field names for safety
  const activities = day.places || day.activities || [];
  const title = buildDayTitle(day);
  const activityCount = activities.length;

  return (
    <div className={`rounded-3xl border transition-all duration-300 overflow-hidden ${
      expanded
        ? 'bg-[#121214] border-zinc-800/80 shadow-xl'
        : 'bg-zinc-900/30 border-zinc-800/60 hover:bg-zinc-900/50'
    }`}>

      {/* Header — always visible */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between p-7 hover:bg-white/[0.02] transition-colors text-left"
      >
        <div className="flex items-center gap-5">
          {/* Day number badge */}
          <div className="w-14 h-14 rounded-2xl bg-zinc-950 border border-zinc-800 flex flex-col items-center justify-center shrink-0">
            <span className="text-[9px] font-bold text-zinc-600 uppercase tracking-widest leading-none mb-0.5">Day</span>
            <span className="text-2xl font-black text-zinc-100 leading-none">{day.dayNumber}</span>
          </div>

          <div className="space-y-1.5">
            <h3 className="text-lg font-bold text-zinc-100 leading-tight">
              {title}
            </h3>
            <div className="flex items-center gap-3 text-xs text-zinc-500">
              {day.date && (
                <span>{new Date(day.date).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })}</span>
              )}
              {day.date && activityCount > 0 && <span className="text-zinc-700">·</span>}
              {activityCount > 0 && (
                <span>{activityCount} {activityCount === 1 ? 'activity' : 'activities'}</span>
              )}
              {/* Fallback badge */}
              {day.fallbackUsed && (
                <>
                  <span className="text-zinc-700">·</span>
                  <span className="text-amber-500/70">Smart defaults</span>
                </>
              )}
            </div>
          </div>
        </div>

        <div className={`w-9 h-9 rounded-full border border-zinc-800 flex items-center justify-center transition-transform duration-300 ${expanded ? 'rotate-180' : ''}`}>
          <svg className="w-4 h-4 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </button>

      {/* Expanded activities */}
      {expanded && (
        <div className="px-7 pb-8">
          {activityCount > 0 ? (
            <div className="relative pl-10 space-y-8 before:absolute before:left-[7px] before:top-2 before:bottom-2 before:w-[2px] before:bg-gradient-to-b before:from-brand-500/30 before:via-zinc-800 before:to-zinc-900 before:rounded-full">
              {activities.map((activity, idx) => {
                const colors = TIME_COLORS[idx % TIME_COLORS.length];
                const timeLabel = TIME_LABELS[idx] || `Stop ${idx + 1}`;
                const name = getActivityName(activity);
                const description = getActivityDescription(activity);

                return (
                  <div key={idx} className="relative">
                    {/* Timeline dot */}
                    <div className={`absolute -left-[37px] top-1.5 w-3 h-3 rounded-full border-2 border-[#121214] z-10 ${colors.dot}`}></div>

                    <div className="space-y-2">
                      <p className={`text-[10px] font-bold uppercase tracking-widest ${colors.text}`}>
                        {timeLabel}
                        {activity.visitTime ? ` · ${activity.visitTime}` : ''}
                      </p>
                      <h4 className="text-base font-semibold text-zinc-100 leading-snug">{name}</h4>
                      {description && (
                        <p className="text-xs text-zinc-500 leading-relaxed">{description}</p>
                      )}
                      {/* Ticket cost if non-zero */}
                      {activity.ticketCost > 0 && (
                        <p className="text-xs text-zinc-600">
                          Entry: {activity.ticketCost}
                        </p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            /* No activities — show a helpful message, not an empty shell */
            <div className="py-6 text-center">
              <p className="text-sm text-zinc-500">Activities for this day will be shown here.</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
