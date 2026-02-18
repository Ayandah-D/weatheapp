# Frontend Technology Stack Documentation

## Overview

This document provides comprehensive details about the frontend technology stack used in the Weather Data Integration Platform. The stack is carefully selected to provide optimal performance, developer experience, and user experience while maintaining modern development practices.

## Core Technologies

### React 19.2.3
**Purpose**: JavaScript library for building user interfaces

**Key Features:**
- **Concurrent Features**: Automatic rendering prioritization for better performance
- **Server Components**: Experimental support for server-side rendering optimization
- **Improved Error Boundaries**: Better error handling and recovery
- **Enhanced Developer Tools**: Improved debugging and profiling capabilities

**Usage in Project:**
```typescript
// Functional components with hooks
import { useState, useEffect, useCallback } from 'react';

export default function WeatherCard({ location, weather }) {
  const [isExpanded, setIsExpanded] = useState(false);
  
  useEffect(() => {
    // Component lifecycle management
  }, [location.id]);
  
  const handleSync = useCallback(() => {
    // Optimized callback function
  }, [location.id]);
  
  return (
    <div className="weather-card">
      {/* Component JSX */}
    </div>
  );
}
```

**Benefits:**
- Component reusability and modularity
- Virtual DOM for efficient rendering
- Rich ecosystem of libraries and tools
- Strong TypeScript support

### Next.js 16.1.6
**Purpose**: React framework for production applications

**Key Features:**
- **Server-Side Rendering (SSR)**: Improved SEO and performance
- **Static Site Generation (SSG)**: Pre-rendered pages for faster loading
- **Incremental Static Regeneration (ISR)**: Update static content without full rebuild
- **API Routes**: Built-in API endpoint creation
- **Image Optimization**: Automatic image optimization and lazy loading
- **File-based Routing**: Simple routing system based on file structure

**Project Configuration:**
```javascript
// next.config.mjs
/** @type {import('next').NextConfig} */
const nextConfig = {
  experimental: {
    appDir: true,
  },
  images: {
    domains: ['api.open-meteo.com'],
  },
  typescript: {
    ignoreBuildErrors: false,
  },
  eslint: {
    ignoreDuringBuilds: false,
  },
};

export default nextConfig;
```

**Routing Structure:**
```
app/
├── layout.tsx          # Root layout
├── page.tsx           # Main dashboard
├── api/               # API routes
│   └── weather/       # Weather API endpoints
└── dashboard/         # Dashboard route
    ├── page.tsx       # Dashboard page
    └── layout.tsx     # Dashboard layout
```

**Performance Benefits:**
- Automatic code splitting
- Built-in performance optimizations
- Edge runtime support for global performance
- Bundle size optimization

## State Management

### SWR 2.2.5
**Purpose**: React Hooks library for data fetching with caching

**Key Features:**
- **Caching**: Automatic request deduplication and caching
- **Revalidation**: Background data updates and stale-while-revalidate
- **Optimistic Updates**: Immediate UI updates with rollback on error
- **Pagination**: Built-in support for paginated data
- **Suspense**: React 18 Suspense integration

**Usage Examples:**

```typescript
// Basic data fetching
import useSWR from 'swr';

function WeatherDashboard() {
  const { data, error, isLoading } = useSWR(
    '/api/weather/locations',
    fetcher
  );

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;
  
  return <WeatherGrid locations={data} />;
}

// With custom configuration
function LocationWeather({ locationId }) {
  const { data, mutate } = useSWR(
    `/api/weather/${locationId}`,
    fetcher,
    {
      refreshInterval: 300000, // 5 minutes
      revalidateOnFocus: true,
      dedupingInterval: 2000,
      errorRetryCount: 3,
    }
  );

  const handleSync = async () => {
    await mutate(); // Re-fetch data
  };

  return (
    <div>
      <WeatherCard weather={data} onSync={handleSync} />
    </div>
  );
}

// Mutations with optimistic updates
function FavoriteButton({ locationId, isFavorite }) {
  const { mutate } = useSWRConfig();

  const toggleFavorite = async () => {
    // Optimistic update
    mutate(
      `/api/locations/${locationId}`,
      { ...location, favorite: !isFavorite },
      false
    );

    try {
      await fetch(`/api/locations/${locationId}/favorite`, {
        method: 'PATCH',
        body: JSON.stringify({ favorite: !isFavorite }),
      });
      
      // Revalidate
      mutate(`/api/locations/${locationId}`);
    } catch (error) {
      // Rollback
      mutate(`/api/locations/${locationId}`, undefined, true);
    }
  };

  return (
    <button onClick={toggleFavorite}>
      {isFavorite ? '★' : '☆'}
    </button>
  );
}
```

