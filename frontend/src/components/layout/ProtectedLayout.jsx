import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar.jsx';

export default function ProtectedLayout() {
  return (
    <div className="min-h-screen bg-navy-950 flex flex-col">
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
      <footer className="border-t border-navy-800 py-4 text-center text-xs text-slate-600">
        TripForge — AI-Powered Smart Trip Planner © 2024
      </footer>
    </div>
  );
}
