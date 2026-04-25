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
    <div className="min-h-screen bg-navy-950 flex items-center justify-center px-4 py-8">
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
          <p className="text-slate-500 text-sm mt-1">Start planning smarter trips</p>
        </div>

        <div className="card p-8">
          <h2 className="text-xl font-semibold text-slate-100 mb-6">Create your account</h2>

          <ErrorAlert message={apiError} onDismiss={() => setApiError('')} />

          <form onSubmit={handleSubmit} className="space-y-4 mt-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="label">First Name</label>
                <input
                  type="text" placeholder="Arjun" value={form.firstName}
                  onChange={(e) => set('firstName', e.target.value)}
                  className={`input ${errors.firstName ? 'input-error' : ''}`}
                />
                {errors.firstName && <p className="field-error">{errors.firstName}</p>}
              </div>
              <div>
                <label className="label">Last Name</label>
                <input
                  type="text" placeholder="Sharma" value={form.lastName}
                  onChange={(e) => set('lastName', e.target.value)}
                  className={`input ${errors.lastName ? 'input-error' : ''}`}
                />
                {errors.lastName && <p className="field-error">{errors.lastName}</p>}
              </div>
            </div>

            <div>
              <label className="label">Email</label>
              <input
                type="email" placeholder="you@example.com" value={form.email}
                onChange={(e) => set('email', e.target.value)}
                className={`input ${errors.email ? 'input-error' : ''}`}
              />
              {errors.email && <p className="field-error">{errors.email}</p>}
            </div>

            <div>
              <label className="label">Password</label>
              <input
                type="password" placeholder="Min. 8 characters" value={form.password}
                onChange={(e) => set('password', e.target.value)}
                className={`input ${errors.password ? 'input-error' : ''}`}
              />
              {errors.password && <p className="field-error">{errors.password}</p>}
            </div>

            <div>
              <label className="label">Confirm Password</label>
              <input
                type="password" placeholder="Repeat password" value={form.confirmPassword}
                onChange={(e) => set('confirmPassword', e.target.value)}
                className={`input ${errors.confirmPassword ? 'input-error' : ''}`}
              />
              {errors.confirmPassword && <p className="field-error">{errors.confirmPassword}</p>}
            </div>

            <button type="submit" disabled={loading} className="btn-primary btn-lg w-full mt-2">
              {loading ? (
                <span className="flex items-center gap-2">
                  <span className="w-4 h-4 border-2 border-navy-900/40 border-t-navy-900
                                   rounded-full animate-spin" />
                  Creating account…
                </span>
              ) : 'Create Account'}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500 mt-6">
            Already have an account?{' '}
            <Link to="/login" className="text-brand-400 hover:text-brand-300 font-medium">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
