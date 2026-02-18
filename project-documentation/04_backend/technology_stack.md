# Backend Technology Stack Documentation

## Overview

This document provides comprehensive details about the backend technology stack used in the Weather Data Integration Platform. The backend is built using Spring Boot with Java 17, providing a robust, scalable, and maintainable REST API with comprehensive features for weather data management.

## Core Technologies

### Spring Boot 3.2.3
**Purpose**: Production-ready application framework for building Java applications

**Key Features:**
- **Auto-configuration**: Automatic configuration based on classpath and dependencies
- **Starter Dependencies**: Pre-configured dependency sets for common use cases
- **Embedded Server**: Built-in Tomcat server for easy deployment
- **Actuator**: Production-ready features for monitoring and management
- **Externalized Configuration**: Environment-specific configuration management

**Configuration Example:**
```yaml
# application.yml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: weather-api
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/weatherdb}
      database: weatherdb
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: UTC

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

**Benefits:**
- Rapid application development with minimal configuration
- Production-ready features out of the box
- Extensive ecosystem of Spring projects
- Strong enterprise adoption and support

### Java 17
**Purpose**: Long-term support (LTS) Java version with modern language features

**Key Features Used:**
- **Records**: Immutable data classes for DTOs
- **Sealed Classes**: Restricted class hierarchies
- **Pattern Matching**: Enhanced instanceof and switch expressions
- **Text Blocks**: Multi-line string literals
- **Records**: Simplified data carrier classes

**Example Usage:**
```java
// Record for immutable data transfer
public record WeatherSummary(
    String locationId,
    String locationName,
    Double currentTemperature,
    String weatherDescription,
    Date lastUpdated
) {}

// Sealed classes for error handling
public sealed interface WeatherResult permits Success, Error {}
public record Success(WeatherData data) implements WeatherResult {}
public record Error(String message, ErrorCode code) implements WeatherResult {}

// Pattern matching in switch
public String formatWeatherCode(int code) {
    return switch (code) {
        case 0 -> "Clear sky";
        case 1, 2, 3 -> "Partly cloudy";
        case 45, 48 -> "Fog";
        case 51, 53, 55 -> "Drizzle";
        case 61, 63, 65 -> "Rain";
        case 71, 73, 75 -> "Snow";
        default -> "Unknown";
    };
}
```

**Benefits:**
- Modern language features for cleaner code
- Enhanced performance and security
- Long-term support and stability
- Strong type safety and immutability

## Database and Persistence

### MongoDB 7.0
**Purpose**: Document-oriented NoSQL database for flexible data storage

**Key Features:**
- **Schema Flexibility**: Dynamic schema for evolving data models
- **Horizontal Scaling**: Built-in sharding and replication
- **Geospatial Queries**: Native support for location-based queries
- **Aggregation Framework**: Powerful data processing and analytics
- **High Performance**: Optimized for read-heavy workloads

**Connection Configuration:**
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://username:password@localhost:27017/weatherdb
      database: weatherdb
      authentication-database: admin
      options:
        connect-timeout: 10000
        socket-timeout: 30000
        max-pool-size: 100
        min-pool-size: 10
```

**Index Configuration:**
```java
@Document(collection = "weatherSnapshots")
public class WeatherSnapshot {
    
    @Indexed(name = "location_fetched_idx", background = true)
    private String locationId;
    
    @Indexed(name = "fetched_at_idx", background = true)
    private Date fetchedAt;
    
    @Indexed(name = "conflict_idx", background = true)
    private Boolean conflictDetected;
    
    @GeoSpatialIndexed(name = "location_coords_idx")
    private Point coordinates;
}
```

**Benefits:**
- Flexible schema for weather data variations
- Excellent performance for time-series data
- Built-in support for geospatial queries
- Horizontal scaling capabilities

### Spring Data MongoDB
**Purpose**: Object-document mapping and repository pattern for MongoDB

**Key Features:**
- **Repository Pattern**: Standardized data access interfaces
- **Query Methods**: Method name-based query generation
- **Custom Queries**: Native MongoDB query support
- **Aggregation**: Complex data processing operations
- **Auditing**: Automatic timestamp management

