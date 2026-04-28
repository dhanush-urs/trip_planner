import React from 'react';

export default function Badge({ children, variant = 'brand' }) {
  const variants = {
    brand:   'bg-brand-500/10 text-brand-400 border-brand-500/20',
    success: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    warning: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    danger:  'bg-red-500/10 text-red-400 border-red-500/20',
    muted:   'bg-zinc-800 text-zinc-400 border-zinc-700',
  };
  
  return (
    <span className={`badge-premium px-2.5 py-0.5 border ${variants[variant] || variants.muted}`}>
      {children}
    </span>
  );
}
