/**
 * Adapter/formatter utilities.
 * Normalizes backend response shapes so components never crash on undefined fields.
 */

/**
 * Resolve the canonical currency for a trip.
 * Priority: budgetBreakdown.currencyCode > splitResult.currencyCode > trip.currency > 'USD'
 * NEVER defaults to INR unless the trip was explicitly created in INR.
 *
 * @param {object} trip - normalized trip object
 * @returns {string} currency code
 */
export function resolveTripCurrency(trip) {
  if (!trip) return 'USD';
  try {
    // 1. Budget breakdown is the most authoritative — it went through FX conversion
    const budgetCurrency = trip.budgetBreakdown?.currencyCode;
    if (budgetCurrency && typeof budgetCurrency === 'string' && budgetCurrency.trim() && budgetCurrency !== 'INR') {
      return budgetCurrency.toUpperCase();
    }
    // 2. Trip-level currency (set at creation time)
    const tripCurrency = trip.currency;
    if (tripCurrency && typeof tripCurrency === 'string' && tripCurrency.trim()) {
      return tripCurrency.toUpperCase();
    }
    // 3. Split currency
    const splitCurrency = trip.splitResult?.currencyCode;
    if (splitCurrency && typeof splitCurrency === 'string' && splitCurrency !== 'INR') {
      return splitCurrency.toUpperCase();
    }
    // 4. Budget currency even if INR (explicit INR trip)
    if (budgetCurrency && typeof budgetCurrency === 'string') return budgetCurrency.toUpperCase();
    if (tripCurrency && typeof tripCurrency === 'string') return tripCurrency.toUpperCase();
  } catch (e) {
    console.error('[TripForge][Currency] resolveTripCurrency failed:', e);
  }
  return 'USD';
}

export const formatCurrency = (amount, currency = 'USD') => {
  if (amount == null || amount === '') return `${currency || 'USD'} 0`;
  const num = Number(amount);
  if (!Number.isFinite(num)) return `${currency || 'USD'} 0`;
  // Validate currency code — must be 3 uppercase letters
  const safeCurrency = (typeof currency === 'string' && /^[A-Z]{3}$/.test(currency))
    ? currency : 'USD';
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: safeCurrency,
      maximumFractionDigits: 0,
    }).format(num);
  } catch {
    return `${safeCurrency} ${num.toLocaleString('en-US')}`;
  }
};

export const formatDate = (dateStr) => {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
};

export const formatDateRange = (start, end) => {
  if (!start || !end) return '—';
  return `${formatDate(start)} → ${formatDate(end)}`;
};

export const calcDuration = (start, end) => {
  if (!start || !end) return 0;
  const diff = new Date(end) - new Date(start);
  return Math.max(1, Math.round(diff / (1000 * 60 * 60 * 24)));
};

/**
 * Normalize a hotel object from any backend shape.
 * Supports both old (sourceProvider/fallbackUsed) and new (sourceType/priceType/providerName) fields.
 */
export const normalizeHotel = (hotel) => {
  if (!hotel) return null;
  return {
    // Core fields
    id:                   hotel.id ?? hotel.hotelId ?? null,
    name:                 hotel.name ?? 'Unknown Hotel',
    destination:          hotel.destination ?? '',
    pricePerNight:        hotel.pricePerNight ?? hotel.price_per_night ?? 0,
    rating:               hotel.rating ?? 0,
    distanceFromCenterKm: hotel.distanceFromCenterKm ?? hotel.distance_from_center_km ?? 0,
    amenities:            Array.isArray(hotel.amenities) ? hotel.amenities : [],
    category:             hotel.category ?? 'STANDARD',
    popularityScore:      hotel.popularityScore ?? hotel.popularity_score ?? 0,
    relevanceScore:       hotel.relevanceScore ?? hotel.relevance_score ?? null,

    // Provider identity
    externalHotelId:  hotel.externalHotelId ?? null,

    // Truthfulness contract (new fields — Phase 10E)
    sourceType:   hotel.sourceType ?? deriveSourceType(hotel),
    priceType:    hotel.priceType  ?? derivePriceType(hotel),
    providerName: hotel.providerName ?? deriveProviderName(hotel),

    // Legacy fields — kept for backward compatibility
    sourceProvider: hotel.sourceProvider ?? 'csv_dataset',
    fallbackUsed:   hotel.fallbackUsed ?? true,
    isSynthetic:    hotel.isSynthetic ?? false,

    // Optional enrichment
    imageUrl:         hotel.imageUrl ?? null,
    reviewCount:      hotel.reviewCount ?? null,
    lat:              hotel.lat ?? null,
    lng:              hotel.lng ?? null,
    areaName:         hotel.areaName ?? null,
    bookingUrl:       hotel.bookingUrl ?? null,
    warnings:         Array.isArray(hotel.warnings) ? hotel.warnings : [],
  };
};