**Repository Example:**
```java
@Repository
public interface WeatherSnapshotRepository extends MongoRepository<WeatherSnapshot, String> {
    
    // Method name-based query
    List<WeatherSnapshot> findByLocationIdOrderByFetchedAtDesc(String locationId);
    
    // Custom query with annotations
    @Query("{'locationId': ?0, 'fetchedAt': {$gte: ?1}}")
    List<WeatherSnapshot> findByLocationIdAndFetchedAtAfter(String locationId, Date date);
    
    // Aggregation pipeline
    @Aggregation(pipeline = {
        "{ $match: { locationId: ?0, fetchedAt: { $gte: ?1 } } }",
        "{ $sort: { fetchedAt: -1 } }",
        "{ $limit: 1 }"
    })
    Optional<WeatherSnapshot> findLatestSnapshot(String locationId, Date since);
    
    // Custom method with MongoTemplate
    default List<WeatherSnapshot> findConflicts(String locationId) {
        Query query = new Query(Criteria.where("locationId").is(locationId)
            .and("conflictDetected").is(true));
        return mongoTemplate.find(query, WeatherSnapshot.class);
    }
}
```

**Benefits:**
- Type-safe repository interfaces
- Automatic query generation
- Integration with Spring ecosystem
- Performance optimization features

## HTTP Client and External API Integration

### Spring WebFlux
**Purpose**: Non-blocking, reactive HTTP client for external API calls

**Key Features:**
- **Reactive Programming**: Non-blocking I/O operations
- **Backpressure Support**: Automatic flow control
- **Connection Pooling**: Efficient connection management
- **Error Handling**: Comprehensive error recovery
- **Caching**: Built-in response caching

**WebClient Configuration:**
```java
@Configuration
public class WebClientConfig {
    
    @Bean
    @Primary
    public WebClient weatherWebClient() {
        return WebClient.builder()
            .baseUrl("https://api.open-meteo.com/v1")
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .responseTimeout(Duration.ofSeconds(30))
                    .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(30))
                    )
            ))
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }
    
    @Bean
    public WebClient geocodingWebClient() {
        return WebClient.builder()
            .baseUrl("https://geocoding-api.open-meteo.com/v1")
            .build();
    }
}
```

**Usage Example:**
```java
@Service
public class WeatherApiClient {
    
    private final WebClient weatherClient;
    private final WebClient geocodingClient;
    
    public WeatherApiClient(WebClient weatherClient, WebClient geocodingClient) {
        this.weatherClient = weatherClient;
        this.geocodingClient = geocodingClient;
    }
    
    public Mono<WeatherResponse> fetchWeatherData(double lat, double lon, String units) {
        return weatherClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/forecast")
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m")
                .queryParam("hourly", "temperature_2m,weather_code")
                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                .queryParam("temperature_unit", units.equals("metric") ? "celsius" : "fahrenheit")
                .queryParam("wind_speed_unit", units.equals("metric") ? "kmh" : "mph")
                .queryParam("precipitation_unit", units.equals("metric") ? "mm" : "inch")
                .queryParam("timezone", "auto")
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                if (clientResponse.statusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    return Mono.error(new RateLimitException());
                }
                return clientResponse.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(new ExternalApiException("Open-Meteo", body)));
            })
            .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                Mono.error(new ExternalApiException("Open-Meteo", "Server error")))
            .bodyToMono(WeatherResponse.class);
    }
}
```

**Benefits:**
- Non-blocking I/O for better performance
- Built-in error handling and retry logic
- Connection pooling and resource management
- Reactive streams support

## Caching and Performance

### Spring Cache Abstraction
**Purpose**: Caching abstraction with multiple cache providers

**Configuration:**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes
      cache-null-values: false
      use-key-prefix: true
    cache-names:
      - weatherData
      - locationSearch
      - userPreferences
      - geocodingResults
```

**Cache Usage:**
```java
@Service
public class WeatherService {
    
    @Cacheable(value = "weatherData", key = "#locationId")
    public WeatherDTO.SnapshotResponse getLatestWeather(String locationId) {
        // Expensive weather data retrieval
        return weatherRepository.findLatestByLocationId(locationId)
            .map(this::convertToResponse)
            .orElseThrow(() -> new NotFoundException("Weather data not found"));
    }
    
