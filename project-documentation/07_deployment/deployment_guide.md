# Deployment Guide

## Overview

This document provides comprehensive deployment instructions for the Weather Data Integration Platform, covering development, staging, and production environments. The platform uses Docker containers and supports multiple deployment strategies including local development and cloud deployment.

## Prerequisites

### System Requirements

#### Development Environment
- **Operating System**: Windows 10/11, macOS 10.15+, or Linux (Ubuntu 20.04+)
- **Docker**: Version 20.10+
- **Docker Compose**: Version 2.0+
- **Node.js**: Version 18+ (for frontend development)
- **Java**: Version 17+ (for backend development)
- **Maven**: Version 3.8+ (for backend builds)
- **Git**: Version 2.30+

#### Production Environment
- **Container Runtime**: Docker 20.10+ or containerd
- **Storage**: Persistent storage for MongoDB

### Required Services

- **MongoDB**: Version 7.0+ for data storage
- **External Weather APIs**: Open-Meteo, OpenWeatherMap

## Environment Configuration

### Environment Variables

#### Frontend Environment Variables
```bash
# .env.local
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
NEXT_PUBLIC_WEATHER_API_KEY=your-weather-api-key
NEXT_PUBLIC_MAPBOX_ACCESS_TOKEN=your-mapbox-token
NEXT_PUBLIC_ENABLE_ANALYTICS=true
NEXT_PUBLIC_SENTRY_DSN=your-sentry-dsn
```

#### Backend Environment Variables
```bash
# .env
# Application Configuration
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
SERVER_CONTEXT_PATH=/api

# Database Configuration
MONGODB_URI=mongodb://username:password@mongodb:27017/weatherdb
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
CORS_ORIGINS=http://localhost:3000,https://weatherapp.com
```

### Configuration Files

#### Docker Compose Configuration
```yaml
# docker-compose.yml
version: '3.8'

services:
  # Frontend Application
  frontend:
    build:
      context: .
      dockerfile: Dockerfile.frontend
    ports:
      - "3000:3000"
    environment:
      - NEXT_PUBLIC_API_BASE_URL=http://backend:8080/api
      - NODE_ENV=production
    depends_on:
      - backend
    networks:
      - weather-network
    restart: unless-stopped

  # Backend Application
  backend:
    build:
      context: ./weather-api-backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - MONGODB_URI=mongodb://mongodb:27017/weatherdb
      - JWT_SECRET=${JWT_SECRET}
      - OPEN_WEATHER_MAP_API_KEY=${OPEN_WEATHER_MAP_API_KEY}
    depends_on:
      - mongodb
    networks:
      - weather-network
    restart: unless-stopped

  # MongoDB Database
  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
      - ./scripts/init-mongodb.js:/docker-entrypoint-initdb.d/init-mongodb.js:ro
    environment:
      - MONGO_INITDB_ROOT_USERNAME=admin
      - MONGO_INITDB_ROOT_PASSWORD=${MONGODB_PASSWORD}
      - MONGO_INITDB_DATABASE=weatherdb
    networks:
      - weather-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  mongodb_data:

networks:
  weather-network:
    driver: bridge
```

## Deployment Strategies

### Local Development Deployment

#### 1. Clone and Setup
```bash
# Open the repository
cd weather-data-platform

# Copy environment files
cp .env.example .env.local
cp weather-api-backend/.env.example weather-api-backend/.env

# Install dependencies
npm install
cd weather-api-backend && mvn clean install
```

#### 2. Start Services
```bash
# Start with Docker Compose
docker-compose up -d

# Or start individually
docker-compose up mongodb
npm run dev  # Frontend
mvn spring-boot:run  # Backend
```

#### 3. Verify Deployment
```bash
# Check services
docker-compose ps

# Test API endpoints
curl http://localhost:8080/api/actuator/health
curl http://localhost:3000/api/health

# Access applications
echo "Frontend: http://localhost:3000"
echo "Backend API: http://localhost:8080/api"
echo "MongoDB: mongodb://localhost:27017"
```

### Staging Environment Deployment

#### 1. Prepare Staging Environment
```bash
# Create staging directory
mkdir -p /opt/weatherapp/staging
cd /opt/weatherapp/staging

# Copy deployment files
cp /path/to/weatherapp/docker-compose.staging.yml .
cp /path/to/weatherapp/.env.staging .env
cp -r /path/to/weatherapp/scripts/ .
```

#### 2. Configure Staging Environment
```bash
# Edit environment variables
nano .env

# Set staging-specific values
SPRING_PROFILES_ACTIVE=staging
MONGODB_URI=mongodb://staging-mongodb:27017/weatherdb_staging
CORS_ORIGINS=https://staging.weatherapp.com
LOG_LEVEL=DEBUG
```

#### 3. Deploy to Staging
```bash
# Build and deploy
docker-compose -f docker-compose.staging.yml up -d --build

# Run database migrations
docker-compose -f docker-compose.staging.yml exec backend \
  java -jar /app/weather-api.jar --spring.profiles.active=staging

# Verify deployment
docker-compose -f docker-compose.staging.yml logs
```

### Production Environment Deployment