/** Derive sourceType from legacy sourceProvider field for backward compatibility */
function deriveSourceType(hotel) {
  const p = hotel.sourceProvider || '';
  if (p === 'TRIPFORGE_FALLBACK') return 'SYNTHETIC';
  if (p === 'csv_dataset') return 'DATASET';
  if (p.includes('overpass') || p.includes('osm')) return 'BASIC_PLACE_DATA';
  if (p.includes('google') || p.includes('amadeus')) return 'LIVE_NO_RATE';
  return hotel.fallbackUsed ? 'DATASET' : 'BASIC_PLACE_DATA';
}

/** Derive priceType from legacy fields */
function derivePriceType(hotel) {
  const p = hotel.sourceProvider || '';
  if (p === 'TRIPFORGE_FALLBACK') return 'ESTIMATED_PRICE';
  if (p === 'csv_dataset') return 'DATASET_PRICE';
  return 'ESTIMATED_PRICE';
}

/** Derive providerName from legacy sourceProvider */
function deriveProviderName(hotel) {
  const p = hotel.sourceProvider || '';
  if (p === 'TRIPFORGE_FALLBACK') return 'SYNTHETIC';
  if (p === 'csv_dataset') return 'CSV';
  if (p.includes('overpass')) return 'OVERPASS_OSM';
  if (p.includes('google')) return 'GOOGLE_PLACES';
  if (p.includes('opentripmap')) return 'OPENTRIPMAP';
  return p.toUpperCase() || 'UNKNOWN';
}

/** Normalize a budget breakdown object */
export const normalizeBudget = (budget) => {
  if (!budget) return null;
  return {
    tripId:          budget.tripId ?? null,
    hotelCost:       budget.hotelCost ?? 0,
    foodCost:        budget.foodCost ?? 0,
    transportCost:   budget.transportCost ?? 0,
    attractionCost:  budget.attractionCost ?? 0,
    miscCost:        budget.miscCost ?? 0,
    totalEstimated:  budget.totalEstimated ?? 0,
    totalBudget:     budget.totalBudget ?? 0,
    remainingBudget: budget.remainingBudget ?? 0,
    overBudget:      budget.overBudget ?? false,
    // Phase 9E: currency fields — do NOT default to INR
    currencyCode:      budget.currencyCode || null,
    exchangeRateUsed:  budget.exchangeRateUsed ?? null,
    fxSourceProvider:  budget.fxSourceProvider ?? null,
    fxFallbackUsed:    budget.fxFallbackUsed ?? false,
    warnings:          Array.isArray(budget.warnings) ? budget.warnings : [],
  };
};

/** Normalize a split result object */
export const normalizeSplit = (split) => {
  if (!split) return null;
  return {
    tripId:          split.tripId ?? null,
    totalAmount:     split.totalAmount ?? 0,
    travelers:       split.travelers ?? 1,
    perPersonAmount: split.perPersonAmount ?? 0,
    participants:    Array.isArray(split.participants)
                       ? split.participants.map(p => ({
                           participantId: p.participantId ?? null,
                           name:          p.name ?? p.participantName ?? 'Traveler',
                           email:         p.email ?? p.participantEmail ?? null,
                           amount:        p.amount ?? 0,
                           percentage:    p.percentage ?? null,
                           currencyCode:  p.currencyCode ?? null,
                         }))
                       : [],
    // Phase 9E — do NOT default to INR; let resolveTripCurrency handle fallback
    currencyCode:    split.currencyCode || null,
    // Phase 9F
    splitMode:       split.splitMode ?? 'EQUAL',
  };
};

