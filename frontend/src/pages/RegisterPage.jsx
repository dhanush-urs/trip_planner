import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';
import ErrorAlert from '../components/common/ErrorAlert.jsx';

export default function RegisterPage() {
  const { register, loading } = useAuth();
  const navigate = useNavigate();

  const [form, setForm]   = useState({
    firstName: '', lastName: '', email: '', password: '', confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState('');

  const set = (k, v) => {
    setForm((f) => ({ ...f, [k]: v }));
    setErrors((e) => ({ ...e, [k]: '' }));
    setApiError('');
  };

  const validate = () => {
    const e = {};
    if (!form.firstName.trim()) e.firstName = 'First name is required.';
    if (!form.lastName.trim())  e.lastName  = 'Last name is required.';
    if (!form.email.trim())     e.email     = 'Email is required.';
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Invalid email format.';
    if (!form.password)         e.password  = 'Password is required.';
    else if (form.password.length < 8) e.password = 'Password must be at least 8 characters.';
    if (form.password !== form.confirmPassword) e.confirmPassword = 'Passwords do not match.';
    return e;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }

    const result = await register({
      firstName: form.firstName.trim(),
      lastName:  form.lastName.trim(),
      email:     form.email.trim(),
      password:  form.password,
    });

    if (result.success) navigate('/dashboard');
    else setApiError(result.message);
  };

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center px-4 py-12 relative overflow-hidden">
      {/* Decorative background elements */}
      <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-brand-500/5 blur-[120px] rounded-full"></div>
      <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-accent-500/5 blur-[120px] rounded-full"></div>

      <div className="w-full max-w-lg z-10 animate-slide-up">
        {/* Brand */}
        <div className="text-center mb-10">
          <Link to="/" className="inline-flex items-center justify-center w-16 h-16 rounded-2xl
                          bg-zinc-900 border border-border/50 shadow-2xl mb-6 group transition-all duration-500 hover:border-brand-500/40">
            <span className="text-brand-400 text-3xl group-hover:scale-110 transition-transform duration-500">✦</span>
          </Link>
          <h1 className="text-4xl font-bold tracking-tight text-zinc-100">
            Create <span className="text-brand-400">Account</span>
          </h1>
          <p className="text-zinc-500 text-sm mt-3 font-medium tracking-wide">
            Join thousands of travelers planning with <span className="text-zinc-300">TripForge</span>
          </p>
        </div>

        <div className="card-premium p-8 sm:p-10 relative group">
          <div className="absolute inset-0 bg-gradient-to-b from-brand-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"></div>
          
          <div className="relative">
            <ErrorAlert message={apiError} onDismiss={() => setApiError('')} />

            <form onSubmit={handleSubmit} className="space-y-6 mt-2">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="input-group-label">First Name</label>
                  <input
                    type="text" placeholder="Arjun" value={form.firstName}
                    onChange={(e) => set('firstName', e.target.value)}
                    className={`input-premium ${errors.firstName ? 'border-danger/50 focus:ring-danger/20' : ''}`}
                  />
                  {errors.firstName && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.firstName}</p>}
                </div>
                <div className="space-y-2">
                  <label className="input-group-label">Last Name</label>
                  <input
                    type="text" placeholder="Sharma" value={form.lastName}
                    onChange={(e) => set('lastName', e.target.value)}
                    className={`input-premium ${errors.lastName ? 'border-danger/50 focus:ring-danger/20' : ''}`}
                  />
                  {errors.lastName && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.lastName}</p>}
                </div>
              </div>

              <div className="space-y-2">
                <label className="input-group-label">Email Address</label>
                <input
                  type="email" placeholder="name@company.com" value={form.email}
                  onChange={(e) => set('email', e.target.value)}
                  className={`input-premium ${errors.email ? 'border-danger/50 focus:ring-danger/20' : ''}`}
                />
                {errors.email && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.email}</p>}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="input-group-label">Password</label>
                  <input
                    type="password" placeholder="••••••••" value={form.password}
                    onChange={(e) => set('password', e.target.value)}
                    className={`input-premium ${errors.password ? 'border-danger/50 focus:ring-danger/20' : ''}`}
                  />
                  {errors.password && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.password}</p>}
                </div>
                <div className="space-y-2">
                  <label className="input-group-label">Confirm Password</label>
                  <input
                    type="password" placeholder="••••••••" value={form.confirmPassword}
                    onChange={(e) => set('confirmPassword', e.target.value)}
                    className={`input-premium ${errors.confirmPassword ? 'border-danger/50 focus:ring-danger/20' : ''}`}
                  />
                  {errors.confirmPassword && <p className="text-[10px] font-bold text-danger uppercase tracking-wider mt-1.5 ml-1">{errors.confirmPassword}</p>}
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
                    Creating Account...
                  </span>
                ) : (
                  <span className="flex items-center gap-2">
                    Create Account
                    <svg className="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
                  </span>
                )}
              </button>
            </form>

            <div className="mt-8 pt-8 border-t border-border/50 text-center">
              <p className="text-sm text-zinc-500">
                Already have an account?{' '}
                <Link to="/login" className="text-zinc-200 hover:text-brand-400 font-semibold transition-colors">
                  Sign in
                </Link>
              </p>
            </div>
          </div>
        </div>

        <p className="text-center text-[11px] text-zinc-600 mt-8 uppercase tracking-[0.2em] font-medium">
          By signing up, you agree to our <a href="#" className="underline decoration-zinc-700 underline-offset-4 hover:text-zinc-400 transition-colors">Terms of Service</a>
        </p>
      </div>
    </div>
  );
}
