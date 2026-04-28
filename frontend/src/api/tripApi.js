/**
 * Trip API module.
 *
 * Convention: every function unwraps the backend ApiResponse<T> wrapper
 * and returns the payload directly. Consumers receive TripResponse,
 * TripSummaryDto[], HotelDto[], etc. — never the raw ApiResponse envelope.
 *
 * Backend shape:  { success, message, data: <T>, timestamp }
 * What we return: <T>
 */
import axiosClient from './axiosClient.js';

export const tripApi = {
  /** Returns TripResponse (full trip with itinerary, hotel, budget, split) */
  createTrip: (data) =>
    axiosClient.post('/api/trip/create', data).then((r) => r.data.data),

  /** Returns TripResponse */
  getTrip: (tripId) =>
    axiosClient.get(`/api/trip/${tripId}`).then((r) => r.data.data),

  /** Returns TripSummaryDto[] */
  getUserTrips: (userId) =>
    axiosClient.get(`/api/trip/user/${userId}`).then((r) => r.data.data ?? []),

  /** Returns TripResponse (updated trip after hotel change) */
  replanTrip: (data) =>
    axiosClient.put('/api/trip/replan', data).then((r) => r.data.data),

  /** Returns HotelDto[] (alternative hotels matching the change reason) */
  changeHotel: (data) =>
    axiosClient.post('/api/hotels/change', data).then((r) => r.data.data ?? []),

  /** Returns HotelDto[] */
  getAlternativeHotels: (tripId, destination, excludeHotelId) =>
    axiosClient
      .get(`/api/hotels/alternatives/${tripId}`, {
        params: { destination, excludeHotelId },
      })
      .then((r) => r.data.data ?? []),

  /** Generic split endpoint caller for custom split modes */
  callSplitEndpoint: (endpoint, payload) =>
    axiosClient.post(endpoint, payload).then((r) => r.data.data),
};

/**
 * Location / destination search API.
 * Powers the destination autocomplete in the Plan Trip form.
 * Returns LocationSuggestionDto[] from external-data-service via Nominatim.
 * Returns empty array on any failure — never throws.
 *
 * Response shape (LocationSuggestionDto):
 *   { id, displayName, primaryText, secondaryText, city, state, country,
 *     countryCode, type, lat, lng, sourceProvider }
 */
export const locationApi = {
  /**
   * Search for destination suggestions.
   * @param {string} query - free-text query, e.g. "Dubai", "Tokyo", "California"
   * @returns {Promise<Array>} array of LocationSuggestionDto
   */
  searchDestinations: (query) =>
    axiosClient
      .get('/api/external/locations/search', { params: { q: query } })
      .then((r) => r.data?.data ?? [])
      .catch(() => []),   // graceful degradation — never propagate to UI
};
