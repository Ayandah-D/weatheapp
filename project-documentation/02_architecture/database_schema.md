# Database Schema Documentation

## Overview

This document describes the MongoDB database schema for the Weather Data Integration Platform. The database is designed to store user information, location data, weather snapshots, and user preferences in a scalable and efficient manner.

## Database Structure

### Database Name: `weatherdb`

The application uses MongoDB as the primary database with the following collections:

## Collections

### 1. Locations Collection

**Purpose**: Stores user-managed weather locations with geocoding data.

**Collection Name**: `locations`

#### Schema Definition

```javascript
{
  _id: ObjectId,                    // MongoDB auto-generated ID
  name: String,                     // City name (e.g., "New York")
  country: String,                  // Country name (e.g., "United States")
  countryCode: String,              // ISO country code (e.g., "US")
  latitude: Number,                 // Geographic latitude (-90 to 90)
  longitude: Number,                // Geographic longitude (-180 to 180)
  displayName: String,              // Custom display name (optional)
  favorite: Boolean,                // Whether location is marked as favorite
  userId: String,                   // Reference to user who owns this location
  lastSyncAt: Date,                 // Last successful weather data sync timestamp
  syncStatus: String,               // Current sync status (e.g., "SYNCED", "PENDING", "FAILED")
  createdAt: Date,                  // Record creation timestamp
  updatedAt: Date                   // Last modification timestamp
}
```

#### Field Descriptions

| Field | Type | Required | Description | Constraints |
|-------|------|----------|-------------|-------------|
| `_id` | ObjectId | Yes | Primary key, auto-generated | MongoDB default |
| `name` | String | Yes | Official city name | Min length: 1, Max length: 100 |
| `country` | String | Yes | Country name | Min length: 2, Max length: 100 |
| `countryCode` | String | Yes | ISO 3166-1 alpha-2 country code | Exactly 2 characters |
| `latitude` | Number | Yes | Geographic latitude | Range: -90 to 90 |
| `longitude` | Number | Yes | Geographic longitude | Range: -180 to 180 |
| `displayName` | String | No | User-customizable display name | Max length: 50 |
| `favorite` | Boolean | Yes | Favorite status flag | Default: false |
| `userId` | String | Yes | User identifier | References user system |
| `lastSyncAt` | Date | No | Last sync completion time | Nullable |
| `syncStatus` | String | Yes | Current synchronization status | Enum: ["SYNCED", "PENDING", "FAILED", "CONFLICT"] |
| `createdAt` | Date | Yes | Record creation timestamp | Auto-generated |
| `updatedAt` | Date | Yes | Last modification timestamp | Auto-updated |

#### Indexes

```javascript
// Compound index for user-specific queries
db.locations.createIndex({ userId: 1, name: 1 }, { unique: true })

// Geospatial index for location-based queries
db.locations.createIndex({ latitude: 1, longitude: 1 })

// Favorite locations index for quick access
db.locations.createIndex({ favorite: 1, userId: 1 })

// Sync status index for monitoring
db.locations.createIndex({ syncStatus: 1, userId: 1 })

// Timestamp index for ordering
db.locations.createIndex({ createdAt: -1 })
```

#### Sample Document

```javascript
{
  "_id": ObjectId("64f123456789012345678901"),
  "name": "New York",
  "country": "United States",
  "countryCode": "US",
  "latitude": 40.7128,
  "longitude": -74.006,
  "displayName": "NYC",
  "favorite": true,
  "userId": "user_12345",
  "lastSyncAt": ISODate("2024-01-15T10:30:00Z"),
  "syncStatus": "SYNCED",
  "createdAt": ISODate("2024-01-10T08:15:00Z"),
  "updatedAt": ISODate("2024-01-15T10:30:00Z")
}
```

### 2. WeatherSnapshots Collection

**Purpose**: Stores historical weather data snapshots for each location.

**Collection Name**: `weatherSnapshots`

#### Schema Definition