    @CacheEvict(value = "weatherData", key = "#locationId")
    public WeatherDTO.SnapshotResponse updateWeatherData(String locationId, WeatherSnapshot snapshot) {
        // Update weather data and evict cache
        WeatherSnapshot saved = weatherRepository.save(snapshot);
        return convertToResponse(saved);
    }
    
    @Cacheable(value = "geocodingResults", key = "#query.toLowerCase()")
    public List<GeocodingResult> searchCities(String query) {
        // External API call for city search
        return geocodingClient.searchCities(query);
    }
}
```

**Cache Providers:**
- **Redis**: Production caching with persistence
- **Caffeine**: In-memory caching for development
- **EhCache**: Alternative in-memory caching
- **Hazelcast**: Distributed caching for clusters

**Benefits:**
- Performance improvement through caching
- Multiple cache provider support
- Annotation-based caching
- Cache invalidation strategies

## API Documentation and Testing

### Springdoc OpenAPI
**Purpose**: OpenAPI 3 specification generation and Swagger UI

**Configuration:**
```java
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Weather Data Integration Platform API")
                .version("1.0.0")
                .description("REST API for weather data management and synchronization")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@weatherapp.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(Arrays.asList(
                new Server().url("http://localhost:8080").description("Development Server"),
                new Server().url("https://api.weatherapp.com").description("Production Server")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization")));
    }
}
```

**API Documentation:**
```java
@RestController
@RequestMapping("/api/weather")
@Tag(name = "Weather", description = "Weather data retrieval and management")
public class WeatherController {
    