**Configuration:**
```typescript
// lib/api.ts
export const fetcher = (url: string) => 
  fetch(url).then(res => {
    if (!res.ok) {
      throw new Error('Network response was not ok');
    }
    return res.json();
  });

// Custom SWR configuration
export const swrConfig = {
  revalidateOnFocus: true,
  revalidateOnReconnect: true,
  refreshInterval: 300000, // 5 minutes
  errorRetryCount: 3,
  errorRetryInterval: 5000,
  loadingTimeout: 10000,
};
```

### React Context for Global State
**Purpose**: Global state management for application-wide data

**Theme Context Example:**
```typescript
// contexts/ThemeContext.tsx
import { createContext, useContext, useEffect, useState } from 'react';

type Theme = 'light' | 'dark' | 'system';

interface ThemeContextType {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  resolvedTheme: 'light' | 'dark';
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = useState<Theme>('system');
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>('light');

  useEffect(() => {
    const savedTheme = localStorage.getItem('theme') as Theme || 'system';
    setTheme(savedTheme);
  }, []);

  useEffect(() => {
    const root = window.document.documentElement;
    
    // Remove existing theme classes
    root.classList.remove('light', 'dark');
    
    if (theme === 'system') {
      const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light';
      root.classList.add(systemTheme);
      setResolvedTheme(systemTheme);
    } else {
      root.classList.add(theme);
      setResolvedTheme(theme);
    }
    
    localStorage.setItem('theme', theme);
  }, [theme]);

  const value = {
    theme,
    setTheme,
    resolvedTheme,
  };

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
```

## Styling and UI

### Tailwind CSS 3.4.17
**Purpose**: Utility-first CSS framework

**Configuration:**
```javascript
// tailwind.config.ts
import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: ['class'],
  content: [
    './app/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './node_modules/@tremor/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    transparent: 'transparent',
    current: 'currentColor',
    extend: {
      fontFamily: {
        sans: ['var(--font-inter)'],
        mono: ['var(--font-jetbrains)'],
      },
      colors: {
        // Tremor
        tremor: {
          brand: {
            faint: '#eff6ff',
            muted: '#bfdbfe',
            subtle: '#60a5fa',
            DEFAULT: '#3b82f6',
            emphasis: '#1d4ed8',
            inverted: '#ffffff',
          },
          background: {
            faint: '#ffffff',
            subtle: '#f8fafc',
            DEFAULT: '#ffffff',
            emphasis: '#1e293b',
          },
          border: {
            DEFAULT: '#e5e7eb',
          },
          ring: {
            DEFAULT: '#e5e7eb',
          },
          content: {
            subtle: '#9ca3af',
            DEFAULT: '#6b7280',
            emphasis: '#374151',
            strong: '#111827',
            inverted: '#ffffff',
          },
        },
        // Custom colors
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
      },
      boxShadow: {
        'tremor-input': '0 1px 2px 0 rgb(0 0 0 / 0.05)',
        'tremor-card': '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
        'tremor-dropdown': '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
      },
      borderRadius: {
        'tremor-small': '0.375rem',
        'tremor-default': '0.5rem',
        'tremor-small': '0.75rem',
        'tremor-full': '9999px',
      },
      fontSize: {
        'tremor-label': ['0.75rem', { lineHeight: '1rem' }],
        'tremor-default': ['0.875rem', { lineHeight: '1.25rem' }],
        'tremor-title': ['1.125rem', { lineHeight: '1.75rem' }],
        'tremor-metric': ['1.875rem', { lineHeight: '2.25rem' }],
      },
    },
  },
  safelist: [
    {
      pattern:
        /^(bg-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-(?:50|100|200|300|400|500|600|700|800|900|950))$/,
      variants: ['dark'],
    },
    {
      pattern:
        /^(text-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-(?:50|100|200|300|400|500|600|700|800|900|950))$/,
      variants: ['dark'],
    },
    {
      pattern:
        /^(border-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-(?:50|100|200|300|400|500|600|700|800|900|950))$/,
      variants: ['dark'],
    },
    {
      pattern:
        /^(ring-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-(?:50|100|200|300|400|500|600|700|800|900|950))$/,
    },
    {
      pattern:
        /^(stroke-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-(?:50|100|200|300|400|500|600|700|800|900|950))$/,
    },
    {
      pattern:
        /^(fill-(?:slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-(?:50|100|200|300|400|500|600|700|800|900|950))$/,
    },
  ],
  plugins: [require('@tailwindcss/forms'), require('@tailwindcss/typography'), require('tailwindcss-animate')],
} satisfies Config;

export default config;
```