/**
 * Normalize an itinerary day.
 * Phase 9C: handles new sourceProvider, fallbackUsed, and place-level coordinates.
 */
export const normalizeItineraryDay = (day) => {
  if (!day) return null;
  return {
    dayNumber:      day.dayNumber ?? day.day_number ?? 1,
    date:           day.date ?? null,
    theme:          day.theme ?? 'Exploration Day',
    places:         Array.isArray(day.places) ? day.places.map(normalizePlace) : [],
    // Phase 9C additions
    sourceProvider: day.sourceProvider ?? 'csv_dataset',
    fallbackUsed:   day.fallbackUsed ?? true,
  };
};

const normalizePlace = (place) => ({
  attractionId:   place.attractionId ?? null,
  name:           place.name ?? 'Attraction',
  category:       place.category ?? '',
  visitTime:      place.visitTime ?? '',
  avgVisitHours:  place.avgVisitHours ?? 1,
  ticketCost:     place.ticketCost ?? 0,
  notes:          place.notes ?? '',
  visitOrder:     place.visitOrder ?? 1,
  // Phase 9C additions
  externalPlaceId:                  place.externalPlaceId ?? null,
  lat:                              place.lat ?? null,
  lng:                              place.lng ?? null,
  travelTimeFromPreviousMinutes:    place.travelTimeFromPreviousMinutes ?? null,
});

/**
 * Normalize a full trip response.
 * Phase 9C: handles new providerMode, providerSummary, warnings, currency.
 */
export const normalizeTrip = (trip) => {
  if (!trip) return null;
  return {
    // Existing fields
    tripId:          trip.tripId ?? trip.id ?? null,
    userId:          trip.userId ?? null,
    destination:     trip.destination ?? '',
    startDate:       trip.startDate ?? null,
    endDate:         trip.endDate ?? null,
    durationDays:    trip.durationDays ?? calcDuration(trip.startDate, trip.endDate),
    totalBudget:     trip.totalBudget ?? 0,
    travelers:       trip.travelers ?? 1,
    interests:       Array.isArray(trip.interests) ? trip.interests : [],
    hotelPreference: trip.hotelPreference ?? 'STANDARD',
    status:          trip.status ?? 'PLANNED',
    createdAt:       trip.createdAt ?? null,
    itinerary:       Array.isArray(trip.itinerary)
                       ? trip.itinerary.map(normalizeItineraryDay) : [],
    selectedHotel:   normalizeHotel(trip.selectedHotel),
    alternativeHotels: Array.isArray(trip.alternativeHotels)
                         ? trip.alternativeHotels.map(normalizeHotel) : [],
    budgetBreakdown: normalizeBudget(trip.budgetBreakdown),
    splitResult:     normalizeSplit(trip.splitResult),

    // Phase 9C additions — all optional, safe defaults
    providerMode:    trip.providerMode ?? 'FALLBACK',
    providerSummary: trip.providerSummary ?? null,
    warnings:        Array.isArray(trip.warnings) ? trip.warnings : [],
    // Do NOT default to INR — use the actual trip currency or null
    currency:        trip.currency || null,

    // Destination coordinates — set when user selects from autocomplete
    destinationLat:  trip.destinationLat ?? null,
    destinationLng:  trip.destinationLng ?? null,

    // Phase 9D additions — AI enrichment fields (all optional, safe defaults)
    aiHeadline:           trip.aiHeadline ?? null,
    aiSummary:            trip.aiSummary ?? null,
    hotelExplanation:     trip.hotelExplanation ?? null,
    itineraryExplanation: trip.itineraryExplanation ?? null,
    aiEnriched:           trip.aiEnriched ?? false,
    aiProvider:           trip.aiProvider ?? null,
  };
};