    @Operation(
        summary = "Get latest weather data",
        description = "Retrieve the most recent weather snapshot for a specific location",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Weather data retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Location not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    @GetMapping("/{locationId}")
    public ResponseEntity<WeatherDTO.SnapshotResponse> getLatestWeather(
            @Parameter(description = "Location ID", required = true)
            @PathVariable String locationId) {
        return ResponseEntity.ok(weatherService.getLatestWeather(locationId));
    }
}
```

**Benefits:**
- Automatic API documentation generation
- Interactive Swagger UI for testing
- OpenAPI 3 specification compliance
- Integration with API testing tools

### Testing Framework

#### JUnit 5 and Spring Boot Test
**Purpose**: Comprehensive testing framework for backend components

**Test Configuration:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.yml")
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @Autowired
    protected LocationRepository locationRepository;
    
    @Autowired
    protected WeatherSnapshotRepository weatherSnapshotRepository;
    
    @Autowired
    protected UserPreferenceRepository userPreferenceRepository;
    
    @BeforeEach
    void setUp() {
        // Test data setup
        locationRepository.deleteAll();
        weatherSnapshotRepository.deleteAll();
        userPreferenceRepository.deleteAll();
    }
}
```

**Service Testing:**
```java
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {
    
    @Mock
    private WeatherApiClient weatherApiClient;
    
    @Mock
    private WeatherSnapshotRepository weatherSnapshotRepository;
    
    @InjectMocks
    private WeatherService weatherService;
    
    @Test
    void shouldReturnLatestWeatherWhenDataExists() {
        // Given
        String locationId = "test-location";
        WeatherSnapshot snapshot = createTestSnapshot(locationId);
        
        when(weatherSnapshotRepository.findLatestByLocationId(locationId))
            .thenReturn(Optional.of(snapshot));
        
        // When
        WeatherDTO.SnapshotResponse result = weatherService.getLatestWeather(locationId);
        
        // Then
        assertNotNull(result);
        assertEquals(locationId, result.getLocationId());
        verify(weatherSnapshotRepository).findLatestByLocationId(locationId);
    }
    
    @Test
    void shouldThrowNotFoundExceptionWhenDataMissing() {
        // Given
        String locationId = "non-existent";
        
        when(weatherSnapshotRepository.findLatestByLocationId(locationId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(NotFoundException.class, () -> 
            weatherService.getLatestWeather(locationId));
    }
}
```

**Integration Testing:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WeatherControllerIntegrationTest extends AbstractIntegrationTest {
    
    @Test
    void shouldReturnWeatherDataWhenLocationExists() {
        // Given
        Location location = createTestLocation();
        locationRepository.save(location);
        
        WeatherSnapshot snapshot = createTestSnapshot(location.getId());
        weatherSnapshotRepository.save(snapshot);
        
        // When
        ResponseEntity<WeatherDTO.SnapshotResponse> response = restTemplate.getForEntity(
            "/api/weather/" + location.getId(), 
            WeatherDTO.SnapshotResponse.class
        );
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(location.getId(), response.getBody().getLocationId());
    }
    
    @Test
    void shouldReturn404WhenLocationNotFound() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/weather/non-existent", 
            String.class
        );
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
```

**Benefits:**
- Comprehensive testing coverage
- Mocking support for external dependencies
- Integration testing with real database
- Test data management and cleanup

## Build and Deployment

### Maven Build Configuration
**Purpose**: Build automation and dependency management

**POM Configuration:**
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.3</version>
        <relativePath/>
    </parent>
    
    <groupId>com.weatherapp</groupId>
    <artifactId>weather-api</artifactId>
    <version>1.0.0</version>
    <name>Weather Data Integration Platform</name>
    <description>Spring Boot backend for Weather Data Integration Platform</description>
    
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <springdoc.version>2.3.0</springdoc.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- OpenAPI Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- Embedded MongoDB for Testing -->
        <dependency>
            <groupId>de.flapdoodle.embed</groupId>
            <artifactId>de.flapdoodle.embed.mongo.spring3x</artifactId>
            <version>4.11.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            
            <!-- Code Quality Plugins -->
            <plugin>
                <groupId>org.sonarsource.scanner.maven</groupId>
                <artifactId>sonar-maven-plugin</artifactId>
                <version>3.9.1.2184</version>
            </plugin>
            
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
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

**Build Commands:**
```bash
# Compile and package
mvn clean package

# Run tests
mvn test

# Run with Spring Boot
mvn spring-boot:run

# Generate code coverage report
mvn test jacoco:report

# Run SonarQube analysis
mvn sonar:sonar
```

**Benefits:**
- Standardized build process
- Dependency management
- Code quality tools integration
- Multiple deployment options

## Monitoring and Observability

### Spring Boot Actuator
**Purpose**: Production-ready monitoring and management features

**Configuration:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,beans,configprops
  endpoint:
    health:
      show-details: always
      show-components: always
    metrics:
      enabled: true
    loggers:
      enabled: true
  health:
    mongodb:
      enabled: true
    redis:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        step: 1m
    tags:
      application: ${spring.application.name}
      version: ${spring.application.version:unknown}
```

**Custom Health Indicators:**
```java
@Component
public class WeatherApiHealthIndicator implements HealthIndicator {
    
    private final WeatherApiClient weatherApiClient;
    
    @Override
    public Health health() {
        try {
            // Test external API connectivity
            weatherApiClient.testConnection();
            return Health.up()
                .withDetail("api", "Open-Meteo API is available")
                .withDetail("timestamp", Instant.now())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("api", "Open-Meteo API is unavailable")
                .withDetail("error", e.getMessage())
                .withDetail("timestamp", Instant.now())
                .build();
        }
    }
}
```

**Custom Metrics:**
```java
@Component
public class WeatherMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter weatherRequests;
    private final Timer syncDuration;
    
    public WeatherMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.weatherRequests = Counter.builder("weather.requests.total")
            .description("Total number of weather requests")
            .register(meterRegistry);
        this.syncDuration = Timer.builder("weather.sync.duration")
            .description("Duration of weather synchronization")
            .register(meterRegistry);
    }
    
    public void recordWeatherRequest(String locationId, String status) {
        weatherRequests.increment(
            Tags.of(
                Tag.of("location", locationId),
                Tag.of("status", status)
            )
        );
    }
    
    public Timer.Sample startSyncTimer() {
        return Timer.start(meterRegistry);
    }
}
```

**Benefits:**
- Production monitoring capabilities
- Custom health checks
- Performance metrics collection
- Integration with monitoring systems

This comprehensive backend technology stack provides a robust, scalable, and maintainable foundation for the weather data integration platform with excellent performance, monitoring, and development experience.