**Usage Examples:**
```tsx
// Component with Tailwind classes
export default function WeatherCard({ location, weather }) {
  return (
    <div className="bg-card border border-border rounded-lg p-6 hover:border-primary/40 transition-colors">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-lg font-semibold text-foreground">{location.name}</h3>
          <p className="text-sm text-muted-foreground">{location.country}</p>
        </div>
        <div className="flex gap-2">
          <button className="p-2 rounded-md hover:bg-muted transition-colors">
            <Heart className="w-4 h-4" />
          </button>
          <button className="p-2 rounded-md hover:bg-muted transition-colors">
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>
      
      <div className="flex items-center gap-4">
        <div className="text-3xl font-bold">{weather.temperature}°C</div>
        <div className="text-sm text-muted-foreground">
          {weather.description}
        </div>
      </div>
    </div>
  );
}
```

### Radix UI
**Purpose**: Accessible, unstyled component primitives

**Key Components Used:**
- **Dialog**: Modal dialogs with proper accessibility
- **Dropdown Menu**: Accessible dropdown menus
- **Select**: Enhanced select components
- **Toggle**: Toggle buttons and switches
- **Checkbox**: Accessible checkboxes
- **Radio Group**: Accessible radio button groups

**Usage Example:**
```tsx
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';

export function AddLocationDialog({ isOpen, onClose, onAdd }) {
  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Add New Location</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <LocationSearch onLocationSelect={onAdd} />
          <div className="flex justify-end space-x-2">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button onClick={onAdd}>
              Add Location
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
```

### Lucide React 0.544.0
**Purpose**: Modern icon library with consistent design

**Usage:**
```tsx
import { Sun, Cloud, CloudRain, Snowflake, Thermometer, Droplets, Wind } from 'lucide-react';

function WeatherIcon({ code }: { code: number }) {
  switch (code) {
    case 0: return <Sun className="text-yellow-500" />;
    case 1: case 2: case 3: return <Cloud className="text-gray-500" />;
    case 51: case 53: case 55: return <CloudDrizzle className="text-blue-500" />;
    case 61: case 63: case 65: return <CloudRain className="text-blue-600" />;
    case 71: case 73: case 75: return <Snowflake className="text-blue-300" />;
    default: return <Cloud className="text-gray-400" />;
  }
}
```

## Form Handling and Validation

### React Hook Form 7.54.1
**Purpose**: Performant, flexible forms with easy validation

**Key Features:**
- **Uncontrolled Components**: Better performance with less re-renders
- **Validation**: Built-in and custom validation support
- **TypeScript**: Excellent TypeScript support
- **Integration**: Easy integration with UI libraries

**Usage Example:**
```tsx
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const locationSchema = z.object({
  name: z.string().min(2, 'City name must be at least 2 characters'),
  country: z.string().min(2, 'Country name must be at least 2 characters'),
  latitude: z.number().min(-90).max(90, 'Invalid latitude'),
  longitude: z.number().min(-180).max(180, 'Invalid longitude'),
});

type LocationFormData = z.infer<typeof locationSchema>;

export function LocationForm({ onSubmit, defaultValues }: { onSubmit: (data: LocationFormData) => void; defaultValues?: Partial<LocationFormData> }) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<LocationFormData>({
    resolver: zodResolver(locationSchema),
    defaultValues: defaultValues || {
      name: '',
      country: '',
      latitude: 0,
      longitude: 0,
    },
  });

  const onSubmitForm = async (data: LocationFormData) => {
    await onSubmit(data);
    reset();
  };

  return (
    <form onSubmit={handleSubmit(onSubmitForm)} className="space-y-4">
      <div>
        <label htmlFor="name" className="block text-sm font-medium text-foreground">
          City Name
        </label>
        <input
          {...register('name')}
          className="mt-1 block w-full rounded-md border-border bg-input text-foreground"
          placeholder="Enter city name"
        />
        {errors.name && (
          <p className="mt-1 text-sm text-destructive">{errors.name.message}</p>
        )}
      </div>

      <div>
        <label htmlFor="country" className="block text-sm font-medium text-foreground">
          Country
        </label>
        <input
          {...register('country')}
          className="mt-1 block w-full rounded-md border-border bg-input text-foreground"
          placeholder="Enter country name"
        />
        {errors.country && (
          <p className="mt-1 text-sm text-destructive">{errors.country.message}</p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="latitude" className="block text-sm font-medium text-foreground">
            Latitude
          </label>
          <input
            type="number"
            step="0.0001"
            {...register('latitude', { valueAsNumber: true })}
            className="mt-1 block w-full rounded-md border-border bg-input text-foreground"
            placeholder="e.g., 40.7128"
          />
          {errors.latitude && (
            <p className="mt-1 text-sm text-destructive">{errors.latitude.message}</p>
          )}
        </div>

        <div>
          <label htmlFor="longitude" className="block text-sm font-medium text-foreground">
            Longitude
          </label>
          <input
            type="number"
            step="0.0001"
            {...register('longitude', { valueAsNumber: true })}
            className="mt-1 block w-full rounded-md border-border bg-input text-foreground"
            placeholder="e.g., -74.0060"
          />
          {errors.longitude && (
            <p className="mt-1 text-sm text-destructive">{errors.longitude.message}</p>
          )}
        </div>
      </div>

      <div className="flex justify-end space-x-2">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Saving...' : 'Save Location'}
        </Button>
      </div>
    </form>
  );
}
```

