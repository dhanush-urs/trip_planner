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
};
