import React, { createContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/authApi.js';
import { storage } from '../utils/storage.js';

export const AuthContext = createContext(null);

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
      // err.response.data is the raw ApiResponse envelope from the backend
      const body = err.response?.data;
      const message =
        body?.message ||          // ApiResponse.message (preferred — human-readable)
        body?.error ||            // ApiResponse.error (error code fallback)
        err.message ||            // Axios/network error
        'Login failed. Please check your credentials.';
      return { success: false, message };
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
      // err.response.data is the raw ApiResponse envelope from the backend
      const body = err.response?.data;
      const message =
        body?.message ||          // ApiResponse.message (preferred — human-readable)
        body?.error ||            // ApiResponse.error (error code fallback)
        err.message ||            // Axios/network error
        'Registration failed. Please try again.';
      return { success: false, message };
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
