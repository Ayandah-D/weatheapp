# Backend Application Overview

## Backend Architecture

The Weather Data Integration Platform backend is built using Spring Boot 3.2.3 with Java 17, providing a robust, scalable, and maintainable REST API. The backend follows modern software engineering practices including dependency injection, separation of concerns, and comprehensive error handling.

## Technology Stack

### Core Framework
- **Spring Boot 3.2.3**: Production-ready application framework
- **Java 17**: Long-term support Java version with modern features
- **Spring Web**: REST controller and HTTP handling
- **Spring Data MongoDB**: NoSQL database integration
- **Spring Validation**: Input validation and constraint checking
- **Spring Cache**: Caching abstraction for performance optimization

### HTTP Client and Async Processing
- **Spring WebFlux**: Non-blocking HTTP client for external API calls
- **WebClient**: Modern HTTP client for reactive programming
- **Reactor**: Reactive streams implementation for async operations

### Database and Persistence
- **MongoDB**: Document-oriented NoSQL database
- **Spring Data MongoDB**: Object-document mapping and repository pattern
- **MongoTemplate**: Advanced MongoDB operations and queries
- **Index Management**: Automatic index creation and optimization

### API Documentation and Testing
- **Springdoc OpenAPI**: OpenAPI 3 specification generation
- **Swagger UI**: Interactive API documentation interface
- **Spring Boot Test**: Comprehensive testing framework
- **Mockito**: Mocking framework for unit tests

### Development and Build Tools
- **Maven**: Build automation and dependency management
- **Lombok**: Code generation for boilerplate reduction
- **JUnit 5**: Modern testing framework
- **Embedded MongoDB**: In-memory database for testing

## Application Structure

### Package Organization

```
com.weatherapp/
├── WeatherApiApplication.java          # Main application class
├── config/                             # Configuration classes
│   ├── RateLimitFilter.java           # Rate limiting implementation
│   ├── WebConfig.java                 # Web configuration
│   └── SecurityConfig.java            # Security configuration
├── controller/                        # REST controllers
│   ├── WeatherController.java         # Weather data endpoints
│   ├── LocationController.java        # Location management endpoints
│   ├── PreferenceController.java      # User preferences endpoints
│   └── SyncController.java           # Data synchronization endpoints
├── dto/                              # Data Transfer Objects
│   ├── LocationDTO.java              # Location data transfer
│   ├── WeatherDTO.java               # Weather data transfer
│   └── PreferenceDTO.java            # Preference data transfer
├── exception/                        # Custom exceptions
│   ├── WeatherApiException.java      # Base exception class
│   ├── GlobalExceptionHandler.java   # Global error handling
│   └── ErrorResponse.java            # Error response structure
├── model/                            # Domain models
│   ├── Location.java                 # Location entity
│   ├── WeatherSnapshot.java          # Weather data entity
│   ├── UserPreference.java           # User preferences entity
│   └── WeatherCodeMapper.java        # Weather code utilities
├── repository/                       # Data access layer
│   ├── LocationRepository.java       # Location data access
│   ├── WeatherSnapshotRepository.java # Weather data access
│   └── UserPreferenceRepository.java # Preference data access
├── service/                          # Business logic layer
│   ├── WeatherService.java           # Weather business logic
│   ├── LocationService.java          # Location business logic
│   ├── PreferenceService.java        # Preference business logic
│   ├── SyncService.java              # Synchronization logic
│   ├── WeatherApiClient.java         # External API integration
│   └── WeatherService.java           # Weather data processing
└── util/                             # Utility classes
    └── WeatherCodeMapper.java        # Weather code mapping utilities
```

## Component Architecture

### Controllers (Presentation Layer)

#### WeatherController
**Purpose**: Handles weather data retrieval and city search operations

**Key Endpoints:**
```java
@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    
    @GetMapping("/{locationId}")
    public ResponseEntity<WeatherDTO.SnapshotResponse> getLatestWeather(
            @PathVariable String locationId) {
        return ResponseEntity.ok(weatherService.getLatestWeather(locationId));
    }
    
    @GetMapping("/{locationId}/history")
    public ResponseEntity<List<WeatherDTO.SnapshotResponse>> getWeatherHistory(
            @PathVariable String locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(weatherService.getWeatherHistory(locationId, page, size));
    }
    
    @GetMapping("/search/cities")
    public ResponseEntity<List<WeatherApiClient.GeocodingResult>> searchCities(
            @RequestParam String query) {
        return ResponseEntity.ok(weatherService.searchCities(query));
    }
}
```

**Features:**
- RESTful API design with proper HTTP methods and status codes
- Input validation and error handling
- Pagination support for historical data
- OpenAPI documentation integration
- Rate limiting protection

