import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth.js';

const NAV_LINKS = [
  { to: '/dashboard',    label: 'Dashboard' },
  { to: '/trip/create',  label: 'Plan Trip'  },
  { to: '/trip/history', label: 'My Trips'   },
];

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav 
      className={`sticky top-0 z-50 transition-all duration-300 ${
        scrolled 
          ? 'glass-effect border-b border-border/50 py-3' 
          : 'bg-transparent py-5'
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">

          {/* Logo */}
          <Link to="/dashboard" className="flex items-center gap-3 group">
            <div className="w-10 h-10 rounded-xl bg-brand-500/10 border border-brand-500/20
                            flex items-center justify-center group-hover:bg-brand-500/20 transition-all duration-300
                            shadow-[0_0_15px_rgba(20,184,166,0.1)] group-hover:shadow-[0_0_20px_rgba(20,184,166,0.2)]">
              <span className="text-brand-400 text-xl group-hover:scale-110 transition-transform">✦</span>
            </div>
            <span className="font-bold text-xl tracking-tight text-zinc-100">
              Trip<span className="text-brand-400">Forge</span>
            </span>
          </Link>

          {/* Desktop nav */}
          <div className="hidden md:flex items-center bg-zinc-900/40 border border-border/40 p-1 rounded-xl">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className={`px-5 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                  isActive(link.to)
                    ? 'bg-zinc-800 text-brand-400 shadow-sm border border-border/50'
                    : 'text-zinc-500 hover:text-zinc-200'
                }`}
              >
                {link.label}
              </Link>
            ))}
          </div>

          {/* User menu */}
          <div className="hidden md:flex items-center gap-6">
            <div className="flex flex-col items-end">
              <p className="text-sm font-semibold text-zinc-100">
                {user?.firstName} {user?.lastName}
              </p>
              <p className="text-[11px] font-medium text-zinc-500 uppercase tracking-wider">
                {user?.email}
              </p>
            </div>
            <div className="h-8 w-[1px] bg-border/60"></div>
            <button 
              onClick={handleLogout} 
              className="text-sm font-medium text-zinc-400 hover:text-red-400 transition-colors"
            >
              Sign out
            </button>
          </div>

          {/* Mobile hamburger */}
          <button
            className="md:hidden p-2.5 rounded-xl bg-zinc-900 border border-border text-zinc-400 hover:text-zinc-100 transition-colors"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Toggle menu"
          >
            {menuOpen ? (
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
            ) : (
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" /></svg>
            )}
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="md:hidden mt-3 mx-4 p-2 rounded-2xl bg-zinc-900 border border-border/60 shadow-2xl animate-scale-in">
          <div className="space-y-1">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                onClick={() => setMenuOpen(false)}
                className={`block px-4 py-3 rounded-xl text-sm font-medium transition-all ${
                  isActive(link.to)
                    ? 'bg-brand-500/10 text-brand-400 border border-brand-500/20'
                    : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800'
                }`}
              >
                {link.label}
              </Link>
            ))}
          </div>
          <div className="mt-2 pt-2 border-t border-border/40">
            <div className="px-4 py-3">
              <p className="text-sm font-semibold text-zinc-100">{user?.firstName} {user?.lastName}</p>
              <p className="text-xs text-zinc-500">{user?.email}</p>
            </div>
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-2 px-4 py-3 text-sm font-medium text-red-400 hover:bg-red-500/10 rounded-xl transition-all"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
              Sign out
            </button>
          </div>
        </div>
      )}
    </nav>
  );
}
