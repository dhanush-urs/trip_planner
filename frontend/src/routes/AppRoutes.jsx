import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';
import ProtectedLayout from '../components/layout/ProtectedLayout.jsx';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';

import LoginPage      from '../pages/LoginPage.jsx';
import RegisterPage   from '../pages/RegisterPage.jsx';
import DashboardPage  from '../pages/DashboardPage.jsx';
import CreateTripPage from '../pages/CreateTripPage.jsx';
import TripResultPage from '../pages/TripResultPage.jsx';
import TripHistoryPage from '../pages/TripHistoryPage.jsx';
import TripDetailsPage from '../pages/TripDetailsPage.jsx';

function RequireAuth({ children }) {
  const { isAuthenticated, initializing } = useAuth();
  if (initializing) return <LoadingSpinner fullScreen />;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return children;
}

function RedirectIfAuth({ children }) {
  const { isAuthenticated, initializing } = useAuth();
  if (initializing) return <LoadingSpinner fullScreen />;
  if (isAuthenticated) return <Navigate to="/dashboard" replace />;
  return children;
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/login"    element={<RedirectIfAuth><LoginPage /></RedirectIfAuth>} />
      <Route path="/register" element={<RedirectIfAuth><RegisterPage /></RedirectIfAuth>} />

      {/* Protected */}
      <Route element={<RequireAuth><ProtectedLayout /></RequireAuth>}>
        <Route path="/dashboard"        element={<DashboardPage />} />
        <Route path="/trip/create"      element={<CreateTripPage />} />
        <Route path="/trip/result"      element={<TripResultPage />} />
        <Route path="/trip/result/:id"  element={<TripResultPage />} />
        <Route path="/trip/history"     element={<TripHistoryPage />} />
        <Route path="/trip/:id"         element={<TripDetailsPage />} />
      </Route>

      {/* Fallback */}
      <Route path="/"   element={<Navigate to="/dashboard" replace />} />
      <Route path="*"   element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
