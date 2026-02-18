# Testing Strategy Documentation

## Testing Overview

The Weather Data Integration Platform implements a comprehensive testing strategy that ensures code quality, functionality, performance, and security across all components. This document outlines the testing approach, tools, frameworks, and best practices used throughout the development lifecycle.

## Testing Pyramid

The testing strategy follows the testing pyramid approach with three main levels:

```
                    [E2E Tests]
                   /            \
                  /              \
                 /                \
                /  Integration     \
               /      Tests         \
              /                      \
             /                        \
            /                          \
           /                            \
          /                              \
         /                                \
        /                                  \
       /                                    \
      /                                      \
     /                                        \
    /                                          \
   /                                            \
  /                                              \
 [Unit Tests]  [Unit Tests]  [Unit Tests]  [Unit Tests]
```

### Unit Tests (Base of Pyramid)
- **Purpose**: Test individual components in isolation
- **Coverage**: 80% of test pyramid
- **Speed**: Fast execution (< 1 second per test)
- **Tools**: JUnit 5, Mockito, Jest, React Testing Library
- **Focus**: Business logic, data models, utility functions

### Integration Tests (Middle Layer)
- **Purpose**: Test component interactions and external dependencies
- **Coverage**: 15% of test pyramid
- **Speed**: Medium execution (1-10 seconds per test)
- **Tools**: Spring Boot Test, TestContainers, Supertest
- **Focus**: API endpoints, database operations, external services

### End-to-End Tests (Top Layer)
- **Purpose**: Test complete user workflows and system integration
- **Coverage**: 5% of test pyramid
- **Speed**: Slow execution (10+ seconds per test)
- **Tools**: Playwright, Cypress, Selenium
- **Focus**: User scenarios, cross-browser compatibility

## Frontend Testing Strategy

### Unit Testing (React Components)

#### Component Testing with React Testing Library
```typescript
// components/weather-card.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import WeatherCard from './weather-card';
import { WeatherData, TrackedLocation } from '@/types/weather';

const mockLocation: TrackedLocation = {
  id: 'loc_123',
  name: 'New York',
  country: 'United States',
  latitude: 40.7128,
  longitude: -74.006,
  favorite: false,
  lastSyncAt: null,
  syncStatus: 'SYNCED'
};

const mockWeather: WeatherData = {
  current: {
    temperature_2m: 22.5,
    relative_humidity_2m: 65,
    apparent_temperature: 24.0,
    precipitation: 0.0,
    weather_code: 1,
    wind_speed_10m: 15.2
  },
  daily: {
    time: ['2024-01-15'],
    temperature_2m_max: [26.5],
    temperature_2m_min: [18.2],
    weather_code: [0]
  },
  hourly: {
    time: ['2024-01-15T11:00:00Z'],
    temperature_2m: [23.1],
    weather_code: [1]
  },
  timezone: 'America/New_York'
};

describe('WeatherCard', () => {
  const mockProps = {
    location: mockLocation,
    weather: mockWeather,
    units: 'metric' as const,
    onSync: jest.fn(),
    onDelete: jest.fn(),
    onToggleFavorite: jest.fn(),
    onSelect: jest.fn()
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders location information correctly', () => {
    render(<WeatherCard {...mockProps} />);
    
    expect(screen.getByText('New York')).toBeInTheDocument();
    expect(screen.getByText('United States')).toBeInTheDocument();
  });

  it('displays weather data when available', () => {
    render(<WeatherCard {...mockProps} />);
    
    expect(screen.getByText('22.5°C')).toBeInTheDocument();
    expect(screen.getByText('Mainly clear')).toBeInTheDocument();
  });

  it('calls onSync when sync button is clicked', () => {
    render(<WeatherCard {...mockProps} />);
    
    const syncButton = screen.getByLabelText('Sync weather data');
    fireEvent.click(syncButton);
    
    expect(mockProps.onSync).toHaveBeenCalledWith('loc_123');
  });

  it('toggles favorite status when favorite button is clicked', () => {
    render(<WeatherCard {...mockProps} />);
    
    const favoriteButton = screen.getByLabelText('Add to favorites');
    fireEvent.click(favoriteButton);
    
    expect(mockProps.onToggleFavorite).toHaveBeenCalledWith('loc_123', true);
  });

  it('shows loading state when weather is null', () => {
    render(<WeatherCard {...mockProps} weather={null} />);
    
    expect(screen.getByText('No weather data yet')).toBeInTheDocument();
    expect(screen.getByText('Fetch Weather')).toBeInTheDocument();
  });
});
```