```javascript
{
  _id: ObjectId,                    // MongoDB auto-generated ID
  locationId: String,               // Reference to locations collection
  current: {                        // Current weather conditions
    temperature: Number,            // Current temperature
    apparentTemperature: Number,    // Feels-like temperature
    humidity: Number,               // Relative humidity percentage
    precipitation: Number,          // Current precipitation
    weatherCode: Number,            // WMO weather code
    weatherDescription: String,     // Human-readable weather description
    windSpeed: Number               // Wind speed
  },
  hourlyForecast: [                 // Array of hourly forecasts
    {
      time: String,                 // ISO timestamp string
      temperature: Number,          // Hourly temperature
      weatherCode: Number,          // WMO weather code
      weatherDescription: String    // Human-readable description
    }
  ],
  dailyForecast: [                  // Array of daily forecasts
    {
      date: String,                 // Date string (YYYY-MM-DD format)
      temperatureMax: Number,       // Daily maximum temperature
      temperatureMin: Number,       // Daily minimum temperature
      weatherCode: Number,          // WMO weather code
      weatherDescription: String    // Human-readable description
    }
  ],
  units: String,                    // Measurement units (metric/imperial)
  timezone: String,                 // Timezone identifier
  fetchedAt: Date,                  // When this snapshot was fetched
  conflictDetected: Boolean,        // Whether data conflict was detected
  conflictDescription: String,      // Description of conflict (if any)
  createdAt: Date                   // Record creation timestamp
}
```

#### Field Descriptions

| Field | Type | Required | Description | Constraints |
|-------|------|----------|-------------|-------------|
| `_id` | ObjectId | Yes | Primary key, auto-generated | MongoDB default |
| `locationId` | String | Yes | Reference to location | References locations._id |
| `current` | Object | Yes | Current weather data | Embedded document |
| `current.temperature` | Number | Yes | Current temperature | Depends on units |
| `current.apparentTemperature` | Number | Yes | Feels-like temperature | Depends on units |
| `current.humidity` | Number | Yes | Relative humidity | Range: 0-100 |
| `current.precipitation` | Number | Yes | Current precipitation | Range: 0+ |
| `current.weatherCode` | Number | Yes | WMO weather condition code | Range: 0-99 |
| `current.weatherDescription` | String | Yes | Human-readable description | Max length: 100 |
| `current.windSpeed` | Number | Yes | Wind speed | Depends on units |
| `hourlyForecast` | Array | Yes | Hourly forecast data | Max 48 hours |
| `dailyForecast` | Array | Yes | Daily forecast data | Max 14 days |
| `units` | String | Yes | Measurement system | Enum: ["metric", "imperial"] |
| `timezone` | String | Yes | Timezone identifier | IANA timezone format |
| `fetchedAt` | Date | Yes | Data fetch timestamp | Auto-generated |
| `conflictDetected` | Boolean | Yes | Conflict flag | Default: false |
| `conflictDescription` | String | No | Conflict details | Max length: 500 |
| `createdAt` | Date | Yes | Record creation timestamp | Auto-generated |

#### Indexes

```javascript
// Compound index for location and time queries
db.weatherSnapshots.createIndex({ locationId: 1, fetchedAt: -1 })

// Conflict detection index
db.weatherSnapshots.createIndex({ conflictDetected: 1, locationId: 1 })

// Time series index for historical queries
db.weatherSnapshots.createIndex({ fetchedAt: -1 })

// Units and timezone index for filtering
db.weatherSnapshots.createIndex({ units: 1, timezone: 1 })
```

#### Sample Document

```javascript
{
  "_id": ObjectId("64f123456789012345678902"),
  "locationId": "64f123456789012345678901",
  "current": {
    "temperature": 22.5,
    "apparentTemperature": 24.0,
    "humidity": 65,
    "precipitation": 0.0,
    "weatherCode": 1,
    "weatherDescription": "Mainly clear",
    "windSpeed": 15.2
  },
  "hourlyForecast": [
    {
      "time": "2024-01-15T11:00:00Z",
      "temperature": 23.1,
      "weatherCode": 1,
      "weatherDescription": "Mainly clear"
    },
    {
      "time": "2024-01-15T12:00:00Z",
      "temperature": 24.8,
      "weatherCode": 0,
      "weatherDescription": "Clear sky"
    }
  ],
  "dailyForecast": [
    {
      "date": "2024-01-15",
      "temperatureMax": 26.5,
      "temperatureMin": 18.2,
      "weatherCode": 0,
      "weatherDescription": "Clear sky"
    },
    {
      "date": "2024-01-16",
      "temperatureMax": 24.1,
      "temperatureMin": 17.8,
      "weatherCode": 2,
      "weatherDescription": "Partly cloudy"
    }
  ],
  "units": "metric",
  "timezone": "America/New_York",
  "fetchedAt": ISODate("2024-01-15T10:30:00Z"),
  "conflictDetected": false,
  "conflictDescription": null,
  "createdAt": ISODate("2024-01-15T10:30:00Z")
}
```

