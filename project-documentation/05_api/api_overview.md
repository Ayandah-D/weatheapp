# API Documentation Overview

## API Architecture

The Weather Data Integration Platform provides a comprehensive RESTful API for weather data management, location tracking, and user preferences. The API follows REST principles with proper HTTP methods, status codes, and JSON data exchange format.

## API Base URL

```
Development: http://localhost:8080/api
Production: https://api.weatherapp.com/api
```

## API Versioning

The API uses URL-based versioning to ensure backward compatibility:

```
/api/v1/ - Current stable version
/api/v2/ - Future version (planned)
```

## Authentication

### JWT Bearer Token Authentication

The API uses JWT (JSON Web Tokens) for authentication. All protected endpoints require a valid JWT token in the Authorization header.

**Authentication Flow:**
1. User logs in with credentials
2. Server returns JWT token
3. Client includes token in subsequent requests
4. Server validates token for each request

**Request Header:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Structure:**
```json
{
  "sub": "user_12345",
  "iat": 1640995200,
  "exp": 1641081600,
  "roles": ["USER"],
  "iss": "weather-api"
}
```

## Rate Limiting

The API implements rate limiting to prevent abuse and ensure fair usage:

- **Per User**: 60 requests per minute
- **Per IP**: 500 requests per hour
- **Global**: 1000 requests per minute