#### Hook Testing
```typescript
// hooks/use-weather.test.ts
import { renderHook, waitFor } from '@testing-library/react';
import { useWeather } from './use-weather';

jest.mock('@/lib/api', () => ({
  fetchWeatherData: jest.fn()
}));

const mockFetchWeatherData = require('@/lib/api').fetchWeatherData;

describe('useWeather', () => {
  const locationId = 'loc_123';

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches weather data successfully', async () => {
    const mockData = { temperature: 22.5, weatherCode: 1 };
    mockFetchWeatherData.mockResolvedValue(mockData);

    const { result } = renderHook(() => useWeather(locationId));

    await waitFor(() => {
      expect(result.current.data).toEqual(mockData);
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('handles fetch errors', async () => {
    const mockError = new Error('Network error');
    mockFetchWeatherData.mockRejectedValue(mockError);

    const { result } = renderHook(() => useWeather(locationId));

    await waitFor(() => {
      expect(result.current.error).toEqual(mockError);
    });

    expect(result.current.data).toBeNull();
    expect(result.current.isLoading).toBe(false);
  });

  it('refetches data when locationId changes', async () => {
    const mockData1 = { temperature: 22.5, weatherCode: 1 };
    const mockData2 = { temperature: 25.0, weatherCode: 0 };
    
    mockFetchWeatherData
      .mockResolvedValueOnce(mockData1)
      .mockResolvedValueOnce(mockData2);

    const { result, rerender } = renderHook(
      ({ locationId }) => useWeather(locationId),
      { initialProps: { locationId } }
    );

    await waitFor(() => {
      expect(result.current.data).toEqual(mockData1);
    });

    rerender({ locationId: 'loc_456' });

    await waitFor(() => {
      expect(result.current.data).toEqual(mockData2);
    });

    expect(mockFetchWeatherData).toHaveBeenCalledTimes(2);
  });
});
```

### Integration Testing (API Endpoints)

#### API Testing with Supertest
```typescript
// __tests__/api/weather.test.ts
import request from 'supertest';
import { app } from '@/app';
import { setupTestDatabase, cleanupTestDatabase } from './helpers/database';

describe('Weather API', () => {
  beforeAll(async () => {
    await setupTestDatabase();
  });

  afterAll(async () => {
    await cleanupTestDatabase();
  });

  describe('GET /api/weather/:locationId', () => {
    it('returns weather data for valid location', async () => {
      const response = await request(app)
        .get('/api/weather/loc_123')
        .expect(200);

      expect(response.body).toHaveProperty('success', true);
      expect(response.body.data).toHaveProperty('locationId', 'loc_123');
      expect(response.body.data).toHaveProperty('current');
      expect(response.body.data.current).toHaveProperty('temperature');
    });

    it('returns 404 for non-existent location', async () => {
      const response = await request(app)
        .get('/api/weather/non-existent')
        .expect(404);

      expect(response.body).toHaveProperty('success', false);
      expect(response.body.error).toHaveProperty('message');
    });

    it('requires authentication', async () => {
      const response = await request(app)
        .get('/api/weather/loc_123')
        .expect(401);

      expect(response.body).toHaveProperty('success', false);
    });
  });

  describe('GET /api/weather/:locationId/history', () => {
    it('returns paginated weather history', async () => {
      const response = await request(app)
        .get('/api/weather/loc_123/history?page=0&size=10')
        .set('Authorization', 'Bearer valid-token')
        .expect(200);

      expect(response.body).toHaveProperty('success', true);
      expect(response.body.data).toHaveProperty('items');
      expect(response.body.data).toHaveProperty('pagination');
      expect(response.body.data.pagination).toHaveProperty('page', 0);
      expect(response.body.data.pagination).toHaveProperty('size', 10);
    });
  });
});
```

