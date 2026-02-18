# MongoDB Compass Connection Guide

## Overview
This guide explains how to connect MongoDB Compass to your weather app's database and verify the data is properly imported.

## Database Connection Details

### Connection String
```
mongodb://localhost:27017
```

### Database Name
```
weatherdb
```

### Collections
- `locations` - Tracked cities and locations
- `weather_snapshots` - Weather data snapshots
- `user_preferences` - User settings

## Step-by-Step Connection

### 1. Open MongoDB Compass
- Launch MongoDB Compass on your computer
- You should see the connection screen

### 2. Enter Connection Details
- **Host**: `localhost`
- **Port**: `27017`
- **Authentication**: None required (default MongoDB setup)

### 3. Connect to Database
- Click "Connect"
- In the database selector, choose `weatherdb`

### 4. Verify Collections
You should see three collections:
- `locations` - Contains your tracked cities
- `weather_snapshots` - Weather data (populated when syncing)
- `user_preferences` - User settings

## Sample Data Verification

### Locations Collection
The `locations` collection should contain sample cities:

```json
{
  "_id": ObjectId("..."),
  "name": "Victoria Falls",
  "country": "Zimbabwe",
  "latitude": -17.9243,
  "longitude": 25.8572,
  "favorite": true,
  "syncStatus": "NEVER_SYNCED",
  "createdAt": ISODate("..."),
  "updatedAt": ISODate("...")
}
```

### User Preferences Collection
Contains default user settings:

```json
{
  "_id": ObjectId("..."),
  "userId": "default",
  "units": "metric",
  "refreshIntervalMinutes": 5,
  "windSpeedUnit": "kmh",
  "precipitationUnit": "mm",
  "theme": "dark",
  "createdAt": ISODate("..."),
  "updatedAt": ISODate("...")
}
```

## Using the Weather App

### 1. Start the Backend (Java Spring Boot)
Since Maven is not installed, you'll need to either:
- Install Maven: `sudo apt install maven`
- Or use the Docker setup: `cd weather-api-backend && docker-compose up -d`

### 2. Start the Frontend
```bash
npm run dev
```

### 3. Access the Application
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080

### 4. Add Cities
- Click "Add City" button
- Search for any city (e.g., "New York", "London", "Tokyo")
- The app will fetch weather data and sync with MongoDB

### 5. Monitor Database Changes
- Open MongoDB Compass
- Navigate to the `weather_snapshots` collection
- You should see weather data being inserted as you sync cities

## API Endpoints

The backend provides these REST endpoints:

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

## Troubleshooting

### MongoDB Connection Issues
1. Ensure MongoDB service is running: `systemctl status mongod`
2. Check if port 27017 is accessible
3. Verify database name is `weatherdb`

### Backend Not Starting
1. Install Maven: `sudo apt install maven`
2. Or use Docker: `docker-compose up -d`
3. Check logs for any errors

### Frontend Not Connecting to Backend
1. Verify `.env.local` contains: `NEXT_PUBLIC_API_URL=http://localhost:8080`
2. Ensure backend is running on port 8080
3. Check browser console for CORS errors

## Data Flow

1. **Frontend** → **Backend API** → **Open-Meteo API**
2. **Backend** → **MongoDB** (stores weather snapshots)
3. **Frontend** ← **Backend** ← **MongoDB** (retrieves stored data)

## Security Notes

- The application uses rate limiting to prevent API abuse
- Weather data is cached to reduce API calls
- MongoDB runs on localhost with no authentication (for development)
- CORS is configured to allow frontend connections

## Next Steps

1. Explore the data in MongoDB Compass
2. Add more cities to track
3. Monitor how weather data changes over time
4. Experiment with different sync intervals
5. Customize user preferences

## Technical Stack

- **Frontend**: Next.js 16, React 19, TypeScript, Tailwind CSS
- **Backend**: Java 17, Spring Boot 3, Spring Data MongoDB
- **Database**: MongoDB 7.0
- **API**: Open-Meteo (weather data), Geocoding API (city search)
- **Caching**: Spring Cache with Caffeine
- **Rate Limiting**: Custom implementation with Redis-like buckets