#### LocationController
**Purpose**: Manages user location tracking and management

**Key Endpoints:**
```java
@RestController
@RequestMapping("/api/locations")
public class LocationController {
    
    @PostMapping
    public ResponseEntity<LocationDTO.Response> addLocation(
            @Valid @RequestBody LocationDTO.CreateRequest request) {
        return ResponseEntity.ok(locationService.addLocation(request));
    }
    
    @GetMapping
    public ResponseEntity<List<LocationDTO.Response>> getLocations() {
        return ResponseEntity.ok(locationService.getLocations());
    }
    
    @PutMapping("/{id}/favorite")
    public ResponseEntity<LocationDTO.Response> toggleFavorite(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> request) {
        return ResponseEntity.ok(locationService.toggleFavorite(id, request.get("favorite")));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable String id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Features:**
- CRUD operations for location management
- Geocoding integration for location validation
- Favorite location management
- User-specific location filtering
- Conflict detection and resolution

#### PreferenceController
**Purpose**: Manages user preferences and application settings

**Key Endpoints:**
```java
@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {
    
    @GetMapping
    public ResponseEntity<PreferenceDTO.Response> getPreferences() {
        return ResponseEntity.ok(preferenceService.getPreferences());
    }
    
    @PutMapping
    public ResponseEntity<PreferenceDTO.Response> updatePreferences(
            @Valid @RequestBody PreferenceDTO.UpdateRequest request) {
        return ResponseEntity.ok(preferenceService.updatePreferences(request));
    }
    
    @GetMapping("/defaults")
    public ResponseEntity<PreferenceDTO.Response> getDefaultPreferences() {
        return ResponseEntity.ok(preferenceService.getDefaultPreferences());
    }
}
```

**Features:**
- User preference management
- Default preference configuration
- Validation and constraint checking
- Real-time preference updates

#### SyncController
**Purpose**: Handles weather data synchronization operations

**Key Endpoints:**
```java
@RestController
@RequestMapping("/api/sync")
public class SyncController {
    
    @PostMapping("/weather")
    public ResponseEntity<WeatherDTO.SyncResponse> syncWeather(
            @RequestParam(required = false) String locationId) {
        return ResponseEntity.ok(syncService.syncWeather(locationId));
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        return ResponseEntity.ok(syncService.getSyncStatus());
    }
    
    @PostMapping("/conflicts/resolve")
    public ResponseEntity<Void> resolveConflicts(
            @RequestBody List<String> snapshotIds) {
        syncService.resolveConflicts(snapshotIds);
        return ResponseEntity.ok().build();
    }
}
```

**Features:**
- Manual and automatic synchronization
- Conflict detection and resolution
- Synchronization status monitoring
- Bulk operation support

### Services (Business Logic Layer)

#### WeatherService
**Purpose**: Core weather data business logic and processing

**Key Responsibilities:**
```java
@Service
public class WeatherService {
    
    public WeatherDTO.SnapshotResponse getLatestWeather(String locationId) {
        // Retrieve latest weather snapshot for location
        // Handle caching and fallback logic
        // Return formatted response
    }
    
    public List<WeatherDTO.SnapshotResponse> getWeatherHistory(
            String locationId, int page, int size) {
        // Paginated historical weather data retrieval
        // Apply filters and sorting
        // Handle pagination logic
    }
    
    public List<WeatherApiClient.GeocodingResult> searchCities(String query) {
        // City search with caching
        // Input validation and sanitization
        // Error handling for external API
    }
    
    public WeatherDTO.SnapshotResponse syncWeatherData(String locationId) {
        // Weather data synchronization logic
        // Conflict detection and resolution
        // Data validation and transformation
    }
}
```

**Features:**
- Business rule enforcement
- Data transformation and validation
- Caching strategy implementation
- Error handling and logging
- Integration with external services

#### LocationService
**Purpose**: Location management and geocoding operations

**Key Responsibilities:**
```java
@Service
public class LocationService {
    
    public LocationDTO.Response addLocation(LocationDTO.CreateRequest request) {
        // Validate location data
        // Check for duplicates
        // Save to database
        // Return created location
    }
    
    public List<LocationDTO.Response> getLocations() {
        // Retrieve user's locations
        // Apply sorting and filtering
        // Include latest weather data
    }
    
    public LocationDTO.Response toggleFavorite(String id, boolean favorite) {
        // Update favorite status
        // Handle validation
        // Return updated location
    }
    
