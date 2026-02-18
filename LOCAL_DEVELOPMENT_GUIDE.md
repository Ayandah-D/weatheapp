# Local Development Guide

## How to Run the Weather Data Integration Platform Locally

This guide provides step-by-step instructions to run the Weather Data Integration Platform on your local machine.

## Prerequisites

### Required Software
- **Docker** (version 20.10+)
- **Docker Compose** (version 2.0+)
- **Node.js** (version 18+)
- **Java** (version 17+)
- **Maven** (version 3.8+)
- **Git**

### Verify Installation
```bash
# Check Docker
docker --version
docker-compose --version

# Check Node.js
node --version
npm --version

# Check Java
java --version
mvn --version

# Check Git
git --version
```

## Quick Start (Recommended)

### 1. Open the Repository
```bash
cd weather-data-platform
```

### 2. Set Up Environment Variables
```bash
# Copy environment files
cp .env.example .env.local
cp weather-api-backend/.env.example weather-api-backend/.env
```

### 3. Install Dependencies
```bash
# Install frontend dependencies
npm install

# Install backend dependencies
cd weather-api-backend && mvn clean install
cd ..
```

### 4. Start the Application
```bash
# Start all services with Docker Compose
docker-compose up -d
```

### 5. Verify Installation
```bash
# Check if services are running
docker-compose ps

# Test the applications
curl http://localhost:8080/api/actuator/health
curl http://localhost:3000/api/health
```

## Manual Development Setup

### Frontend Development (React + Next.js)

#### 1. Install Frontend Dependencies
```bash
cd weather-data-platform
npm install
```

#### 2. Configure Frontend Environment
Edit `.env.local`:
```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
NEXT_PUBLIC_WEATHER_API_KEY=your-weather-api-key
NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN=your-mapbox-token
```

#### 3. Start Frontend Development Server
```bash
npm run dev
```

#### 4. Access Frontend
Open your browser and navigate to: http://localhost:3000

### Backend Development (Spring Boot + Java)

#### 1. Install Backend Dependencies
```bash
cd weather-api-backend
mvn clean install
```

#### 2. Configure Backend Environment
Edit `.env` file:
```bash
# Application Configuration
SPRING_PROFILES_ACTIVE=development
SERVER_PORT=8080

# Database Configuration
MONGODB_URI=mongodb://localhost:27017/weatherdb

# External API Configuration
OPEN_METEO_BASE_URL=https://api.open-meteo.com/v1
OPEN_WEATHER_MAP_API_KEY=your-openweathermap-key

# Security Configuration
JWT_SECRET=your-jwt-secret-key

# CORS Configuration
CORS_ORIGINS=http://localhost:3000
```

#### 3. Start MongoDB (if not using Docker)
```bash
# Option 1: Using Docker (recommended)
docker run -d -p 27017:27017 --name weather-mongodb mongo:7.0

# Option 2: Using local MongoDB installation
mongod --dbpath /data/db
```

#### 4. Start Backend Development Server
```bash
# Run with Maven
mvn spring-boot:run

# Or run the compiled JAR
java -jar target/weather-api-1.0.0.jar
```

#### 5. Access Backend API
- **API Documentation**: http://localhost:8080/swagger-ui/index.html
- **Health Check**: http://localhost:8080/actuator/health
- **API Base URL**: http://localhost:8080/api

## Docker Development Setup

### Using Docker Compose (Recommended for Development)

#### 1. Start MongoDB and Backend
```bash
# Start only MongoDB
docker-compose up mongodb -d

# Start MongoDB and Backend
docker-compose up mongodb weather-api -d
```

#### 2. Start Frontend Separately
```bash
# In a separate terminal
cd weather-data-platform
npm run dev
```

#### 3. Full Development Stack
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## Configuration Options

### Environment Variables

#### Frontend (.env.local)
```bash
# API Configuration
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api

# Weather API Keys (Optional for basic functionality)
NEXT_PUBLIC_WEATHER_API_KEY=your-weather-api-key
NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN=your-mapbox-token

# Feature Flags
NEXT_PUBLIC_ENABLE_ANALYTICS=true
NEXT_PUBLIC_SENTRY_DSN=your-sentry-dsn
```