**Rate Limit Headers:**
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 58
X-RateLimit-Reset: 1640995260
```

## Error Handling

### Standard Error Response Format

All API errors follow a consistent format:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "email",
        "message": "Email format is invalid"
      }
    ]
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### HTTP Status Codes

| Status Code | Description | Usage |
|-------------|-------------|-------|
| 200 | OK | Successful GET, PUT, PATCH, DELETE |
| 201 | Created | Successful POST that creates a resource |
| 204 | No Content | Successful request with no response body |
| 400 | Bad Request | Invalid request syntax or parameters |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Valid authentication but insufficient permissions |
| 404 | Not Found | Resource does not exist |
| 405 | Method Not Allowed | HTTP method not supported for endpoint |
| 409 | Conflict | Resource conflict (e.g., duplicate email) |
| 422 | Unprocessable Entity | Validation errors |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |

## Response Format

### Success Response Format

```json
{
  "success": true,
  "data": {
    // Response data
  },
  "message": "Operation successful",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Pagination Format

For endpoints that return lists, pagination is supported:

```json
{
  "success": true,
  "data": {
    "items": [...],
    "pagination": {
      "page": 1,
      "size": 10,
      "total": 100,
      "totalPages": 10,
      "hasNext": true,
      "hasPrev": false
    }
  },
  "message": "Locations retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## API Endpoints

### User Management

#### Authentication Endpoints

**POST /api/auth/login**
- **Description**: User login
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
- **Response**:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": "user_12345",
      "email": "user@example.com",
      "name": "John Doe"
    }
  },
  "message": "Login successful",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**POST /api/auth/register**
- **Description**: User registration
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe"
}
```
- **Response**: 201 Created with user data

**POST /api/auth/logout**
- **Description**: User logout
- **Headers**: Authorization: Bearer token
- **Response**: 200 OK

#### User Profile Endpoints

**GET /api/users/profile**
- **Description**: Get user profile
- **Headers**: Authorization: Bearer token
- **Response**:
```json
{
  "success": true,
  "data": {
    "id": "user_12345",
    "email": "user@example.com",
    "name": "John Doe",
    "createdAt": "2024-01-01T12:00:00Z"
  },
  "message": "Profile retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**PUT /api/users/profile**
- **Description**: Update user profile
- **Headers**: Authorization: Bearer token
- **Request Body**:
```json
{
  "name": "John Smith",
  "email": "john.smith@example.com"
}
```

**DELETE /api/users/account**
- **Description**: Delete user account
- **Headers**: Authorization: Bearer token
- **Response**: 204 No Content

### Location Management

#### Location Endpoints

**GET /api/locations**
- **Description**: Get user's locations
- **Headers**: Authorization: Bearer token
- **Query Parameters**:
  - `page`: Page number (default: 0)
  - `size`: Page size (default: 10)
  - `favorite`: Filter by favorite status (optional)
- **Response**:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "loc_123",
        "name": "New York",
        "country": "United States",
        "countryCode": "US",
        "latitude": 40.7128,
        "longitude": -74.006,
        "favorite": true,
        "lastSyncAt": "2024-01-01T12:00:00Z",
        "syncStatus": "SYNCED",
        "weather": {
          "temperature": 22.5,
          "weatherCode": 1,
          "weatherDescription": "Mainly clear"
        }
      }
    ],
    "pagination": {
      "page": 0,
      "size": 10,
      "total": 5,
      "totalPages": 1,
      "hasNext": false,
      "hasPrev": false
    }
  },
  "message": "Locations retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**POST /api/locations**
- **Description**: Add new location
- **Headers**: Authorization: Bearer token
- **Request Body**:
```json
{
  "name": "London",
  "country": "United Kingdom",
  "countryCode": "GB",
  "latitude": 51.5074,
  "longitude": -0.1278
}
```
- **Response**: 201 Created with location data

**GET /api/locations/{id}**
- **Description**: Get location details
- **Headers**: Authorization: Bearer token
- **Response**:
```json
{
  "success": true,
  "data": {
    "id": "loc_123",
    "name": "New York",
    "country": "United States",
    "countryCode": "US",
    "latitude": 40.7128,
    "longitude": -74.006,
    "favorite": true,
    "lastSyncAt": "2024-01-01T12:00:00Z",
    "syncStatus": "SYNCED",
    "weather": {
      "temperature": 22.5,
      "weatherCode": 1,
      "weatherDescription": "Mainly clear"
    }
  },
  "message": "Location retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**PUT /api/locations/{id}**
- **Description**: Update location
- **Headers**: Authorization: Bearer token
- **Request Body**:
```json
{
  "displayName": "NYC",
  "favorite": false
}
```

**DELETE /api/locations/{id}**
- **Description**: Delete location
- **Headers**: Authorization: Bearer token
- **Response**: 204 No Content

**PUT /api/locations/{id}/favorite**
- **Description**: Toggle favorite status
- **Headers**: Authorization: Bearer token
- **Request Body**:
```json
{
  "favorite": true
}
```

**GET /api/locations/{id}/weather**
- **Description**: Get location weather
- **Headers**: Authorization: Bearer token
- **Response**:
```json
{
  "success": true,
  "data": {
    "locationId": "loc_123",
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
      }
    ],
    "dailyForecast": [
      {
        "date": "2024-01-15",
        "temperatureMax": 26.5,
        "temperatureMin": 18.2,
        "weatherCode": 0,
        "weatherDescription": "Clear sky"
      }
    ],
    "units": "metric",
    "timezone": "America/New_York",
    "fetchedAt": "2024-01-15T10:30:00Z"
  },
  "message": "Weather data retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

#### Location Search Endpoints

**GET /api/locations/search**
- **Description**: Search for locations
- **Query Parameters**:
  - `query`: Search query (required)
  - `limit`: Maximum results (default: 10)
- **Response**:
```json
{
  "success": true,
  "data": [
    {
      "name": "New York",
      "country": "United States",
      "countryCode": "US",
      "latitude": 40.7128,
      "longitude": -74.006,
      "admin1": "New York"
    }
  ],
  "message": "Locations found",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Weather Data

#### Weather Endpoints

**GET /api/weather/{locationId}**
- **Description**: Get latest weather for location
- **Headers**: Authorization: Bearer token
- **Response**: Same as location weather response above

**GET /api/weather/{locationId}/history**
- **Description**: Get weather history
- **Headers**: Authorization: Bearer token
- **Query Parameters**:
  - `page`: Page number (default: 0)
  - `size`: Page size (default: 10)
- **Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": "snap_123",
      "locationId": "loc_123",
      "current": {
        "temperature": 22.5,
        "apparentTemperature": 24.0,
        "humidity": 65,
        "precipitation": 0.0,
        "weatherCode": 1,
        "weatherDescription": "Mainly clear",
        "windSpeed": 15.2
      },
      "units": "metric",
      "timezone": "America/New_York",
      "fetchedAt": "2024-01-15T10:30:00Z",
      "conflictDetected": false
    }
  ],
  "message": "Weather history retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**GET /api/weather/search/cities**
- **Description**: Search cities via geocoding API
- **Query Parameters**:
  - `query`: City name to search (required)
- **Response**:
```json
{
  "success": true,
  "data": [
    {
      "name": "New York",
      "country": "United States",
      "countryCode": "US",
      "latitude": 40.7128,
      "longitude": -74.006,
      "admin1": "New York"
    }
  ],
  "message": "Cities found",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### User Preferences

#### Preferences Endpoints

**GET /api/preferences**
- **Description**: Get user preferences
- **Headers**: Authorization: Bearer token
- **Response**:
```json
{
  "success": true,
  "data": {
    "userId": "user_12345",
    "units": "metric",
    "refreshIntervalMinutes": 30,
    "windSpeedUnit": "kmh",
    "precipitationUnit": "mm",
    "defaultLocationId": "loc_123",
    "theme": "light"
  },
  "message": "Preferences retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**PUT /api/preferences**
- **Description**: Update user preferences
- **Headers**: Authorization: Bearer token
- **Request Body**:
```json
{
  "units": "imperial",
  "refreshIntervalMinutes": 15,
  "windSpeedUnit": "mph",
  "precipitationUnit": "inch",
  "theme": "dark"
}
```

**GET /api/preferences/defaults**
- **Description**: Get default preferences
- **Response**:
```json
{
  "success": true,
  "data": {
    "units": "metric",
    "refreshIntervalMinutes": 30,
    "windSpeedUnit": "kmh",
    "precipitationUnit": "mm",
    "theme": "light"
  },
  "message": "Default preferences retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Data Synchronization

#### Sync Endpoints

**POST /api/sync/weather**
- **Description**: Sync weather data
- **Headers**: Authorization: Bearer token
- **Query Parameters**:
  - `locationId`: Specific location to sync (optional, syncs all if not provided)
- **Response**:
```json
{
  "success": true,
  "data": {
    "locationId": "loc_123",
    "locationName": "New York",
    "success": true,
    "message": "Weather data synchronized successfully",
    "syncedAt": "2024-01-15T10:30:00Z",
    "conflictDetected": false,
    "conflictDescription": null
  },
  "message": "Weather synchronization completed",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**GET /api/sync/status**
- **Description**: Get synchronization status
- **Headers**: Authorization: Bearer token
- **Response**:
```json
{
  "success": true,
  "data": {
    "totalLocations": 5,
    "syncedLocations": 4,
    "pendingSync": 1,
    "failedSync": 0,
    "lastSync": "2024-01-15T10:30:00Z",
    "nextSync": "2024-01-15T11:00:00Z",
    "conflicts": 2
  },
  "message": "Sync status retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**POST /api/sync/conflicts/resolve**
- **Description**: Resolve data conflicts
- **Headers**: Authorization: Bearer token
- **Request Body**:
```json
{
  "snapshotIds": ["snap_123", "snap_456"]
}
```
- **Response**: 200 OK

**GET /api/sync/conflicts**
- **Description**: Get unresolved conflicts
- **Headers**: Authorization: Bearer token
- **Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": "snap_123",
      "locationId": "loc_123",
      "locationName": "New York",
      "fetchedAt": "2024-01-15T10:30:00Z",
      "conflictDescription": "Temperature difference exceeds threshold",
      "currentTemperature": 22.5,
      "newTemperature": 28.0,
      "threshold": 10.0
    }
  ],
  "message": "Conflicts retrieved successfully",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## API Usage Examples

### JavaScript/Node.js Example

```javascript
class WeatherApiClient {
  constructor(baseURL, token) {
    this.baseURL = baseURL;
    this.token = token;
  }

  async getLocations() {
    const response = await fetch(`${this.baseURL}/api/locations`, {
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return response.json();
  }

  async addLocation(locationData) {
    const response = await fetch(`${this.baseURL}/api/locations`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(locationData)
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return response.json();
  }

  async syncWeather(locationId) {
    const response = await fetch(`${this.baseURL}/api/sync/weather?locationId=${locationId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return response.json();
  }
}

// Usage
const client = new WeatherApiClient('http://localhost:8080', 'your-jwt-token');
client.getLocations().then(locations => console.log(locations));
```

### Python Example

```python
import requests
import json

class WeatherApiClient:
    def __init__(self, base_url, token):
        self.base_url = base_url
        self.token = token
        self.headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }

    def get_locations(self):
        response = requests.get(f'{self.base_url}/api/locations', headers=self.headers)
        response.raise_for_status()
        return response.json()

    def add_location(self, location_data):
        response = requests.post(
            f'{self.base_url}/api/locations',
            headers=self.headers,
            json=location_data
        )
        response.raise_for_status()
        return response.json()

    def sync_weather(self, location_id):
        response = requests.post(
            f'{self.base_url}/api/sync/weather?locationId={location_id}',
            headers=self.headers
        )
        response.raise_for_status()
        return response.json()

# Usage
client = WeatherApiClient('http://localhost:8080', 'your-jwt-token')
locations = client.get_locations()
print(locations)
```

## API Testing

### Using cURL

```bash
# Get user locations
curl -X GET "http://localhost:8080/api/locations" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json"

# Add new location
curl -X POST "http://localhost:8080/api/locations" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "London",
    "country": "United Kingdom",
    "countryCode": "GB",
    "latitude": 51.5074,
    "longitude": -0.1278
  }'

# Sync weather data
curl -X POST "http://localhost:8080/api/sync/weather?locationId=loc_123" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json"
```

### Using Postman

1. **Set up Environment Variables**:
   - `baseUrl`: `http://localhost:8080`
   - `token`: Your JWT token

2. **Create Collections**:
   - Authentication
   - Locations
   - Weather
   - Preferences
   - Sync

3. **Set up Authentication**:
   - Type: Bearer Token
   - Token: `{{token}}`

This API documentation provides comprehensive coverage of all endpoints, request/response formats, authentication, and usage examples for the Weather Data Integration Platform.