    public void deleteLocation(String id) {
        // Delete location and related data
        // Handle cascading operations
        // Validate permissions
    }
}
```

**Features:**
- Location validation and geocoding
- Duplicate detection and prevention
- User-specific data isolation
- Data integrity enforcement
- Performance optimization

#### SyncService
**Purpose**: Weather data synchronization and conflict management

**Key Responsibilities:**
```java
@Service
public class SyncService {
    
    public WeatherDTO.SyncResponse syncWeather(String locationId) {
        // Determine sync scope (single location or all)
        // Fetch data from external API
        // Detect and handle conflicts
        // Update database
        // Return sync result
    }
    
    public Map<String, Object> getSyncStatus() {
        // Collect synchronization metrics
        // Check for pending operations
        // Monitor external API status
        // Return status information
    }
    
    public void resolveConflicts(List<String> snapshotIds) {
        // Identify conflict types
        // Apply resolution strategies
        // Update affected data
        // Log resolution actions
    }
}
```

**Features:**
- Automated synchronization scheduling
- Conflict detection algorithms
- Resolution strategy implementation
- Performance monitoring
- Error recovery mechanisms

### Repository Layer (Data Access)

#### LocationRepository
**Purpose**: Data access for location entities

**Key Features:**
```java
@Repository
public interface LocationRepository extends MongoRepository<Location, String> {
    
    @Query("{'userId': ?0, 'favorite': true}")
    List<Location> findByUserIdAndFavoriteTrue(String userId);
    
    @Query("{'userId': ?0}")
    List<Location> findByUserIdOrderByFavoriteDescNameAsc(String userId);
    
    @Query("{'userId': ?0, 'name': ?1}")
    Optional<Location> findByUserIdAndName(String userId, String name);
    
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0 } }",
        "{ $group: { _id: null, count: { $sum: 1 } } }"
    })
    Optional<Map<String, Long>> countByUserId(String userId);
}
```

**Features:**
- Custom query methods with MongoDB annotations
- Aggregation pipeline support
- Index optimization
- Pagination support
- Performance monitoring

#### WeatherSnapshotRepository
**Purpose**: Data access for weather snapshot entities

**Key Features:**
```java
@Repository
public interface WeatherSnapshotRepository extends MongoRepository<WeatherSnapshot, String> {
    
    @Query("{'locationId': ?0}")
    List<WeatherSnapshot> findByLocationIdOrderByFetchedAtDesc(String locationId);
    
    @Query("{'locationId': ?0, 'fetchedAt': {$gte: ?1, $lte: ?2}}")
    List<WeatherSnapshot> findByLocationIdAndFetchedAtBetween(
            String locationId, Date start, Date end);
    
    @Query("{'conflictDetected': true}")
    List<WeatherSnapshot> findConflicts();
    
    @Aggregation(pipeline = {
        "{ $match: { locationId: ?0, fetchedAt: { $gte: ?1 } } }",
        "{ $sort: { fetchedAt: -1 } }",
        "{ $limit: 1 }"
    })
    Optional<WeatherSnapshot> findLatestByLocationId(String locationId, Date since);
}
```

**Features:**
- Time-series data queries
- Conflict detection queries
- Aggregation for analytics
- Index optimization for time-based queries
- Data retention management

### Model Layer (Domain Entities)

#### Location Entity
**Purpose**: Represents a tracked weather location

```java
@Document(collection = "locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    
    @Id
    private String id;
    
    @NotBlank
    private String name;
    
    @NotBlank
    private String country;
    
    @NotBlank
    private String countryCode;
    
    @NotNull
    @Range(min = -90, max = 90)
    private Double latitude;
    
    @NotNull
    @Range(min = -180, max = 180)
    private Double longitude;
    
    private String displayName;
    
    @Builder.Default
    private Boolean favorite = false;
    
    @NotBlank
    private String userId;
    
    private Date lastSyncAt;
    
    @Builder.Default
    private Location.SyncStatus syncStatus = Location.SyncStatus.NEVER_SYNCED;
    
    @CreatedDate
    private Date createdAt;
    
    @LastModifiedDate
    private Date updatedAt;
    
    public enum SyncStatus {
        NEVER_SYNCED, SYNCED, PENDING, FAILED, CONFLICT
    }
}
```

**Features:**
- MongoDB document mapping
- Validation constraints
- Audit fields with automatic timestamps
- Enum-based status management
- Builder pattern for object creation

#### WeatherSnapshot Entity
**Purpose**: Represents a weather data snapshot for a location

```java
@Document(collection = "weatherSnapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherSnapshot {
    
    @Id
    private String id;
    
    @NotBlank
    private String locationId;
    
    @Embedded
    private CurrentWeather current;
    
    @Embedded
    private List<HourlyForecast> hourlyForecast;
    
    @Embedded
    private List<DailyForecast> dailyForecast;
    
    @NotBlank
    private String units;
    
    @NotBlank
    private String timezone;
    
    @NotNull
    private Date fetchedAt;
    
    @Builder.Default
    private Boolean conflictDetected = false;
    
    private String conflictDescription;
    
    @CreatedDate
    private Date createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentWeather {
        private Double temperature;
        private Double apparentTemperature;
        private Double humidity;
        private Double precipitation;
        private Integer weatherCode;
        private String weatherDescription;
        private Double windSpeed;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyForecast {
        private String time;
        private Double temperature;
        private Integer weatherCode;
        private String weatherDescription;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecast {
        private String date;
        private Double temperatureMax;
        private Double temperatureMin;
        private Integer weatherCode;
        private String weatherDescription;
    }
}
```

**Features:**
- Embedded document structure for related data
- Complex object mapping
- Time-series data organization
- Conflict tracking and resolution
- Automatic timestamp management

## Configuration and Security

### Application Configuration
```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: weather-api
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/weatherdb}
      database: weatherdb

weather:
  api:
    base-url: https://api.open-meteo.com/v1
    geocoding-url: https://geocoding-api.open-meteo.com/v1
    openweathermap-base-url: https://api.openweathermap.org/data/2.5
    openweathermap-key: ${OPENWEATHERMAP_API_KEY:}
  sync:
    default-interval-minutes: 30
    stale-threshold-minutes: 60
    temperature-conflict-threshold: 10.0

cors:
  allowed-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}

