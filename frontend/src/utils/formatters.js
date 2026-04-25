/**
 * Adapter/formatter utilities.
 * Normalizes backend response shapes so components never crash on undefined fields.
 */

export const formatCurrency = (amount) => {
  if (amount == null) return '₹0';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', maximumFractionDigits: 0,
  }).format(amount);
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

/** Normalize a hotel object from any backend shape */
export const normalizeHotel = (hotel) => {
  if (!hotel) return null;
  return {
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
  };
};

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
    participants:    Array.isArray(split.participants) ? split.participants : [],
  };
};

/** Normalize an itinerary day */
export const normalizeItineraryDay = (day) => {
  if (!day) return null;
  return {
    dayNumber: day.dayNumber ?? day.day_number ?? 1,
    date:      day.date ?? null,
    theme:     day.theme ?? 'Exploration Day',
    places:    Array.isArray(day.places) ? day.places.map(normalizePlace) : [],
  };
};

const normalizePlace = (place) => ({
  attractionId:  place.attractionId ?? null,
  name:          place.name ?? 'Attraction',
  category:      place.category ?? '',
  visitTime:     place.visitTime ?? '',
  avgVisitHours: place.avgVisitHours ?? 1,
  ticketCost:    place.ticketCost ?? 0,
  notes:         place.notes ?? '',
  visitOrder:    place.visitOrder ?? 1,
});

/** Normalize a full trip response */
export const normalizeTrip = (trip) => {
  if (!trip) return null;
  return {
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
  };
};
