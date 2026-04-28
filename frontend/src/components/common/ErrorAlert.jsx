import React from 'react';

export default function ErrorAlert({ message, onDismiss }) {
  if (!message) return null;
  return (
    <div className="flex items-center gap-4 p-4 rounded-2xl bg-red-500/5 border border-red-500/20
                    text-red-400 animate-scale-in shadow-sm">
      <div className="w-8 h-8 rounded-full bg-red-500/10 flex items-center justify-center shrink-0">
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-bold uppercase tracking-wider mb-0.5">Operation Error</p>
        <p className="text-sm font-medium text-red-300/90 leading-tight truncate">{message}</p>
      </div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          className="p-1.5 rounded-lg hover:bg-red-500/10 text-red-400 hover:text-red-300 transition-all shrink-0"
          aria-label="Dismiss"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
        </button>
      )}
    </div>
  );
}
