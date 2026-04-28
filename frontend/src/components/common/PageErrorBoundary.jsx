import React from 'react';
import { Link } from 'react-router-dom';

/**
 * PageErrorBoundary — catches any React render error in its subtree.
 * Renders a premium dark fallback card instead of a blank screen.
 *
 * Usage:
 *   <PageErrorBoundary>
 *     <TripDetailsPage />
 *   </PageErrorBoundary>
 */
export default class PageErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error('[TripForge][RenderError] Page crashed:', error);
    console.error('[TripForge][RenderError] Component stack:', info?.componentStack);
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <div className="min-h-screen bg-zinc-950 flex items-center justify-center px-4 py-20">
        <div className="w-full max-w-lg">
          <div className="card-premium p-10 rounded-3xl border border-red-500/20 bg-red-500/5 space-y-6 text-center">
            <div className="text-5xl">⚠️</div>

            <div className="space-y-2">
              <h2 className="text-xl font-bold text-zinc-100">
                Trip details failed to render
              </h2>
              <p className="text-sm text-zinc-500 leading-relaxed">
                The trip was created, but part of the page crashed while loading.
                Your trip data is safe — try refreshing or go back to My Trips.
              </p>
            </div>

            {/* Error detail (dev-friendly) */}
            {this.state.error && (
              <div className="p-4 rounded-2xl bg-zinc-950/60 border border-zinc-800 text-left">
                <p className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest mb-1">
                  Error detail
                </p>
                <p className="text-xs text-red-400 font-mono break-all">
                  {this.state.error.message || String(this.state.error)}
                </p>
              </div>
            )}

            <div className="flex flex-col sm:flex-row gap-3 justify-center pt-2">
              <button
                onClick={this.handleRetry}
                className="px-6 py-3 rounded-xl bg-brand-500 text-zinc-950 text-sm font-bold hover:bg-brand-400 transition-all"
              >
                Retry
              </button>
              <Link
                to="/trip/history"
                className="px-6 py-3 rounded-xl bg-zinc-900 border border-zinc-800 text-sm font-semibold text-zinc-300 hover:text-zinc-100 hover:border-zinc-700 transition-all text-center"
              >
                My Trips
              </Link>
              <Link
                to="/dashboard"
                className="px-6 py-3 rounded-xl bg-zinc-900 border border-zinc-800 text-sm font-semibold text-zinc-300 hover:text-zinc-100 hover:border-zinc-700 transition-all text-center"
              >
                Dashboard
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }
}
