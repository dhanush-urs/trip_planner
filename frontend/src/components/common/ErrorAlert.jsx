import React from 'react';

export default function ErrorAlert({ message, onDismiss }) {
  if (!message) return null;
  return (
    <div className="flex items-start gap-3 p-4 rounded-xl bg-danger/10 border border-danger/30
                    text-red-300 animate-fade-in">
      <span className="text-lg mt-0.5 shrink-0">⚠</span>
      <p className="text-sm flex-1">{message}</p>
      {onDismiss && (
        <button
          onClick={onDismiss}
          className="text-red-400 hover:text-red-200 transition-colors shrink-0"
          aria-label="Dismiss"
        >
          ✕
        </button>
      )}
    </div>
  );
}