### End-to-End Testing (User Workflows)

#### Playwright E2E Tests
```typescript
// e2e/weather-app.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Weather Application', () => {
  test.beforeEach(async ({ page }) => {
    // Setup authentication
    await page.goto('/login');
    await page.fill('[data-testid="email-input"]', 'test@example.com');
    await page.fill('[data-testid="password-input"]', 'password123');
    await page.click('[data-testid="login-button"]');
    await expect(page).toHaveURL('/');
  });

  test('user can add a new location', async ({ page }) => {
    // Navigate to add location
    await page.click('[data-testid="add-location-button"]');
    
    // Fill form
    await page.fill('[data-testid="location-name-input"]', 'London');
    await page.fill('[data-testid="country-input"]', 'United Kingdom');
    await page.fill('[data-testid="latitude-input"]', '51.5074');
    await page.fill('[data-testid="longitude-input"]', '-0.1278');
    
    // Submit form
    await page.click('[data-testid="save-location-button"]');
    
    // Verify success
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="location-card"]')).toContainText('London');
  });

  test('user can view weather data', async ({ page }) => {
    // Click on a location card
    await page.click('[data-testid="location-card"]');
    
    // Verify weather data is displayed
    await expect(page.locator('[data-testid="current-temperature"]')).toBeVisible();
    await expect(page.locator('[data-testid="weather-description"]')).toBeVisible();
    await expect(page.locator('[data-testid="humidity-value"]')).toBeVisible();
  });

  test('user can sync weather data', async ({ page }) => {
    // Navigate to location details
    await page.click('[data-testid="location-card"]');
    
    // Click sync button
    await page.click('[data-testid="sync-button"]');
    
    // Verify sync success
    await expect(page.locator('[data-testid="sync-success-message"]')).toBeVisible();
  });

  test('user can manage favorites', async ({ page }) => {
    // Click favorite button
    await page.click('[data-testid="favorite-button"]');
    
    // Verify favorite status changed
    await expect(page.locator('[data-testid="favorite-icon"]')).toHaveClass(/favorite/);
    
    // Navigate to favorites page
    await page.click('[data-testid="favorites-link"]');
    
    // Verify location appears in favorites
    await expect(page.locator('[data-testid="favorite-location"]')).toContainText('New York');
  });

  test('user can search for cities', async ({ page }) => {
    // Open search dialog
    await page.click('[data-testid="search-button"]');
    
    // Search for a city
    await page.fill('[data-testid="search-input"]', 'Paris');
    await page.click('[data-testid="search-submit"]');
    
    // Verify results
    await expect(page.locator('[data-testid="search-results"]')).toBeVisible();
    await expect(page.locator('[data-testid="city-result"]')).toContainText('Paris');
  });
});
```

## Backend Testing Strategy

### Unit Testing (Services and Controllers)