### Zod 3.24.1
**Purpose**: TypeScript-first schema validation library

**Schema Examples:**
```typescript
import { z } from 'zod';

// User preferences schema
export const userPreferencesSchema = z.object({
  units: z.enum(['metric', 'imperial']),
  refreshIntervalMinutes: z.number().min(5).max(60),
  windSpeedUnit: z.enum(['kmh', 'mph']),
  precipitationUnit: z.enum(['mm', 'inch']),
  theme: z.enum(['light', 'dark', 'system']),
  defaultLocationId: z.string().optional(),
});

// Weather data schema
export const weatherDataSchema = z.object({
  current: z.object({
    temperature: z.number(),
    apparentTemperature: z.number(),
    humidity: z.number().min(0).max(100),
    precipitation: z.number().min(0),
    weatherCode: z.number().min(0).max(99),
    weatherDescription: z.string(),
    windSpeed: z.number().min(0),
  }),
  hourlyForecast: z.array(z.object({
    time: z.string(),
    temperature: z.number(),
    weatherCode: z.number(),
    weatherDescription: z.string(),
  })),
  dailyForecast: z.array(z.object({
    date: z.string(),
    temperatureMax: z.number(),
    temperatureMin: z.number(),
    weatherCode: z.number(),
    weatherDescription: z.string(),
  })),
  units: z.enum(['metric', 'imperial']),
  timezone: z.string(),
  fetchedAt: z.date(),
});

// API response schema
export const apiResponseSchema = z.object({
  success: z.boolean(),
  data: z.any(),
  message: z.string().optional(),
  timestamp: z.string(),
});
```

## Data Visualization

### Recharts 2.15.0
**Purpose**: Composable charting library built on React and D3

**Key Components:**
- **LineChart**: Line charts for time series data
- **BarChart**: Bar charts for categorical data
- **AreaChart**: Area charts for cumulative data
- **PieChart**: Pie charts for proportional data
- **ResponsiveContainer**: Responsive chart containers

**Usage Example:**
```tsx
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
} from 'recharts';

interface HourlyForecastProps {
  data: Array<{
    time: string;
    temperature: number;
    weatherCode: number;
  }>;
}

export function HourlyTemperatureChart({ data }: HourlyForecastProps) {
  // Format time for display
  const formattedData = data.map(item => ({
    time: new Date(item.time).getHours() + ':00',
    temperature: item.temperature,
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={formattedData}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="time" />
        <YAxis />
        <Tooltip />
        <Legend />
        <Line
          type="monotone"
          dataKey="temperature"
          stroke="#3b82f6"
          strokeWidth={2}
          dot={{ fill: '#3b82f6' }}
          activeDot={{ r: 8 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

interface DailyForecastProps {
  data: Array<{
    date: string;
    temperatureMax: number;
    temperatureMin: number;
    weatherCode: number;
  }>;
}

export function DailyForecastChart({ data }: DailyForecastProps) {
  const chartData = data.map(item => ({
    name: new Date(item.date).toLocaleDateString('en-US', { weekday: 'short' }),
    max: item.temperatureMax,
    min: item.temperatureMin,
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" />
        <YAxis />
        <Tooltip />
        <Legend />
        <Bar dataKey="max" fill="#3b82f6" name="Max Temp" />
        <Bar dataKey="min" fill="#93c5fd" name="Min Temp" />
      </BarChart>
    </ResponsiveContainer>
  );
}
```