#### 1. Production Prerequisites
```bash
# Set up production server
sudo apt update && sudo apt upgrade -y
sudo apt install docker.io docker-compose -y
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
```

#### 2. Production Configuration
```bash
# Create production directory
sudo mkdir -p /opt/weatherapp/production
sudo chown -R $USER:$USER /opt/weatherapp

# Copy production files
cp docker-compose.production.yml /opt/weatherapp/production/
cp .env.production /opt/weatherapp/production/.env
cp -r scripts/ /opt/weatherapp/production/

# Configure production environment
nano /opt/weatherapp/production/.env
```

#### 3. Production Deployment
```bash
# Navigate to production directory
cd /opt/weatherapp/production

# Build and deploy
docker-compose -f docker-compose.production.yml up -d --build

# Verify deployment
docker-compose -f docker-compose.production.yml logs
```

## Monitoring and Observability

### 1. Application Monitoring

#### Health Checks
```bash
# Backend health check
curl http://localhost:8080/actuator/health

# Frontend health check
curl http://localhost:3000/api/health

# Database connectivity
docker-compose exec mongodb mongosh --eval "db.adminCommand('ping')"
```

### 2. Log Management

#### Log Aggregation
```bash
# View application logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Export logs to file
docker-compose logs --tail=1000 > application-logs.txt
```

#### Log Analysis
```bash
# Search for errors
docker-compose logs backend | grep ERROR

# Monitor specific endpoints
docker-compose logs backend | grep "/api/weather"

# Performance monitoring
docker stats
```

### 3. Performance Monitoring

#### Response Time Monitoring
```bash
# Test API response times
time curl http://localhost:8080/api/weather/loc_123

# Load testing with Apache Bench
ab -n 1000 -c 10 http://localhost:8080/api/weather/loc_123
```

## Backup and Recovery

### 1. Database Backup

#### MongoDB Backup Script
```bash
#!/bin/bash
# backup-mongodb.sh

BACKUP_DIR="/opt/backups/mongodb"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/weatherdb_${DATE}.gz"

# Create backup directory
mkdir -p $BACKUP_DIR

# Perform backup
mongodump --host localhost:27017 \
          --username admin \
          --password $MONGODB_PASSWORD \
          --db weatherdb \
          --gzip \
          --archive=$BACKUP_FILE

# Keep only last 7 days of backups
find $BACKUP_DIR -name "*.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_FILE"
```

#### Automated Backup
```bash
# Add to crontab
echo "0 2 * * * /opt/scripts/backup-mongodb.sh" | crontab -
```

### 2. Application Backup

#### Configuration Backup
```bash
#!/bin/bash
# backup-config.sh

BACKUP_DIR="/opt/backups/config"
DATE=$(date +%Y%m%d_%H%M%S)

# Backup configuration files
tar -czf "${BACKUP_DIR}/config_${DATE}.tar.gz" \
    docker-compose.yml \
    .env \
    scripts/

# Backup Docker images
docker save weatherapp/backend:latest | gzip > "${BACKUP_DIR}/backend_${DATE}.tar.gz"
docker save weatherapp/frontend:latest | gzip > "${BACKUP_DIR}/frontend_${DATE}.tar.gz"

echo "Configuration backup completed"
```

### 3. Disaster Recovery

#### Recovery Script
```bash
#!/bin/bash
# restore-backup.sh

BACKUP_DATE=$1
BACKUP_DIR="/opt/backups"

if [ -z "$BACKUP_DATE" ]; then
    echo "Usage: $0 <backup-date>"
    exit 1
fi

# Restore MongoDB
mongorestore --host localhost:27017 \
             --username admin \
             --password $MONGODB_PASSWORD \
             --gzip \
             --archive="${BACKUP_DIR}/mongodb/weatherdb_${BACKUP_DATE}.gz" \
             --drop

# Restore configuration
tar -xzf "${BACKUP_DIR}/config/config_${BACKUP_DATE}.tar.gz" -C /

# Restart services
docker-compose down
docker-compose up -d

echo "Recovery completed for date: $BACKUP_DATE"
```

## Troubleshooting

### Common Issues

#### 1. Database Connection Issues
```bash
# Check MongoDB status
docker-compose exec mongodb mongosh --eval "db.adminCommand('ping')"

# Check connection from backend
docker-compose exec backend curl http://mongodb:27017

# Verify environment variables
docker-compose exec backend env | grep MONGODB
```

#### 2. Application Startup Issues
```bash
# Check application logs
docker-compose logs backend

# Verify port bindings
netstat -tlnp | grep 8080

# Check Docker container status
docker-compose ps
```

#### 3. Performance Issues
```bash
# Monitor resource usage
docker stats

# Check database performance
docker-compose exec mongodb mongosh --eval "db.currentOp()"
```

### Health Check Endpoints

#### Backend Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Disk space health
curl http://localhost:8080/actuator/health/diskSpace
```

#### Frontend Health Checks
```bash
# Application health
curl http://localhost:3000/api/health

# Build information
curl http://localhost:3000/api/info
```

This comprehensive deployment guide ensures that the Weather Data Integration Platform can be successfully deployed across different environments with proper monitoring, backup, and troubleshooting capabilities.