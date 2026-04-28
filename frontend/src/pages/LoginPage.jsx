import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';
import ErrorAlert from '../components/common/ErrorAlert.jsx';

export default function LoginPage() {
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const [form, setForm]   = useState({ email: '', password: '' });
  const [error, setError] = useState('');

  const set = (k, v) => { setForm((f) => ({ ...f, [k]: v })); setError(''); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.email || !form.password) { setError('Email and password are required.'); return; }
    const result = await login(form.email, form.password);
    if (result.success) navigate('/dashboard');
    else setError(result.message);
  };

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center px-4 relative overflow-hidden">
      {/* Decorative background elements */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-brand-500/5 blur-[120px] rounded-full"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-accent-500/5 blur-[120px] rounded-full"></div>

      <div className="w-full max-w-md z-10 animate-slide-up">
        {/* Brand */}
        <div className="text-center mb-10">
          <Link to="/" className="inline-flex items-center justify-center w-16 h-16 rounded-2xl
                          bg-zinc-900 border border-border/50 shadow-2xl mb-6 group transition-all duration-500 hover:border-brand-500/40">
            <span className="text-brand-400 text-3xl group-hover:scale-110 transition-transform duration-500">✦</span>
          </Link>
          <h1 className="text-4xl font-bold tracking-tight text-zinc-100">
            Welcome <span className="text-brand-400">Back</span>
          </h1>
          <p className="text-zinc-500 text-sm mt-3 font-medium tracking-wide">
            Sign in to continue your journey with <span className="text-zinc-300">TripForge</span>
          </p>
        </div>

        <div className="card-premium p-8 sm:p-10 relative group">
          {/* Subtle glow effect on hover */}
          <div className="absolute inset-0 bg-gradient-to-b from-brand-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"></div>
          
          <div className="relative">
            <ErrorAlert message={error} onDismiss={() => setError('')} />

            <form onSubmit={handleSubmit} className="space-y-6 mt-2">
              <div className="space-y-2">
                <label className="input-group-label">Email Address</label>
                <div className="relative">
                  <span className="absolute left-4 top-3.5 text-zinc-500">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.206" /></svg>
                  </span>
                  <input
                    type="email" autoComplete="email" placeholder="name@company.com"
                    value={form.email} onChange={(e) => set('email', e.target.value)}
                    className="input-premium pl-11"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <label className="input-group-label">Password</label>
                  <a href="#" className="text-[11px] font-semibold text-brand-400 hover:text-brand-300 uppercase tracking-wider transition-colors">Forgot?</a>
                </div>
                <div className="relative">
                  <span className="absolute left-4 top-3.5 text-zinc-500">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                  </span>
                  <input
                    type="password" autoComplete="current-password" placeholder="••••••••"
                    value={form.password} onChange={(e) => set('password', e.target.value)}
                    className="input-premium pl-11"
                  />
                </div>
              </div>

              <button 
                type="submit" 
                disabled={loading} 
                className="btn-primary w-full py-4 text-sm font-bold uppercase tracking-widest mt-4 group"
              >
                {loading ? (
                  <span className="flex items-center gap-3">
                    <svg className="animate-spin h-4 w-4 text-zinc-950" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Processing...
                  </span>
                ) : (
                  <span className="flex items-center gap-2">
                    Sign In
                    <svg className="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
                  </span>
                )}
              </button>
            </form>

            <div className="mt-8 pt-8 border-t border-border/50 text-center">
              <p className="text-sm text-zinc-500">
                New to TripForge?{' '}
                <Link to="/register" className="text-zinc-200 hover:text-brand-400 font-semibold transition-colors">
                  Create an account
                </Link>
              </p>
            </div>
          </div>
        </div>
        
        {/* Footer info */}
        <p className="text-center text-[11px] text-zinc-600 mt-8 uppercase tracking-[0.2em] font-medium">
          Secure AI Orchestration &bull; v1.0.4
        </p>
      </div>
    </div>
  );
}
