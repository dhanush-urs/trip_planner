import React, { useState } from 'react';
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

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="sticky top-0 z-50 bg-navy-900/95 backdrop-blur border-b border-navy-700">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">

          {/* Logo */}
          <Link to="/dashboard" className="flex items-center gap-2.5 group">
            <div className="w-8 h-8 rounded-lg bg-brand-500/20 border border-brand-500/40
                            flex items-center justify-center group-hover:bg-brand-500/30 transition-colors">
              <span className="text-brand-400 text-lg">✦</span>
            </div>
            <span className="font-bold text-lg text-slate-100">
              Trip<span className="text-brand-400">Forge</span>
            </span>
          </Link>

          {/* Desktop nav */}
          <div className="hidden md:flex items-center gap-1">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive(link.to)
                    ? 'bg-brand-500/15 text-brand-300'
                    : 'text-slate-400 hover:text-slate-100 hover:bg-navy-700'
                }`}
              >
                {link.label}
              </Link>
            ))}
          </div>

          {/* User menu */}
          <div className="hidden md:flex items-center gap-3">
            <div className="text-right">
              <p className="text-sm font-medium text-slate-200">
                {user?.firstName} {user?.lastName}
              </p>
              <p className="text-xs text-slate-500">{user?.email}</p>
            </div>
            <button onClick={handleLogout} className="btn-secondary btn-sm">
              Sign out
            </button>
          </div>

          {/* Mobile hamburger */}
          <button
            className="md:hidden p-2 rounded-lg text-slate-400 hover:text-slate-100
                       hover:bg-navy-700 transition-colors"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Toggle menu"
          >
            {menuOpen ? '✕' : '☰'}
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="md:hidden border-t border-navy-700 bg-navy-900 px-4 py-3 space-y-1">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              onClick={() => setMenuOpen(false)}
              className={`block px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive(link.to)
                  ? 'bg-brand-500/15 text-brand-300'
                  : 'text-slate-400 hover:text-slate-100 hover:bg-navy-700'
              }`}
            >
              {link.label}
            </Link>
          ))}
          <div className="pt-2 border-t border-navy-700">
            <p className="px-4 py-1 text-xs text-slate-500">{user?.email}</p>
            <button
              onClick={handleLogout}
              className="w-full text-left px-4 py-2.5 text-sm text-danger
                         hover:bg-danger/10 rounded-lg transition-colors"
            >
              Sign out
            </button>
          </div>
        </div>
      )}
    </nav>
  );
}
