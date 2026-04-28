import React from 'react';

export default function EmptyState({ icon = '🗺️', title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center py-24 text-center animate-scale-in">
      <div className="w-20 h-20 rounded-3xl bg-zinc-900 border border-border/40 flex items-center justify-center text-4xl shadow-2xl mb-8 group transition-all duration-500 hover:border-brand-500/30">
        <span className="group-hover:scale-110 transition-transform duration-500">{icon}</span>
      </div>
      <h3 className="text-2xl font-bold text-zinc-100 tracking-tight mb-3">{title}</h3>
      {description && (
        <p className="text-zinc-500 max-w-sm mb-10 font-medium leading-relaxed">
          {description}
        </p>
      )}
      <div className="animate-fade-in" style={{ animationDelay: '0.2s' }}>
        {action}
      </div>
    </div>
  );
}
