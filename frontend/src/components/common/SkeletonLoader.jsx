import React from 'react';

export default function SkeletonLoader({ className = '', variant = 'rect' }) {
  const base = "animate-pulse bg-zinc-800/50 border border-border/20";
  const variants = {
    rect: "rounded-xl",
    circle: "rounded-full",
    text: "rounded-md h-4 w-3/4",
    title: "rounded-md h-8 w-1/2",
  };

  return (
    <div className={`${base} ${variants[variant] || variants.rect} ${className}`} />
  );
}

export function SkeletonCard() {
  return (
    <div className="card-premium p-6 space-y-4">
      <SkeletonLoader variant="rect" className="h-12 w-12" />
      <div className="space-y-2">
        <SkeletonLoader variant="title" />
        <SkeletonLoader variant="text" />
      </div>
      <div className="pt-4 border-t border-border/20 flex justify-between">
        <SkeletonLoader variant="rect" className="h-6 w-20" />
        <SkeletonLoader variant="rect" className="h-6 w-12" />
      </div>
    </div>
  );
}

export function SkeletonItinerary() {
  return (
    <div className="card-premium p-6 space-y-6">
      <div className="flex items-center gap-4">
        <SkeletonLoader variant="rect" className="h-12 w-12" />
        <div className="flex-1 space-y-2">
          <SkeletonLoader variant="title" className="w-1/3" />
          <SkeletonLoader variant="text" className="w-1/4" />
        </div>
      </div>
      <div className="space-y-8 pl-8 border-l border-zinc-800">
        {[1, 2, 3].map(n => (
          <div key={n} className="space-y-2">
            <SkeletonLoader variant="text" className="w-20" />
            <SkeletonLoader variant="title" className="h-6 w-1/2" />
            <SkeletonLoader variant="text" className="h-10 w-full" />
          </div>
        ))}
      </div>
    </div>
  );
}