#### Service Testing with Mockito
```java
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {
    
    @Mock
    private WeatherApiClient weatherApiClient;
    
    @Mock
    private WeatherSnapshotRepository weatherSnapshotRepository;
    
    @Mock
    private LocationRepository locationRepository;
    
    @InjectMocks
    private WeatherService weatherService;
    
    @Test
    void shouldReturnLatestWeatherWhenDataExists() {
        // Given
        String locationId = "test-location";
        Location location = createTestLocation(locationId);
        WeatherSnapshot snapshot = createTestSnapshot(locationId);
        
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(weatherSnapshotRepository.findLatestByLocationId(locationId))
            .thenReturn(Optional.of(snapshot));
        
        // When
        WeatherDTO.SnapshotResponse result = weatherService.getLatestWeather(locationId);
        
        // Then
        assertNotNull(result);
        assertEquals(locationId, result.getLocationId());
        assertEquals(snapshot.getCurrent().getTemperature(), result.getCurrent().getTemperature());
        
        verify(locationRepository).findById(locationId);
        verify(weatherSnapshotRepository).findLatestByLocationId(locationId);
    }
    
    @Test
    void shouldThrowNotFoundExceptionWhenLocationNotFound() {
        // Given
        String locationId = "non-existent";
        
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(NotFoundException.class, () -> 
            weatherService.getLatestWeather(locationId));
        
        verify(locationRepository).findById(locationId);
        verify(weatherSnapshotRepository, never()).findLatestByLocationId(anyString());
    }
    
    @Test
    void shouldSyncWeatherDataSuccessfully() {
        // Given
        String locationId = "test-location";
        Location location = createTestLocation(locationId);
        WeatherResponse apiResponse = createMockApiResponse();
        
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(weatherApiClient.fetchWeatherData(anyDouble(), anyDouble(), anyString()))
            .thenReturn(Mono.just(apiResponse));
        when(weatherSnapshotRepository.save(any(WeatherSnapshot.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        WeatherDTO.SnapshotResponse result = weatherService.syncWeatherData(locationId);
        
        // Then
        assertNotNull(result);
        assertEquals(locationId, result.getLocationId());
        assertTrue(result.getSyncedAt() != null);
        
        verify(weatherApiClient).fetchWeatherData(location.getLatitude(), location.getLongitude(), "metric");
        verify(weatherSnapshotRepository).save(any(WeatherSnapshot.class));
    }
    
    @Test
    void shouldHandleExternalApiError() {
        // Given
        String locationId = "test-location";
        Location location = createTestLocation(locationId);
        
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(weatherApiClient.fetchWeatherData(anyDouble(), anyDouble(), anyString()))
            .thenReturn(Mono.error(new ExternalApiException("Open-Meteo", "Service unavailable")));
        
        // When & Then
        assertThrows(ExternalApiException.class, () -> 
            weatherService.syncWeatherData(locationId));
        
        verify(weatherApiClient).fetchWeatherData(location.getLatitude(), location.getLongitude(), "metric");
        verify(weatherSnapshotRepository, never()).save(any(WeatherSnapshot.class));
    }
    
    private Location createTestLocation(String id) {
        return Location.builder()
            .id(id)
            .name("Test Location")
            .country("Test Country")
            .countryCode("TC")
            .latitude(40.7128)
            .longitude(-74.006)
            .userId("user_123")
            .build();
    }
    
    private WeatherSnapshot createTestSnapshot(String locationId) {
        return WeatherSnapshot.builder()
            .locationId(locationId)
            .current(WeatherSnapshot.CurrentWeather.builder()
                .temperature(22.5)
                .apparentTemperature(24.0)
                .humidity(65)
                .precipitation(0.0)
                .weatherCode(1)
                .weatherDescription("Mainly clear")
                .windSpeed(15.2)
                .build())
            .units("metric")
            .timezone("UTC")
            .fetchedAt(new Date())
            .build();
    }
    
    private WeatherResponse createMockApiResponse() {
        return WeatherResponse.builder()
            .current(CurrentWeatherResponse.builder()
                .temperature_2m(22.5)
                .relative_humidity_2m(65)
                .apparent_temperature(24.0)
                .precipitation(0.0)
                .weather_code(1)
                .wind_speed_10m(15.2)
                .build())
            .hourly(HourlyForecastResponse.builder()
                .time(List.of("2024-01-15T11:00:00Z"))
                .temperature_2m(List.of(23.1))
                .weather_code(List.of(1))
                .build())
            .daily(DailyForecastResponse.builder()
                .time(List.of("2024-01-15"))
                .temperature_2m_max(List.of(26.5))
                .temperature_2m_min(List.of(18.2))
                .weather_code(List.of(0))
                .build())
            .timezone("UTC")
            .build();
    }
}
```

