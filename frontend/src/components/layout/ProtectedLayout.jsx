import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar.jsx';

export default function ProtectedLayout() {
  return (
    <div className="min-h-screen bg-background flex flex-col relative overflow-x-hidden">
      {/* Abstract background decorative elements for the entire app shell */}
      <div className="fixed top-[-10%] right-[-10%] w-[40%] h-[40%] bg-brand-500/5 blur-[120px] rounded-full pointer-events-none z-0"></div>
      <div className="fixed bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-accent-500/5 blur-[120px] rounded-full pointer-events-none z-0"></div>
      
      <Navbar />
      
      <main className="flex-1 relative z-10">
        <Outlet />
      </main>
      
      <footer className="relative z-10 border-t border-border/40 py-10 text-center">
        <p className="text-[10px] font-bold text-zinc-600 uppercase tracking-[0.3em]">
          TripForge Travel Intelligence Orchestrator &bull; &copy; 2026
        </p>
      </footer>
    </div>
  );
}