### 3. UserPreferences Collection

**Purpose**: Stores user-specific application preferences and settings.

**Collection Name**: `userPreferences`

#### Schema Definition

```javascript
{
  _id: ObjectId,                    // MongoDB auto-generated ID
  userId: String,                   // Unique user identifier
  units: String,                    // Temperature units (metric/imperial)
  refreshIntervalMinutes: Number,   // Auto-refresh interval for weather data
  windSpeedUnit: String,            // Wind speed units (kmh/mph)
  precipitationUnit: String,        // Precipitation units (mm/inch)
  defaultLocationId: String,        // Default location for weather display
  theme: String,                    // UI theme (light/dark/system)
  createdAt: Date,                  // Record creation timestamp
  updatedAt: Date                   // Last modification timestamp
}
```

#### Field Descriptions

| Field | Type | Required | Description | Constraints |
|-------|------|----------|-------------|-------------|
| `_id` | ObjectId | Yes | Primary key, auto-generated | MongoDB default |
| `userId` | String | Yes | Unique user identifier | Unique constraint |
| `units` | String | Yes | Temperature measurement units | Enum: ["metric", "imperial"] |
| `refreshIntervalMinutes` | Number | Yes | Auto-refresh interval | Range: 5-60 minutes |
| `windSpeedUnit` | String | Yes | Wind speed units | Enum: ["kmh", "mph"] |
| `precipitationUnit` | String | Yes | Precipitation units | Enum: ["mm", "inch"] |
| `defaultLocationId` | String | No | Default location reference | References locations._id |
| `theme` | String | Yes | UI theme preference | Enum: ["light", "dark", "system"] |
| `createdAt` | Date | Yes | Record creation timestamp | Auto-generated |
| `updatedAt` | Date | Yes | Last modification timestamp | Auto-updated |

#### Indexes

```javascript
// Unique index on userId
db.userPreferences.createIndex({ userId: 1 }, { unique: true })

// Theme index for quick theme-based queries
db.userPreferences.createIndex({ theme: 1 })

// Units preference index
db.userPreferences.createIndex({ units: 1, windSpeedUnit: 1, precipitationUnit: 1 })
```

#### Sample Document

```javascript
{
  "_id": ObjectId("64f123456789012345678903"),
  "userId": "user_12345",
  "units": "metric",
  "refreshIntervalMinutes": 30,
  "windSpeedUnit": "kmh",
  "precipitationUnit": "mm",
  "defaultLocationId": "64f123456789012345678901",
  "theme": "light",
  "createdAt": ISODate("2024-01-10T08:15:00Z"),
  "updatedAt": ISODate("2024-01-12T14:20:00Z")
}
```

## Data Relationships

### Relationship Diagram

```
UserPreferences (1) ────┐
                        │
                        ├─→ userId references user system
                        │
                        │
Locations (N) ←───────→ userId (N)
   │
   ├─→ _id (referenced by WeatherSnapshots.locationId)
   │
   └─→ defaultLocationId (references Locations._id)
   │
WeatherSnapshots (N) ←────→ locationId
```

### Relationship Types

1. **One-to-One**: User → UserPreferences
   - Each user has exactly one preference record
   - Enforced by unique constraint on userId

2. **One-to-Many**: User → Locations
   - Each user can have multiple locations
   - Enforced by userId field in locations collection

3. **One-to-Many**: Location → WeatherSnapshots
   - Each location can have multiple weather snapshots
   - Enforced by locationId field in weatherSnapshots collection

4. **Self-Reference**: UserPreferences → Locations
   - Default location reference within user preferences
   - Optional relationship for default weather display

## Data Validation Rules

### Application-Level Validation

#### Location Validation
```javascript
// Required fields validation
- name: required, min: 1, max: 100
- country: required, min: 2, max: 100
- countryCode: required, exactly 2 characters
- latitude: required, range: -90 to 90
- longitude: required, range: -180 to 180
- userId: required, non-empty string

// Business logic validation
- Unique constraint: { userId, name } combination
- Geospatial validation: valid latitude/longitude coordinates
- Country code validation: ISO 3166-1 alpha-2 format
```