#### Controller Testing with MockMvc
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WeatherControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private WeatherService weatherService;
    
    @Test
    void shouldReturnWeatherDataWhenLocationExists() throws Exception {
        // Given
        String locationId = "test-location";
        WeatherDTO.SnapshotResponse response = createTestResponse();
        
        when(weatherService.getLatestWeather(locationId)).thenReturn(response);
        
        // When & Then
        mockMvc.perform(get("/api/weather/{locationId}", locationId)
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.locationId").value(locationId))
            .andExpect(jsonPath("$.data.current.temperature").value(22.5))
            .andExpect(jsonPath("$.data.current.weatherDescription").value("Mainly clear"));
        
        verify(weatherService).getLatestWeather(locationId);
    }
    
    @Test
    void shouldReturn404WhenLocationNotFound() throws Exception {
        // Given
        String locationId = "non-existent";
        
        when(weatherService.getLatestWeather(locationId))
            .thenThrow(new NotFoundException("Location not found"));
        
        // When & Then
        mockMvc.perform(get("/api/weather/{locationId}", locationId)
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("Location not found"));
        
        verify(weatherService).getLatestWeather(locationId);
    }
    
    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/weather/test-location"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    void shouldReturnWeatherHistoryWithPagination() throws Exception {
        // Given
        String locationId = "test-location";
        List<WeatherDTO.SnapshotResponse> history = createTestHistory();
        
        when(weatherService.getWeatherHistory(locationId, 0, 10)).thenReturn(history);
        
        // When & Then
        mockMvc.perform(get("/api/weather/{locationId}/history", locationId)
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].locationId").value(locationId));
        
        verify(weatherService).getWeatherHistory(locationId, 0, 10);
    }
    
    private WeatherDTO.SnapshotResponse createTestResponse() {
        return WeatherDTO.SnapshotResponse.builder()
            .locationId("test-location")
            .current(WeatherDTO.CurrentWeatherResponse.builder()
                .temperature(22.5)
                .apparentTemperature(24.0)
                .humidity(65)
                .precipitation(0.0)
                .weatherCode(1)
                .weatherDescription("Mainly clear")
                .windSpeed(15.2)
                .build())
            .units("metric")
            .timezone("UTC")
            .fetchedAt(new Date())
            .build();
    }
    
    private List<WeatherDTO.SnapshotResponse> createTestHistory() {
        WeatherDTO.SnapshotResponse snapshot1 = createTestResponse();
        WeatherDTO.SnapshotResponse snapshot2 = snapshot1.toBuilder()
            .fetchedAt(Date.from(Instant.now().minus(Duration.ofHours(1))))
            .current(snapshot1.getCurrent().toBuilder()
                .temperature(21.0)
                .build())
            .build();
        
        return Arrays.asList(snapshot1, snapshot2);
    }
}
```

### Integration Testing (Database and External Services)

#### TestContainers for Database Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.yml")
@ActiveProfiles("test")
@Testcontainers
class DatabaseIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
        .withExposedPorts(27017);
    
    @Autowired
    private LocationRepository locationRepository;
    
    @Autowired
    private WeatherSnapshotRepository weatherSnapshotRepository;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @BeforeEach
    void setUp() {
        // Clean database before each test
        locationRepository.deleteAll();
        weatherSnapshotRepository.deleteAll();
    }
    
    @Test
    void shouldSaveAndRetrieveLocation() {
        // Given
        Location location = Location.builder()
            .name("Test Location")
            .country("Test Country")
            .countryCode("TC")
            .latitude(40.7128)
            .longitude(-74.006)
            .userId("user_123")
            .build();
        
        // When
        Location saved = locationRepository.save(location);
        
        // Then
        assertNotNull(saved.getId());
        assertEquals("Test Location", saved.getName());
        
        Optional<Location> retrieved = locationRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getId(), retrieved.get().getId());
    }
    
    @Test
    void shouldFindLocationsByUserId() {
        // Given
        Location location1 = createLocation("user_123", "Location 1");
        Location location2 = createLocation("user_123", "Location 2");
        Location location3 = createLocation("user_456", "Location 3");
        
        locationRepository.saveAll(Arrays.asList(location1, location2, location3));
        
        // When
        List<Location> userLocations = locationRepository.findByUserIdOrderByFavoriteDescNameAsc("user_123");
        
        // Then
        assertEquals(2, userLocations.size());
        assertEquals("Location 1", userLocations.get(0).getName());
        assertEquals("Location 2", userLocations.get(1).getName());
    }
    
    @Test
    void shouldHandleWeatherSnapshotLifecycle() {
        // Given
        String locationId = "test-location";
        WeatherSnapshot snapshot = WeatherSnapshot.builder()
            .locationId(locationId)
            .current(WeatherSnapshot.CurrentWeather.builder()
                .temperature(22.5)
                .weatherCode(1)
                .build())
            .units("metric")
            .timezone("UTC")
            .fetchedAt(new Date())
            .build();
        
        // When
        WeatherSnapshot saved = weatherSnapshotRepository.save(snapshot);
        
        // Then
        assertNotNull(saved.getId());
        assertEquals(locationId, saved.getLocationId());
        
        // Test retrieval
        List<WeatherSnapshot> snapshots = weatherSnapshotRepository.findByLocationIdOrderByFetchedAtDesc(locationId);
        assertEquals(1, snapshots.size());
        assertEquals(saved.getId(), snapshots.get(0).getId());
    }
    
    private Location createLocation(String userId, String name) {
        return Location.builder()
            .name(name)
            .country("Test Country")
            .countryCode("TC")
            .latitude(40.7128)
            .longitude(-74.006)
            .userId(userId)
            .build();
    }
}
```

