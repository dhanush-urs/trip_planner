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
    <div className="min-h-screen bg-navy-950 flex items-center justify-center px-4">
      <div className="w-full max-w-md animate-slide-up">

        {/* Brand */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl
                          bg-brand-500/20 border border-brand-500/40 mb-4">
            <span className="text-brand-400 text-2xl">✦</span>
          </div>
          <h1 className="text-3xl font-bold text-slate-100">
            Trip<span className="text-brand-400">Forge</span>
          </h1>
          <p className="text-slate-500 text-sm mt-1">AI-Powered Smart Trip Planner</p>
        </div>

        <div className="card p-8">
          <h2 className="text-xl font-semibold text-slate-100 mb-6">Welcome back</h2>

          <ErrorAlert message={error} onDismiss={() => setError('')} />

          <form onSubmit={handleSubmit} className="space-y-4 mt-4">
            <div>
              <label className="label">Email</label>
              <input
                type="email" autoComplete="email" placeholder="you@example.com"
                value={form.email} onChange={(e) => set('email', e.target.value)}
                className="input"
              />
            </div>
            <div>
              <label className="label">Password</label>
              <input
                type="password" autoComplete="current-password" placeholder="••••••••"
                value={form.password} onChange={(e) => set('password', e.target.value)}
                className="input"
              />
            </div>

            <button type="submit" disabled={loading} className="btn-primary btn-lg w-full mt-2">
              {loading ? (
                <span className="flex items-center gap-2">
                  <span className="w-4 h-4 border-2 border-navy-900/40 border-t-navy-900
                                   rounded-full animate-spin" />
                  Signing in…
                </span>
              ) : 'Sign In'}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500 mt-6">
            Don't have an account?{' '}
            <Link to="/register" className="text-brand-400 hover:text-brand-300 font-medium">
              Create one
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
