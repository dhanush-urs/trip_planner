/**
 * Auth API module.
 *
 * Convention: every function unwraps the backend ApiResponse<T> wrapper
 * and returns the payload directly. Consumers receive AuthResponse,
 * UserProfileDto, etc. — never the raw ApiResponse envelope.
 *
 * Backend shape:  { success, message, data: <T>, timestamp }
 * What we return: <T>
 */
import axiosClient from './axiosClient.js';

export const authApi = {
  /** Returns AuthResponse: { token, tokenType, userId, email, firstName, lastName, expiresIn } */
  register: (data) =>
    axiosClient.post('/api/auth/register', data).then((r) => r.data.data),

  /** Returns AuthResponse: { token, tokenType, userId, email, firstName, lastName, expiresIn } */
  login: (data) =>
    axiosClient.post('/api/auth/login', data).then((r) => r.data.data),

  /** Returns UserProfileDto: { id, email, firstName, lastName, phone, role, createdAt } */
  getProfile: () =>
    axiosClient.get('/api/users/profile').then((r) => r.data.data),

  /** Returns UserPreferenceDto: { interests, hotelPreference, defaultBudget, defaultTravelers } */
  updatePreferences: (data) =>
    axiosClient.put('/api/users/preferences', data).then((r) => r.data.data),

  /** Returns UserPreferenceDto: { interests, hotelPreference, defaultBudget, defaultTravelers } */
  getPreferences: () =>
    axiosClient.get('/api/users/preferences').then((r) => r.data.data),
};