## Development Tools

### TypeScript 5.7.3
**Purpose**: Type-safe JavaScript development

**Configuration:**
```json
// tsconfig.json
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["dom", "dom.iterable", "ES6"],
    "allowJs": true,
    "skipLibCheck": true,
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "preserve",
    "incremental": true,
    "plugins": [
      {
        "name": "next"
      }
    ],
    "baseUrl": ".",
    "paths": {
      "@/*": ["./app/*"],
      "@/components/*": ["./components/*"],
      "@/lib/*": ["./lib/*"],
      "@/hooks/*": ["./hooks/*"],
      "@/styles/*": ["./styles/*"]
    }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx", ".next/types/**/*.ts"],
  "exclude": ["node_modules"]
}
```

**Type Definitions:**
```typescript
// types/weather.ts
export interface WeatherData {
  current: {
    temperature: number;
    apparentTemperature: number;
    humidity: number;
    precipitation: number;
    weatherCode: number;
    weatherDescription: string;
    windSpeed: number;
  };
  hourlyForecast: Array<{
    time: string;
    temperature: number;
    weatherCode: number;
    weatherDescription: string;
  }>;
  dailyForecast: Array<{
    date: string;
    temperatureMax: number;
    temperatureMin: number;
    weatherCode: number;
    weatherDescription: string;
  }>;
  units: 'metric' | 'imperial';
  timezone: string;
  fetchedAt: Date;
}

export interface Location {
  id: string;
  name: string;
  country: string;
  countryCode: string;
  latitude: number;
  longitude: number;
  displayName?: string;
  favorite: boolean;
  userId: string;
  lastSyncAt?: Date;
  syncStatus: 'SYNCED' | 'PENDING' | 'FAILED' | 'CONFLICT';
  createdAt: Date;
  updatedAt: Date;
}

export interface UserPreferences {
  userId: string;
  units: 'metric' | 'imperial';
  refreshIntervalMinutes: number;
  windSpeedUnit: 'kmh' | 'mph';
  precipitationUnit: 'mm' | 'inch';
  defaultLocationId?: string;
  theme: 'light' | 'dark' | 'system';
  createdAt: Date;
  updatedAt: Date;
}
```

### ESLint Configuration
```javascript
// .eslintrc.js
module.exports = {
  extends: [
    'next/core-web-vitals',
    '@typescript-eslint/recommended',
    'prettier',
  ],
  plugins: ['@typescript-eslint', 'prettier'],
  rules: {
    'prettier/prettier': 'error',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    '@typescript-eslint/explicit-function-return-type': 'off',
    '@typescript-eslint/explicit-module-boundary-types': 'off',
    '@typescript-eslint/no-explicit-any': 'off',
  },
  overrides: [
    {
      files: ['**/*.tsx'],
      rules: {
        'react/prop-types': 'off',
      },
    },
  ],
};
```

### Prettier Configuration
```javascript
// .prettierrc.js
module.exports = {
  semi: true,
  trailingComma: 'es5',
  singleQuote: true,
  printWidth: 80,
  tabWidth: 2,
  useTabs: false,
  bracketSpacing: true,
  arrowParens: 'avoid',
  endOfLine: 'lf',
};
```

## Performance Monitoring

### Web Vitals Integration
```typescript
// lib/web-vitals.ts
import { getCLS, getFID, getFCP, getLCP, getTTFB, Metric } from 'web-vitals';

function sendToAnalytics(metric: Metric) {
  const body = JSON.stringify(metric);
  
  // Use sendBeacon if available, otherwise use fetch
  if (navigator.sendBeacon) {
    navigator.sendBeacon('/api/analytics', body);
  } else {
    fetch('/api/analytics', {
      body,
      method: 'POST',
      keepalive: true,
    });
  }
}

export function initWebVitals() {
  getCLS(sendToAnalytics);
  getFID(sendToAnalytics);
  getFCP(sendToAnalytics);
  getLCP(sendToAnalytics);
  getTTFB(sendToAnalytics);
}
```

### Bundle Analysis
```json
// package.json scripts
{
  "scripts": {
    "analyze": "next build && next export && next-bundle-analyzer",
    "analyze:server": "ANALYZE=server next build",
    "analyze:client": "ANALYZE=client next build",
    "analyze:both": "ANALYZE=both next build"
  }
}
```

This comprehensive technology stack provides a robust foundation for building a modern, performant, and maintainable weather application frontend that delivers excellent user experience across all devices and platforms.