rate-limit:
  requests-per-minute: 60
  requests-per-hour: 500

logging:
  level:
    com.weatherapp: DEBUG
    org.springframework.data.mongodb: INFO
```

### Security Configuration
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(List.of("*"));
                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(Arrays.asList("*"));
                config.setAllowCredentials(true);
                return config;
            }))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/weather/search/cities").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
    
    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }
}
```

### Rate Limiting Implementation
```java
@Component
public class RateLimitFilter implements Filter {
    
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = getClientIpAddress(httpRequest);
        
        RateLimitBucket bucket = buckets.computeIfAbsent(clientIp, k -> new RateLimitBucket());
        
        if (!bucket.tryConsume()) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private static class RateLimitBucket {
        private final AtomicInteger requests = new AtomicInteger(0);
        private final long windowStart = System.currentTimeMillis();
        private final int maxRequests = 60;
        private final long windowSize = 60000; // 1 minute
        
        public boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowSize) {
                requests.set(0);
            }
            return requests.incrementAndGet() <= maxRequests;
        }
    }
}
```

## Error Handling and Validation

### Global Exception Handler
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(WeatherApiException.class)
    public ResponseEntity<ErrorResponse> handleWeatherApiException(WeatherApiException e) {
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .error(ErrorResponse.FieldError.builder()
                .code(e.getClass().getSimpleName())
                .message(e.getMessage())
                .build())
            .timestamp(Instant.now())
            .build();
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (e instanceof WeatherApiException.NotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (e instanceof WeatherApiException.DuplicateException) {
            status = HttpStatus.CONFLICT;
        } else if (e instanceof WeatherApiException.RateLimitException) {
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else if (e instanceof WeatherApiException.InvalidCityException) {
            status = HttpStatus.BAD_REQUEST;
        }
        
        return ResponseEntity.status(status).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fieldError -> ErrorResponse.FieldError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build())
            .collect(Collectors.toList());
        
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .error(ErrorResponse.FieldError.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed")
                .details(fieldErrors)
                .build())
            .timestamp(Instant.now())
            .build();
        
        return ResponseEntity.badRequest().body(error);
    }
}
```

### Custom Exception Hierarchy
```java
public abstract class WeatherApiException extends RuntimeException {
    
    public WeatherApiException(String message) {
        super(message);
    }
    
    public static class NotFoundException extends WeatherApiException {
        public NotFoundException(String message) {
            super(message);
        }
    }
    
    public static class DuplicateException extends WeatherApiException {
        public DuplicateException(String message) {
            super(message);
        }
    }
    
    public static class RateLimitException extends WeatherApiException {
        public RateLimitException() {
            super("Rate limit exceeded");
        }
    }
    
    public static class InvalidCityException extends WeatherApiException {
        public InvalidCityException(String city) {
            super("Invalid city: " + city);
        }
    }
    
    public static class ExternalApiException extends WeatherApiException {
        public ExternalApiException(String service, String message) {
            super(service + " API error: " + message);
        }
    }
}
```

This backend architecture provides a solid foundation for a scalable, maintainable, and secure weather data integration platform with comprehensive error handling, validation, and monitoring capabilities.