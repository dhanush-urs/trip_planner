import React from 'react';

export default function LoadingSpinner({ fullScreen = false, size = 'md', text = '' }) {
  const sizes = { 
    sm: 'w-5 h-5 border-2', 
    md: 'w-10 h-10 border-[3px]', 
    lg: 'w-16 h-16 border-[4px]' 
  };

  const spinner = (
    <div className="flex flex-col items-center gap-6 animate-fade-in">
      <div className="relative">
        {/* Glow effect */}
        <div className={`absolute inset-0 bg-brand-500/20 blur-xl rounded-full animate-pulse ${sizes[size]}`}></div>
        
        {/* Spinner */}
        <div
          className={`${sizes[size]} border-zinc-800 border-t-brand-500
                      rounded-full animate-spin relative z-10`}
        />
      </div>
      
      {text && (
        <div className="text-center">
          <p className="text-sm font-bold text-zinc-100 uppercase tracking-[0.2em] animate-pulse">
            {text}
          </p>
          <p className="text-[10px] font-medium text-zinc-500 uppercase tracking-widest mt-2">
            TripForge Orchestrator v1.0
          </p>
        </div>
      )}
    </div>
  );

  if (fullScreen) {
    return (
      <div className="fixed inset-0 bg-background/80 backdrop-blur-md flex items-center justify-center z-50">
        {spinner}
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center py-16">
      {spinner}
    </div>
  );
}
