import axios from 'axios';
import { storage } from '../utils/storage.js';

/**
 * API base URL strategy:
 *
 * In Docker (production/demo):
 *   - Browser loads the app from http://localhost:3000 (nginx on port 80)
 *   - All /api/* calls are RELATIVE — nginx proxies them to api-gateway:8080
 *   - VITE_API_BASE_URL is intentionally empty so no absolute URL is baked in
 *   - This means the browser NEVER needs to know about api-gateway or any
 *     internal Docker hostname
 *
 * In local dev (npm run dev / Vite dev server):
 *   - Set VITE_API_BASE_URL=http://localhost:8080 in frontend/.env
 *   - Vite dev server does NOT proxy /api — the explicit base URL is needed
 *   - The .env file is gitignored and never baked into Docker images
 *
 * Safe fallback: if VITE_API_BASE_URL is empty or missing, baseURL is ''
 * which makes axios use relative paths — correct for the nginx proxy setup.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const axiosClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 60000,   // 60s — trip planning involves AI + external APIs
});

// ── Request interceptor: attach JWT ──────────────────────────────────────────
axiosClient.interceptors.request.use(
  (config) => {
    const token = storage.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor: handle 401 ─────────────────────────────────────────
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      storage.clear();
      // Redirect to login — avoid circular import by using window.location
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
