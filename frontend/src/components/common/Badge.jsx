import React from 'react';

export default function Badge({ children, variant = 'cyan' }) {
  const variants = {
    cyan:   'badge-cyan',
    green:  'badge-green',
    yellow: 'badge-yellow',
    red:    'badge-red',
    gray:   'badge-gray',
  };
  return <span className={variants[variant] || 'badge-gray'}>{children}</span>;
}
