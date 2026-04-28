import React, { createContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/authApi.js';
import { storage } from '../utils/storage.js';

export const AuthContext = createContext(null);

/**
 * Maps an axios error to a user-friendly message.
 * Never shows raw HTTP status text or internal hostnames.
 *
 * Priority:
 *   1. Backend ApiResponse.message (most specific)
 *   2. HTTP status code → friendly string
 *   3. Network/timeout error → friendly string
 *   4. Fallback default
 */
function resolveErrorMessage(err, fallback) {
  const status = err.response?.status;
  const body   = err.response?.data;

  // 1. Backend sent a structured message — use it
  if (body?.message && typeof body.message === 'string' && body.message.trim()) {
    return body.message;
  }

  // 2. Map HTTP status to friendly text
  if (status === 400) return body?.message || 'Invalid request. Please check your input.';
  if (status === 401) return 'Invalid email or password.';
  if (status === 403) return 'Access denied.';
  if (status === 404) return 'Service not found. Please try again later.';
  if (status === 409) return body?.message || 'An account with this email already exists.';
  if (status === 429) return 'Too many attempts. Please wait a moment and try again.';
  if (status === 500) return 'Server error. Please try again in a moment.';
  if (status === 502 || status === 503 || status === 504) {
    return 'Backend temporarily unavailable. Please wait and try again.';
  }

  // 3. Network / timeout errors (no response at all)
  if (!err.response) {
    if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
      return 'Request timed out. Check that the backend is running.';
    }
    return 'Network error — cannot reach the server. Check that Docker is running.';
  }

  // 4. Fallback
  return fallback;
}

export function AuthProvider({ children }) {
  const [user, setUser]       = useState(storage.getUser());
  const [token, setToken]     = useState(storage.getToken());
  const [loading, setLoading] = useState(false);
  const [initializing, setInitializing] = useState(true);

  // Restore session on mount
  useEffect(() => {
    const savedToken = storage.getToken();
    const savedUser  = storage.getUser();
    if (savedToken && savedUser) {
      setToken(savedToken);
      setUser(savedUser);
    }
    setInitializing(false);
  }, []);

  const login = useCallback(async (email, password) => {
    setLoading(true);
    try {
      // authApi.login() returns AuthResponse payload directly (wrapper already unwrapped)
      const authData = await authApi.login({ email, password });
      storage.setToken(authData.token);
      storage.setUser({
        id:        authData.userId,
        email:     authData.email,
        firstName: authData.firstName,
        lastName:  authData.lastName,
      });
      setToken(authData.token);
      setUser({
        id:        authData.userId,
        email:     authData.email,
        firstName: authData.firstName,
        lastName:  authData.lastName,
      });
      return { success: true };
    } catch (err) {
      return { success: false, message: resolveErrorMessage(err, 'Login failed. Please check your credentials.') };
    } finally {
      setLoading(false);
    }
  }, []);

  const register = useCallback(async (formData) => {
    setLoading(true);
    try {
      // authApi.register() returns AuthResponse payload directly (wrapper already unwrapped)
      const authData = await authApi.register(formData);
      storage.setToken(authData.token);
      storage.setUser({
        id:        authData.userId,
        email:     authData.email,
        firstName: authData.firstName,
        lastName:  authData.lastName,
      });
      setToken(authData.token);
      setUser({
        id:        authData.userId,
        email:     authData.email,
        firstName: authData.firstName,
        lastName:  authData.lastName,
      });
      return { success: true };
    } catch (err) {
      return { success: false, message: resolveErrorMessage(err, 'Registration failed. Please try again.') };
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    storage.clear();
    setToken(null);
    setUser(null);
  }, []);

  const isAuthenticated = Boolean(token && user);

  return (
    <AuthContext.Provider
      value={{ user, token, loading, initializing, isAuthenticated, login, register, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}