#### Backend (.env)
```bash
# Application Configuration
SPRING_PROFILES_ACTIVE=development
SERVER_PORT=8080

# Database Configuration
MONGODB_URI=mongodb://localhost:27017/weatherdb
MONGODB_DATABASE=weatherdb

# External API Configuration
OPEN_METEO_BASE_URL=https://api.open-meteo.com/v1
OPEN_WEATHER_MAP_API_KEY=your-openweathermap-key
GEOCODING_API_URL=https://geocoding-api.open-meteo.com/v1

# Security Configuration
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=3600000
BCRYPT_ROUNDS=12

# CORS Configuration
CORS_ORIGINS=http://localhost:3000

# Rate Limiting
RATE_LIMIT_REQUESTS_PER_MINUTE=60
```

## Development Workflow

### Frontend Development
```bash
# Start development server with hot reload
npm run dev

# Build for production
npm run build

# Run tests
npm test

# Run linting
npm run lint

# Run type checking
npm run typecheck
```

### Backend Development
```bash
# Run tests
mvn test

# Run with Maven
mvn spring-boot:run

# Package application
mvn clean package

# Run packaged JAR
java -jar target/weather-api-1.0.0.jar
```

### Database Operations

#### MongoDB Shell Access
```bash
# Access MongoDB shell
docker exec -it weather-mongodb mongosh

# Or if using local MongoDB
mongosh
```

#### Database Initialization
The application automatically initializes the database schema on startup. You can also manually run the initialization script:

```bash
# Run database initialization
docker exec -it weather-mongodb mongosh /docker-entrypoint-initdb.d/init-mongodb.js
```

## Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Check what's using port 3000
lsof -i :3000

# Kill processes using the ports
kill -9 <PID>
```

#### 2. MongoDB Connection Issues
```bash
# Check MongoDB status
docker ps | grep mongodb

# Restart MongoDB
docker restart weather-mongodb

# Check MongoDB logs
docker logs weather-mongodb
```

#### 3. Dependency Issues
```bash
# Clean and reinstall frontend dependencies
rm -rf node_modules package-lock.json
npm install

# Clean and rebuild backend
cd weather-api-backend
mvn clean
mvn install
```

#### 4. Environment Variable Issues
```bash
# Check environment variables
echo $SPRING_PROFILES_ACTIVE
echo $MONGODB_URI

# Reload environment
source .env
```

### Development Tips

#### 1. Hot Reload
- Frontend: Changes are automatically reflected in the browser
- Backend: Use `mvn spring-boot:run` for automatic restart on changes

#### 2. Debugging
- Frontend: Use browser developer tools
- Backend: Add breakpoints in your IDE or use `mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"`

#### 3. API Testing
```bash
# Test API endpoints
curl http://localhost:8080/api/locations
curl http://localhost:8080/api/weather/loc_123

# Test with authentication (when implemented)
curl -H "Authorization: Bearer your-jwt-token" http://localhost:8080/api/locations
```

## Production Build

### Build for Production
```bash
# Build frontend
cd weather-data-platform
npm run build

# Build backend
cd weather-api-backend
mvn clean package

# Start production services
docker-compose -f docker-compose.yml up -d
```

### Production Environment Variables
Create `.env.production` files with production values:
```bash
# Frontend
NEXT_PUBLIC_API_BASE_URL=https://your-domain.com/api

# Backend
SPRING_PROFILES_ACTIVE=production
MONGODB_URI=mongodb://your-production-db:27017/weatherdb
```

## Performance Optimization

### Development Performance
```bash
# Use Docker volumes for faster builds
docker-compose up -d --build --no-cache

# Monitor resource usage
docker stats

# Clean up unused Docker resources
docker system prune -f
```

### Local Development Tips
- Use Docker Compose for consistent environment
- Keep MongoDB data in volumes for persistence
- Use environment-specific configuration files
- Monitor logs for debugging: `docker-compose logs -f`

## Next Steps

1. **Explore the Application**: Visit http://localhost:3000 to see the frontend
2. **Test APIs**: Visit http://localhost:8080/swagger-ui/index.html for API documentation
3. **Add Data**: Use the frontend to add locations and test weather data fetching
4. **Customize**: Modify components and APIs as needed for your use case

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Review the application logs: `docker-compose logs -f`
3. Check the project documentation in the `project-documentation/` folder
4. Create an issue on the project repository