# Complete Weather App Integration Guide

## Overview
This guide explains how to fully integrate MongoDB Compass with your weather app and connect the frontend to the backend API.

## Database Setup ✅ Complete

### MongoDB Connection Details
- **Host**: `localhost`
- **Port**: `27017`
- **Database**: `weatherdb`
- **Collections**: `locations`, `weather_snapshots`, `user_preferences`

### Sample Data
The database contains sample cities:
- Victoria Falls, Zimbabwe
- Cape Town, South Africa  
- London, United Kingdom

## Frontend-Backend Integration ✅ Complete

### API Routes Created
The following Next.js API routes proxy requests to the Spring Boot backend:

```
app/api/locations/route.ts          → GET/POST /api/locations
app/api/weather/[locationId]/route.ts → GET /api/weather/{locationId}
app/api/sync/[locationId]/route.ts   → POST /api/sync/{locationId}
app/api/sync/all/route.ts           → POST /api/sync/all
app/api/preferences/route.ts        → GET/PUT /api/preferences
```

### Environment Configuration
```bash
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
MONGODB_URI=mongodb://localhost:27017/weatherdb
```

## How to Use the Complete System

### 1. Start MongoDB (if not running)
```bash
sudo systemctl start mongod
```

### 2. Start the Backend
**Option A: Using Maven (recommended)**
```bash
cd weather-api-backend
sudo apt install maven  # If not installed
mvn spring-boot:run
```

**Option B: Using Docker**
```bash
cd weather-api-backend
docker-compose up -d
```

### 3. Start the Frontend
```bash
npm run dev
```

### 4. Access Applications
- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8080

### 5. Connect MongoDB Compass
1. Open MongoDB Compass
2. Enter connection string: `mongodb://localhost:27017`
3. Select database: `weatherdb`
4. View collections and data

## Data Flow Architecture

```
Frontend (Next.js) 
    ↓ (API Routes)
Backend (Spring Boot) 
    ↓ (Open-Meteo API)
External Weather API
    ↓ (Weather Data)
Backend (Spring Boot)
    ↓ (MongoDB)
Database (MongoDB)
    ↓ (Cached Data)
Backend (Spring Boot)
    ↓ (API Routes)
Frontend (Next.js)
```

## API Endpoints Reference

### Locations Management
- `GET /api/locations` - Get all tracked locations
- `POST /api/locations` - Add new location
- `PUT /api/locations/{id}` - Update location
- `DELETE /api/locations/{id}` - Delete location

### Weather Data
- `GET /api/weather/{locationId}` - Get weather for specific location
- `POST /api/sync/{locationId}` - Sync weather data for location
- `POST /api/sync/all` - Sync all locations

### User Preferences
- `GET /api/preferences` - Get user preferences
- `PUT /api/preferences` - Update preferences

## Frontend Features

### City Management
- Add cities via search (geocoding API)
- Mark cities as favorites
- Delete cities from tracking
- View sync status for each location

### Weather Display
- Current temperature, humidity, wind speed
- Weather condition descriptions and icons
- 7-day daily forecast
- 24-hour hourly forecast
- Automatic data refresh

### Backend Integration
- Automatic fallback to direct API if backend fails
- Real-time sync status updates
- Error handling and user feedback
- Data persistence in MongoDB

## MongoDB Compass Usage

### View Collections
1. **locations** - Tracked cities with metadata
2. **weather_snapshots** - Weather data with TTL (auto-deletes after 2 days)
3. **user_preferences** - User settings

### Monitor Data Changes
- Watch weather data being inserted as you sync cities
- View sync status updates in real-time
- Monitor TTL expiration of old weather data

### Sample Queries
```javascript
// Find all locations
db.locations.find()

// Find weather data for a specific location
db.weather_snapshots.find({ locationId: "location-id" })

// Find failed syncs
db.locations.find({ syncStatus: "FAILED" })
```

## Troubleshooting

### Common Issues

**1. Backend not starting**
```bash
# Check if Maven is installed
mvn --version

# If not installed
sudo apt install maven

# Start backend
cd weather-api-backend && mvn spring-boot:run
```

**2. Frontend can't connect to backend**
- Check `.env.local` has correct `NEXT_PUBLIC_API_URL`
- Ensure backend is running on port 8080
- Check browser console for CORS errors

**3. MongoDB connection issues**
```bash
# Check MongoDB status
systemctl status mongod

# Check if port is accessible
netstat -an | grep 27017
```

**4. API errors**
- Check backend logs for detailed error messages
- Verify MongoDB has proper permissions
- Ensure Open-Meteo API is accessible

### Error Handling
The frontend includes comprehensive error handling:
- Backend API failures → Fallback to direct Open-Meteo API
- Network errors → User-friendly error messages
- Invalid cities → Clear feedback to user
- Sync failures → Retry mechanism available

## Performance Features

### Caching
- Backend caches weather data for 30 minutes
- TTL indexes automatically clean up old data
- Reduces API calls to external services

### Rate Limiting
- Backend implements rate limiting to prevent API abuse
- 100 requests per 15 minutes per IP
- Protects against excessive API usage

### Data Optimization
- MongoDB indexes for fast queries
- Efficient data structures for weather snapshots
- Automatic cleanup of stale data

## Next Steps

1. **Explore the data** in MongoDB Compass
2. **Add more cities** to track weather for
3. **Monitor sync behavior** and data persistence
4. **Experiment with different sync intervals**
5. **Customize user preferences** (units, refresh rate, etc.)

## Technical Stack

### Frontend
- Next.js 16 with React 19
- TypeScript for type safety
- Tailwind CSS for styling
- SWR for data fetching
- Next.js API routes for backend communication

### Backend
- Java 17 with Spring Boot 3
- Spring Data MongoDB for database access
- WebClient for external API calls
- Spring Cache with Caffeine for caching
- Custom rate limiting implementation

### Database
- MongoDB 7.0
- TTL indexes for automatic data cleanup
- Comprehensive validation schemas
- Optimized indexes for performance

### External APIs
- Open-Meteo for weather data
- Geocoding API for city search
- Rate-limited to prevent abuse

This complete integration provides a robust, scalable weather application with proper database management, API integration, and user-friendly frontend interface.