#### Weather Data Validation
```javascript
// Current weather validation
- temperature: required, numeric
- humidity: required, range: 0-100
- precipitation: required, range: 0+
- weatherCode: required, range: 0-99 (WMO codes)
- windSpeed: required, numeric

// Forecast validation
- hourlyForecast: array, max 48 items
- dailyForecast: array, max 14 items
- time/date format validation: ISO 8601 for time, YYYY-MM-DD for date
```

#### User Preferences Validation
```javascript
// Required fields validation
- userId: required, unique
- units: required, enum: ["metric", "imperial"]
- refreshIntervalMinutes: required, range: 5-60
- windSpeedUnit: required, enum: ["kmh", "mph"]
- precipitationUnit: required, enum: ["mm", "inch"]
- theme: required, enum: ["light", "dark", "system"]

// Business logic validation
- Unique constraint on userId
- Default location must exist in locations collection
```

## Data Retention Policies

### Weather Data Retention
- **Current Weather**: Real-time, no retention (fetched on demand)
- **Weather Snapshots**: Retain for 90 days
- **Hourly Forecasts**: Retain with associated snapshots
- **Daily Forecasts**: Retain with associated snapshots

### Location Data Retention
- **Active Locations**: Indefinite retention
- **Deleted Locations**: Soft delete with 30-day retention before hard delete
- **Sync History**: Retain last sync timestamp and status

### User Preferences Retention
- **Active Users**: Indefinite retention
- **Deleted Users**: Delete preferences when user account is deleted
- **Audit Trail**: Maintain creation and modification timestamps

## Performance Considerations

### Query Optimization

#### Common Query Patterns
```javascript
// User's locations with weather data
db.locations.find({ userId: "user_123" }).sort({ favorite: -1, name: 1 })

// Latest weather snapshot for a location
db.weatherSnapshots.findOne(
  { locationId: "loc_123" },
  { sort: { fetchedAt: -1 } }
)

// User preferences with default location
db.userPreferences.findOne({ userId: "user_123" })

// Weather data for time range
db.weatherSnapshots.find({
  locationId: "loc_123",
  fetchedAt: { $gte: startDate, $lte: endDate }
})
```

#### Aggregation Examples
```javascript
// User's favorite locations with latest weather
db.locations.aggregate([
  { $match: { userId: "user_123", favorite: true } },
  { $lookup: {
    from: "weatherSnapshots",
    localField: "_id",
    foreignField: "locationId",
    as: "weather"
  }},
  { $sort: { name: 1 } }
])

// Average temperature by day for a location
db.weatherSnapshots.aggregate([
  { $match: { locationId: "loc_123" } },
  { $group: {
    _id: { $dateToString: { format: "%Y-%m-%d", date: "$fetchedAt" } },
    avgTemp: { $avg: "$current.temperature" }
  }},
  { $sort: { _id: 1 } }
])
```

### Indexing Strategy

#### Performance Indexes
```javascript
// User-specific queries
db.locations.createIndex({ userId: 1, favorite: -1, name: 1 })

// Weather data queries
db.weatherSnapshots.createIndex({ locationId: 1, fetchedAt: -1 })
db.weatherSnapshots.createIndex({ fetchedAt: -1, locationId: 1 })

// Preference queries
db.userPreferences.createIndex({ userId: 1 })
db.userPreferences.createIndex({ theme: 1, units: 1 })

// Geospatial queries
db.locations.createIndex({ latitude: 1, longitude: 1 })
```

## Backup and Recovery

### Backup Strategy
- **Daily Backups**: Full database backup at 2 AM UTC
- **Incremental Backups**: Every 4 hours during peak usage
- **Point-in-Time Recovery**: 7-day recovery window
- **Backup Storage**: Encrypted cloud storage with geographic redundancy

### Recovery Procedures
1. **Database Restore**: Point-in-time recovery from latest backup
2. **Collection Restore**: Individual collection recovery if needed
3. **Data Validation**: Post-restore data integrity checks
4. **Service Verification**: Application functionality testing after restore

This database schema provides a robust foundation for the weather data integration platform, supporting efficient data storage, retrieval, and management while maintaining data integrity and performance.