#### External Service Mocking
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class ExternalServiceIntegrationTest {
    
    @Autowired
    private WeatherService weatherService;
    
    @Test
    void shouldHandleSuccessfulExternalApiCall() {
        // Given
        stubFor(get(urlPathEqualTo("/v1/forecast"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "current": {
                            "temperature_2m": 22.5,
                            "relative_humidity_2m": 65,
                            "apparent_temperature": 24.0,
                            "precipitation": 0.0,
                            "weather_code": 1,
                            "wind_speed_10m": 15.2
                        },
                        "hourly": {
                            "time": ["2024-01-15T11:00:00Z"],
                            "temperature_2m": [23.1],
                            "weather_code": [1]
                        },
                        "daily": {
                            "time": ["2024-01-15"],
                            "temperature_2m_max": [26.5],
                            "temperature_2m_min": [18.2],
                            "weather_code": [0]
                        },
                        "timezone": "UTC"
                    }
                    """)));
        
        // When
        WeatherDTO.SnapshotResponse result = weatherService.syncWeatherData("test-location");
        
        // Then
        assertNotNull(result);
        assertEquals(22.5, result.getCurrent().getTemperature());
        assertEquals(1, result.getCurrent().getWeatherCode());
    }
    
    @Test
    void shouldHandleExternalApiError() {
        // Given
        stubFor(get(urlPathEqualTo("/v1/forecast"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "Internal Server Error",
                        "message": "Service temporarily unavailable"
                    }
                    """)));
        
        // When & Then
        assertThrows(ExternalApiException.class, () -> 
            weatherService.syncWeatherData("test-location"));
    }
    
    @Test
    void shouldHandleRateLimiting() {
        // Given
        stubFor(get(urlPathEqualTo("/v1/forecast"))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "Too Many Requests",
                        "message": "Rate limit exceeded"
                    }
                    """)));
        
        // When & Then
        assertThrows(RateLimitException.class, () -> 
            weatherService.syncWeatherData("test-location"));
    }
}
```

### Performance Testing

#### Load Testing with Gatling
```scala
// src/test/scala/WeatherApiSimulation.scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class WeatherApiSimulation extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .authorizationHeader("Bearer valid-jwt-token")
  
  val scn = scenario("Weather API Load Test")
    .exec(
      http("Get Locations")
        .get("/api/locations")
        .check(status.is(200))
        .check(jsonPath("$.data.items").count.greaterThan(0))
    )
    .pause(1)
    .exec(
      http("Get Weather Data")
        .get("/api/weather/loc_123")
        .check(status.is(200))
        .check(jsonPath("$.data.current.temperature").exists)
    )
    .pause(1)
    .exec(
      http("Sync Weather Data")
        .post("/api/sync/weather?locationId=loc_123")
        .check(status.is(200))
        .check(jsonPath("$.data.success").is("true"))
    )
  
  setUp(
    scn.inject(
      rampUsers(10) during (10 seconds),
      constantUsers(50) during (60 seconds),
      rampUsers(100) during (30 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(1000), // 95% of requests should be under 1s
     global.responseTime.percentile4.lt(2000), // 99% of requests should be under 2s
     global.successfulRequests.percent.gt(99)   // 99% of requests should succeed
   )
}
```

#### Stress Testing
```scala
class WeatherApiStressTest extends Simulation {
  
  val stressScenario = scenario("Stress Test")
    .exec(
      http("High Load Weather Requests")
        .get("/api/weather/loc_123")
        .check(status.in(200 to 210)) // Allow for rate limiting
    )
  
  setUp(
    stressScenario.inject(
      rampUsers(1000) during (60 seconds),
      constantUsers(1000) during (120 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile4.lt(5000), // 99% under 5s even under stress
     global.successfulRequests.percent.gt(95)   // 95% success rate under stress
   )
}
```

## Testing Best Practices

### Test Data Management

#### Test Data Factories
```java
@Component
public class TestDataFactory {
    
    public Location createLocation(String userId) {
        return Location.builder()
            .name("Test Location " + UUID.randomUUID().toString().substring(0, 8))
            .country("Test Country")
            .countryCode("TC")
            .latitude(40.0 + Math.random() * 20)
            .longitude(-100.0 + Math.random() * 40)
            .userId(userId)
            .favorite(false)
            .syncStatus(Location.SyncStatus.SYNCED)
            .build();
    }
    
    public WeatherSnapshot createWeatherSnapshot(String locationId) {
        return WeatherSnapshot.builder()
            .locationId(locationId)
            .current(WeatherSnapshot.CurrentWeather.builder()
                .temperature(15.0 + Math.random() * 20)
                .apparentTemperature(16.0 + Math.random() * 20)
                .humidity(40 + (int)(Math.random() * 40))
                .precipitation(Math.random() * 10)
                .weatherCode((int)(Math.random() * 10))
                .weatherDescription("Test weather")
                .windSpeed(5.0 + Math.random() * 20)
                .build())
            .units("metric")
            .timezone("UTC")
            .fetchedAt(new Date())
            .build();
    }
    
    public UserPreference createUserPreference(String userId) {
        return UserPreference.builder()
            .userId(userId)
            .units("metric")
            .refreshIntervalMinutes(30)
            .windSpeedUnit("kmh")
            .precipitationUnit("mm")
            .theme("light")
            .build();
    }
}
```

#### Test Database Cleanup
```java
@Component
public class TestDatabaseCleanup {
    
    @Autowired
    private LocationRepository locationRepository;
    
    @Autowired
    private WeatherSnapshotRepository weatherSnapshotRepository;
    
    @Autowired
    private UserPreferenceRepository userPreferenceRepository;
    
    @BeforeEach
    public void cleanDatabase() {
        weatherSnapshotRepository.deleteAll();
        locationRepository.deleteAll();
        userPreferenceRepository.deleteAll();
    }
    
    @AfterEach
    public void verifyDatabaseClean() {
        assertEquals(0, locationRepository.count());
        assertEquals(0, weatherSnapshotRepository.count());
        assertEquals(0, userPreferenceRepository.count());
    }
}
```

### Mocking Strategies

#### External Service Mocking
```java
@ExtendWith(MockitoExtension.class)
class ExternalServiceMockingTest {
    
    @Mock
    private WeatherApiClient weatherApiClient;
    
    @Mock
    private WebClient webClient;
    
    @Test
    void shouldMockExternalApiCall() {
        // Given
        String expectedResponse = """
            {
                "current": {
                    "temperature_2m": 22.5,
                    "weather_code": 1
                }
            }
            """;
        
        when(weatherApiClient.fetchWeatherData(anyDouble(), anyDouble(), anyString()))
            .thenReturn(Mono.just(parseJson(expectedResponse, WeatherResponse.class)));
        
        // When
        Mono<WeatherResponse> result = weatherApiClient.fetchWeatherData(40.7128, -74.006, "metric");
        
        // Then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(22.5, response.getCurrent().getTemperature_2m());
                assertEquals(1, response.getCurrent().getWeather_code());
            })
            .verifyComplete();
    }
    
    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
```

### Test Configuration

#### Test Profiles
```yaml
# application-test.yml
spring:
  profiles:
    active: test
  data:
    mongodb:
      uri: mongodb://localhost:27017/weather_test
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: UTC

weather:
  api:
    base-url: http://localhost:8080/mock-api
  sync:
    default-interval-minutes: 1  # Fast sync for testing
    stale-threshold-minutes: 2

logging:
  level:
    com.weatherapp: DEBUG
    org.springframework.data.mongodb: INFO
    org.testcontainers: WARN
    org.mongodb.driver: WARN

test:
  containers:
    mongodb:
      enabled: true
      image: mongo:7.0
```

#### Test Utilities
```java
@Component
public class TestUtilities {
    
    public String createValidJwtToken(String userId, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 3600000); // 1 hour
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("roles", roles)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS256, "test-secret-key")
            .compact();
    }
    
    public void waitForAsyncOperations() {
        try {
            Thread.sleep(1000); // Wait for async operations
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void assertThrowsWithMessage(Class<? extends Exception> expectedType, 
                                       String expectedMessage, 
                                       Executable executable) {
        Exception exception = assertThrows(expectedType, executable);
        assertEquals(expectedMessage, exception.getMessage());
    }
}
```

## Continuous Testing

### Test Automation Pipeline

#### GitHub Actions Workflow
```yaml
# .github/workflows/test.yml
name: Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run linting
        run: npm run lint
      
      - name: Run type checking
        run: npm run typecheck
      
      - name: Run unit tests
        run: npm run test:unit
      
      - name: Run integration tests
        run: npm run test:integration
      
      - name: Run E2E tests
        run: npm run test:e2e
      
      - name: Upload coverage reports
        uses: codecov/codecov-action@v3
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: ./coverage/lcov.info

  test-backend:
    runs-on: ubuntu-latest
    services:
      mongodb:
        image: mongo:7.0
        ports:
          - 27017:27017
        options: --health-cmd "mongo --eval 'db.adminCommand(\"ismaster\")'" --health-interval 10s --health-timeout 5s --health-retries 5
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      
      - name: Run unit tests
        run: mvn test
      
      - name: Run integration tests
        run: mvn verify -P integration-test
      
      - name: Generate test report
        run: mvn surefire-report:report
      
      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: failure()
        with:
          name: test-results
          path: target/surefire-reports/

  performance-test:
    runs-on: ubuntu-latest
    needs: [test-frontend, test-backend]
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run performance tests
        run: mvn gatling:test -P performance-test
      
      - name: Upload performance results
        uses: actions/upload-artifact@v3
        with:
          name: performance-results
          path: target/gatling/results/
```

### Test Coverage Requirements

#### Coverage Thresholds
```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### Frontend Coverage
```json
// package.json
{
  "scripts": {
    "test:coverage": "jest --coverage --coverageThreshold='{\"global\":{\"branches\":70,\"functions\":70,\"lines\":80,\"statements\":80}}'"
  }
}
```

This comprehensive testing strategy ensures that the Weather Data Integration Platform maintains high code quality, reliability, and performance throughout the development lifecycle while catching issues early and providing confidence in deployments.