/**
 * MongoDB Initialization / Migration Script
 * ==========================================
 *
 * this script to set up the weatherdb database with
 * proper collections, indexes, and validation schemas.
 *
 * Usage:
 *   mongosh weatherdb init-mongodb.js
 *
 * Or with Docker:
 *   docker exec -i mongodb mongosh weatherdb < init-mongodb.js
 */

// Switch to the weatherdb database
db = db.getSiblingDB('weatherdb');

print('--- Setting up Weather Database ---');

// 1. Create collections with validation schemas
print('Creating collections with validation...');

// Locations collection
db.createCollection('locations', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['name', 'country', 'latitude', 'longitude'],
            properties: {
                name: {
                    bsonType: 'string',
                    description: 'City name - required'
                },
                country: {
                    bsonType: 'string',
                    description: 'Country name or code - required'
                },
                latitude: {
                    bsonType: 'double',
                    minimum: -90,
                    maximum: 90,
                    description: 'Latitude coordinate - required'
                },
                longitude: {
                    bsonType: 'double',
                    minimum: -180,
                    maximum: 180,
                    description: 'Longitude coordinate - required'
                },
                displayName: {
                    bsonType: ['string', 'null'],
                    description: 'Optional user-set display name'
                },
                favorite: {
                    bsonType: 'bool',
                    description: 'Whether location is marked as favorite'
                },
                syncStatus: {
                    enum: ['NEVER_SYNCED', 'SUCCESS', 'FAILED', 'IN_PROGRESS', 'STALE'],
                    description: 'Status of last sync attempt'
                }
            }
        }
    }
});
print('  - locations collection created');

// Weather Snapshots collection
db.createCollection('weather_snapshots', {
    validator: {
        $jsonSchema: {
            bsonType: 'object',
            required: ['locationId'],
            properties: {
                locationId: {
                    bsonType: 'string',
                    description: 'Reference to Location document - required'
                },
                units: {
                    bsonType: 'string',
                    description: 'Units used for the weather data'
                },
                timezone: {
                    bsonType: 'string',
                    description: 'Timezone of the location'
                },
                conflictDetected: {
                    bsonType: 'bool',
                    description: 'Whether a conflict was detected'
                }
            }
        }
    }
});
print('  - weather_snapshots collection created');

// User Preferences collection
db.createCollection('user_preferences');
print('  - user_preferences collection created');

// 2. Create indexes
print('Creating indexes...');

// Locations indexes
db.locations.createIndex(
    { name: 1, country: 1 },
    { unique: true, name: 'city_country_unique_idx' }
);
db.locations.createIndex(
    { favorite: 1, name: 1 },
    { name: 'favorite_name_idx' }
);
db.locations.createIndex(
    { syncStatus: 1 },
    { name: 'sync_status_idx' }
);
print('  - locations indexes created');

// Weather Snapshots indexes
db.weather_snapshots.createIndex(
    { locationId: 1, fetchedAt: -1 },
    { name: 'location_fetched_idx' }
);
db.weather_snapshots.createIndex(
    { locationId: 1 },
    { name: 'location_id_idx' }
);
db.weather_snapshots.createIndex(
    { conflictDetected: 1 },
    { name: 'conflict_idx',
      partialFilterExpression: { conflictDetected: true } }
);
// TTL index: automatically delete snapshots older than 2 days
db.weather_snapshots.createIndex(
    { fetchedAt: 1 },
    { name: 'snapshot_ttl_idx', expireAfterSeconds: 172800 }
);
print('  - weather_snapshots indexes created');

// User Preferences indexes
db.user_preferences.createIndex(
    { userId: 1 },
    { unique: true, name: 'user_id_unique_idx' }
);
print('  - user_preferences indexes created');

// 3. Insert default user preferences
print('Inserting default preferences...');

db.user_preferences.updateOne(
    { userId: 'default' },
    {
        $setOnInsert: {
            userId: 'default',
            units: 'metric',
            refreshIntervalMinutes: 5,
            windSpeedUnit: 'kmh',
            precipitationUnit: 'mm',
            theme: 'dark',
            createdAt: new Date(),
            updatedAt: new Date()
        }
    },
    { upsert: true }
);
print('  - default preferences inserted');

// 4. Insert sample locations (optional, for demo purposes)
print('Inserting sample locations...');

const sampleLocations = [
    {
        name: 'Victoria Falls',
        country: 'Zimbabwe',
        latitude: -17.9243,
        longitude: 25.8572,
        favorite: true,
        syncStatus: 'NEVER_SYNCED',
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: 'Cape Town',
        country: 'South Africa',
        latitude: -33.9249,
        longitude: 18.4241,
        favorite: false,
        syncStatus: 'NEVER_SYNCED',
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        name: 'London',
        country: 'United Kingdom',
        latitude: 51.5074,
        longitude: -0.1278,
        favorite: false,
        syncStatus: 'NEVER_SYNCED',
        createdAt: new Date(),
        updatedAt: new Date()
    }
];

sampleLocations.forEach(loc => {
    db.locations.updateOne(
        { name: loc.name, country: loc.country },
        { $setOnInsert: loc },
        { upsert: true }
    );
});
print('  - sample locations inserted');

print('--- Database setup complete! ---');
print('Collections: ' + db.getCollectionNames().join(', '));
