export const CURRENCIES = [
  { value: 'INR', label: '₹ INR — Indian Rupee',    symbol: '₹'   },
  { value: 'USD', label: '$ USD — US Dollar',        symbol: '$'   },
  { value: 'EUR', label: '€ EUR — Euro',             symbol: '€'   },
  { value: 'GBP', label: '£ GBP — British Pound',   symbol: '£'   },
  { value: 'AED', label: 'AED — UAE Dirham',         symbol: 'AED' },
  { value: 'SGD', label: 'S$ SGD — Singapore Dollar',symbol: 'S$'  },
];

export const INTERESTS = [
  { value: 'nature',    label: '🌿 Nature'    },
  { value: 'nightlife', label: '🌙 Nightlife'  },
  { value: 'food',      label: '🍜 Food'       },
  { value: 'temples',   label: '🛕 Temples'    },
  { value: 'shopping',  label: '🛍️ Shopping'   },
  { value: 'adventure', label: '🧗 Adventure'  },
  { value: 'beaches',   label: '🏖️ Beaches'    },
];

export const HOTEL_PREFERENCES = [
  { value: 'BUDGET',   label: '💰 Budget',   desc: 'Affordable stays' },
  { value: 'STANDARD', label: '🏨 Standard', desc: 'Comfortable & well-rated' },
  { value: 'LUXURY',   label: '✨ Luxury',   desc: 'Premium experience' },
];

export const HOTEL_CHANGE_REASONS = [
  { value: 'CHEAPER',      label: '💸 Cheaper',       desc: 'Find a more affordable option' },
  { value: 'BETTER_RATING',label: '⭐ Better Rating',  desc: 'Higher rated hotel' },
  { value: 'CLOSER',       label: '📍 Closer',         desc: 'Nearer to city center' },
  { value: 'PREMIUM',      label: '✨ More Premium',   desc: 'Upgrade to luxury' },
];

export const TRIP_STATUS_COLORS = {
  PLANNED:   'badge-cyan',
  ACTIVE:    'badge-green',
  COMPLETED: 'badge-gray',
  CANCELLED: 'badge-red',
};
