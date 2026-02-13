# Complete API Project Documentation

## Table of Contents
1. [Setup Instructions](#setup-instructions)
2. [Architecture Decisions](#architecture-decisions)
3. [Database Schema](#database-schema)
4. [Data Integrity](#data-integrity)
5. [Error Handling](#error-handling)
6. [External API Integration](#external-api-integration)
7. [API Documentation](#api-documentation)

---

## Setup Instructions

### Prerequisites
- Node.js 18+ or Python 3.9+
- PostgreSQL 14+ or MongoDB 6+
- Redis 7+ (for caching)
- Git

### Initial Setup

#### 1. Clone and Install Dependencies

```bash
# Clone the repository
git clone https://github.com/yourusername/your-project.git
cd your-project

# For Node.js projects
npm install

# For Python projects
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

#### 2. Environment Configuration

Create a `.env` file in the project root:

```bash
# Database
DATABASE_URL=postgresql://user:password@localhost:5432/dbname
DB_POOL_MIN=2
DB_POOL_MAX=10

# Redis
REDIS_URL=redis://localhost:6379
REDIS_TTL=3600

# External APIs
EXTERNAL_API_KEY=your_api_key_here
EXTERNAL_API_BASE_URL=https://api.example.com/v1
EXTERNAL_API_TIMEOUT=30000
EXTERNAL_API_RATE_LIMIT=100

# Application
NODE_ENV=development
PORT=3000
LOG_LEVEL=info

# Security
JWT_SECRET=your_super_secret_jwt_key_change_this
JWT_EXPIRY=7d
BCRYPT_ROUNDS=12
```

#### 3. Database Setup

```bash
# Create database
createdb your_database_name

# Run migrations
npm run migrate:latest  # or python manage.py migrate

# Seed initial data (optional)
npm run seed  # or python manage.py seed
```

#### 4. Verify Installation

```bash
# Run tests
npm test  # or pytest

# Start development server
npm run dev  # or python manage.py runserver
```

### Docker Setup (Alternative)

```bash
# Build and start containers
docker-compose up -d

# Run migrations
docker-compose exec app npm run migrate:latest

# View logs
docker-compose logs -f app
```

---

## Architecture Decisions

### 1. **Layered Architecture Pattern**

**Decision:** Implement a 3-tier architecture (Controller → Service → Repository)

**Rationale:**
- **Separation of Concerns:** Each layer has distinct responsibilities
- **Testability:** Easier to unit test individual layers
- **Maintainability:** Changes in one layer don't cascade to others
- **Scalability:** Layers can be scaled independently

```
┌─────────────────┐
│   Controllers   │  ← HTTP handling, validation
├─────────────────┤
│    Services     │  ← Business logic
├─────────────────┤
│  Repositories   │  ← Data access
├─────────────────┤
│    Database     │
└─────────────────┘
```

### 2. **Database Choice: PostgreSQL**

**Decision:** Use PostgreSQL as primary database

**Rationale:**
- ACID compliance for data integrity
- Rich data types (JSON, arrays, timestamps)
- Powerful indexing and query optimization
- Strong community support
- Excellent for relational data with complex queries

### 3. **Caching Strategy: Redis**

**Decision:** Implement Redis for caching and rate limiting

**Rationale:**
- Reduce database load for frequently accessed data
- Fast in-memory operations
- Support for complex data structures
- Built-in expiration mechanisms
- Pub/sub for real-time features

### 4. **API Design: RESTful**

**Decision:** Follow REST principles with versioning

**Rationale:**
- Industry standard, widely understood
- Stateless and cacheable
- Clear resource-based URLs
- Standard HTTP methods and status codes

### 5. **Error Handling: Centralized Middleware**

**Decision:** Implement global error handling middleware

**Rationale:**
- Consistent error responses across application
- Single point for logging and monitoring
- Prevents sensitive information leakage
- Easier debugging and maintenance

---

## Database Schema

### Entity Relationship Diagram

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│    Users     │────1:N──│    Orders    │────N:M──│   Products   │
└──────────────┘         └──────────────┘         └──────────────┘
       │                        │                         │
       │                        │                         │
       │                  ┌──────────────┐                │
       └─────────1:N──────│   Reviews    │────N:1─────────┘
                          └──────────────┘
```

### Schema Definitions

#### Users Table

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) DEFAULT 'user' CHECK (role IN ('user', 'admin', 'moderator')),
    email_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Indexes
    CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
```

#### Products Table

```sql
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    stock_quantity INTEGER DEFAULT 0 CHECK (stock_quantity >= 0),
    category VARCHAR(100),
    tags TEXT[],
    metadata JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Full-text search
    search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B')
    ) STORED
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_search ON products USING GIN(search_vector);
CREATE INDEX idx_products_tags ON products USING GIN(tags);
```

#### Orders Table

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'pending' CHECK (
        status IN ('pending', 'processing', 'shipped', 'delivered', 'cancelled', 'refunded')
    ),
    subtotal DECIMAL(10, 2) NOT NULL CHECK (subtotal >= 0),
    tax DECIMAL(10, 2) DEFAULT 0 CHECK (tax >= 0),
    shipping DECIMAL(10, 2) DEFAULT 0 CHECK (shipping >= 0),
    total DECIMAL(10, 2) NOT NULL CHECK (total >= 0),
    shipping_address JSONB,
    billing_address JSONB,
    payment_method VARCHAR(50),
    payment_status VARCHAR(20) DEFAULT 'pending',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT total_calculation CHECK (total = subtotal + tax + shipping)
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_order_number ON orders(order_number);
```

#### Order Items Table

```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    subtotal DECIMAL(10, 2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT subtotal_calculation CHECK (subtotal = quantity * unit_price),
    UNIQUE(order_id, product_id)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
```

#### Reviews Table

```sql
CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title VARCHAR(255),
    comment TEXT,
    is_verified_purchase BOOLEAN DEFAULT FALSE,
    helpful_count INTEGER DEFAULT 0 CHECK (helpful_count >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    UNIQUE(user_id, product_id)
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);
```

### Triggers for Updated_at

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

---

## Data Integrity

### 1. **Database-Level Constraints**

#### Primary Keys
- All tables use UUID for primary keys
- Provides global uniqueness
- Prevents ID enumeration attacks
- Better for distributed systems

#### Foreign Keys
- All relationships enforced with foreign keys
- `ON DELETE CASCADE` for dependent data
- `ON DELETE RESTRICT` for referenced data (e.g., products in orders)

#### Check Constraints
```sql
-- Price validation
CHECK (price >= 0)

-- Stock quantity validation
CHECK (stock_quantity >= 0)

-- Rating range validation
CHECK (rating BETWEEN 1 AND 5)

-- Status validation
CHECK (status IN ('pending', 'processing', 'shipped', 'delivered'))

-- Email format validation
CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
```

#### Unique Constraints
```sql
-- Prevent duplicate emails
UNIQUE(email)

-- Prevent duplicate usernames
UNIQUE(username)

-- Prevent duplicate SKUs
UNIQUE(sku)

-- One review per user per product
UNIQUE(user_id, product_id)
```

### 2. **Application-Level Validation**

#### Input Validation Example (Node.js)

```javascript
const { body, param, query, validationResult } = require('express-validator');

const validateCreateProduct = [
    body('name')
        .trim()
        .notEmpty().withMessage('Name is required')
        .isLength({ min: 3, max: 255 }).withMessage('Name must be 3-255 characters'),
    
    body('sku')
        .trim()
        .notEmpty().withMessage('SKU is required')
        .matches(/^[A-Z0-9-]+$/).withMessage('SKU must contain only uppercase letters, numbers, and hyphens'),
    
    body('price')
        .isFloat({ min: 0 }).withMessage('Price must be a positive number')
        .custom(value => {
            // Ensure max 2 decimal places
            return /^\d+(\.\d{1,2})?$/.test(value);
        }).withMessage('Price can have maximum 2 decimal places'),
    
    body('stock_quantity')
        .optional()
        .isInt({ min: 0 }).withMessage('Stock quantity must be a non-negative integer'),
    
    body('email')
        .isEmail().withMessage('Invalid email format')
        .normalizeEmail(),
    
    (req, res, next) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({ 
                error: 'Validation failed', 
                details: errors.array() 
            });
        }
        next();
    }
];
```

### 3. **Transaction Management**

#### Ensure Data Consistency

```javascript
async function createOrderWithItems(orderData, items) {
    const client = await pool.connect();
    
    try {
        await client.query('BEGIN');
        
        // 1. Create order
        const orderResult = await client.query(
            `INSERT INTO orders (user_id, order_number, subtotal, total, status)
             VALUES ($1, $2, $3, $4, $5)
             RETURNING id`,
            [orderData.userId, orderData.orderNumber, orderData.subtotal, orderData.total, 'pending']
        );
        
        const orderId = orderResult.rows[0].id;
        
        // 2. Insert order items and update stock
        for (const item of items) {
            // Check stock availability
            const stockCheck = await client.query(
                'SELECT stock_quantity FROM products WHERE id = $1 FOR UPDATE',
                [item.productId]
            );
            
            if (stockCheck.rows[0].stock_quantity < item.quantity) {
                throw new Error(`Insufficient stock for product ${item.productId}`);
            }
            
            // Insert order item
            await client.query(
                `INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
                 VALUES ($1, $2, $3, $4, $5)`,
                [orderId, item.productId, item.quantity, item.unitPrice, item.subtotal]
            );
            
            // Update stock
            await client.query(
                'UPDATE products SET stock_quantity = stock_quantity - $1 WHERE id = $2',
                [item.quantity, item.productId]
            );
        }
        
        await client.query('COMMIT');
        return orderId;
        
    } catch (error) {
        await client.query('ROLLBACK');
        throw error;
    } finally {
        client.release();
    }
}
```

### 4. **Data Sanitization**

```javascript
const sanitizeHtml = require('sanitize-html');

function sanitizeUserInput(data) {
    return {
        ...data,
        // Remove HTML tags from text fields
        name: sanitizeHtml(data.name, { allowedTags: [] }),
        description: sanitizeHtml(data.description, {
            allowedTags: ['b', 'i', 'em', 'strong', 'p', 'br'],
            allowedAttributes: {}
        }),
        // Trim whitespace
        email: data.email?.trim().toLowerCase(),
        // Remove null bytes
        comment: data.comment?.replace(/\0/g, '')
    };
}
```

### 5. **Audit Logging**

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_name VARCHAR(50) NOT NULL,
    record_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('INSERT', 'UPDATE', 'DELETE')),
    old_values JSONB,
    new_values JSONB,
    changed_by UUID REFERENCES users(id),
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX idx_audit_logs_table_record ON audit_logs(table_name, record_id);
CREATE INDEX idx_audit_logs_changed_at ON audit_logs(changed_at DESC);
```

---

## Error Handling

### 1. **Error Hierarchy**

```javascript
// base-error.js
class BaseError extends Error {
    constructor(message, statusCode, isOperational = true) {
        super(message);
        this.statusCode = statusCode;
        this.isOperational = isOperational;
        Error.captureStackTrace(this, this.constructor);
    }
}

// validation-error.js
class ValidationError extends BaseError {
    constructor(message, details = []) {
        super(message, 400);
        this.details = details;
    }
}

// not-found-error.js
class NotFoundError extends BaseError {
    constructor(resource) {
        super(`${resource} not found`, 404);
    }
}

// unauthorized-error.js
class UnauthorizedError extends BaseError {
    constructor(message = 'Unauthorized') {
        super(message, 401);
    }
}

// forbidden-error.js
class ForbiddenError extends BaseError {
    constructor(message = 'Forbidden') {
        super(message, 403);
    }
}

// conflict-error.js
class ConflictError extends BaseError {
    constructor(message) {
        super(message, 409);
    }
}

// rate-limit-error.js
class RateLimitError extends BaseError {
    constructor(retryAfter = 60) {
        super('Too many requests', 429);
        this.retryAfter = retryAfter;
    }
}

// external-api-error.js
class ExternalAPIError extends BaseError {
    constructor(service, message, originalError) {
        super(`External API error from ${service}: ${message}`, 502);
        this.service = service;
        this.originalError = originalError;
    }
}

module.exports = {
    BaseError,
    ValidationError,
    NotFoundError,
    UnauthorizedError,
    ForbiddenError,
    ConflictError,
    RateLimitError,
    ExternalAPIError
};
```

### 2. **Global Error Handler Middleware**

```javascript
// error-handler.js
const logger = require('./logger');
const { BaseError } = require('./errors');

function errorHandler(err, req, res, next) {
    // Log error
    logger.error({
        message: err.message,
        stack: err.stack,
        path: req.path,
        method: req.method,
        ip: req.ip,
        userId: req.user?.id
    });
    
    // Operational errors (known errors)
    if (err.isOperational) {
        return res.status(err.statusCode).json({
            error: {
                message: err.message,
                ...(err.details && { details: err.details }),
                ...(err.retryAfter && { retryAfter: err.retryAfter })
            }
        });
    }
    
    // Database errors
    if (err.code === '23505') { // Unique violation
        return res.status(409).json({
            error: {
                message: 'Resource already exists',
                field: err.constraint
            }
        });
    }
    
    if (err.code === '23503') { // Foreign key violation
        return res.status(400).json({
            error: {
                message: 'Referenced resource does not exist'
            }
        });
    }
    
    // JWT errors
    if (err.name === 'JsonWebTokenError') {
        return res.status(401).json({
            error: { message: 'Invalid token' }
        });
    }
    
    if (err.name === 'TokenExpiredError') {
        return res.status(401).json({
            error: { message: 'Token expired' }
        });
    }
    
    // Programming or unknown errors - don't leak details
    return res.status(500).json({
        error: {
            message: process.env.NODE_ENV === 'production' 
                ? 'Internal server error' 
                : err.message
        }
    });
}

// Async error wrapper
function asyncHandler(fn) {
    return (req, res, next) => {
        Promise.resolve(fn(req, res, next)).catch(next);
    };
}

// Unhandled rejection handler
process.on('unhandledRejection', (reason, promise) => {
    logger.error('Unhandled Rejection at:', promise, 'reason:', reason);
    // In production, you might want to restart the process
    if (process.env.NODE_ENV === 'production') {
        process.exit(1);
    }
});

// Uncaught exception handler
process.on('uncaughtException', (error) => {
    logger.error('Uncaught Exception:', error);
    process.exit(1); // Must exit
});

module.exports = { errorHandler, asyncHandler };
```

### 3. **Service-Level Error Handling**

```javascript
// product-service.js
const { NotFoundError, ConflictError } = require('../errors');

class ProductService {
    async getProductById(id) {
        const product = await db.query(
            'SELECT * FROM products WHERE id = $1',
            [id]
        );
        
        if (!product.rows.length) {
            throw new NotFoundError('Product');
        }
        
        return product.rows[0];
    }
    
    async createProduct(data) {
        try {
            const result = await db.query(
                `INSERT INTO products (sku, name, price, stock_quantity)
                 VALUES ($1, $2, $3, $4)
                 RETURNING *`,
                [data.sku, data.name, data.price, data.stockQuantity]
            );
            
            return result.rows[0];
            
        } catch (error) {
            if (error.code === '23505') { // Unique constraint violation
                throw new ConflictError('Product with this SKU already exists');
            }
            throw error;
        }
    }
    
    async updateStock(productId, quantity) {
        const result = await db.query(
            `UPDATE products 
             SET stock_quantity = stock_quantity + $1 
             WHERE id = $2 
             RETURNING stock_quantity`,
            [quantity, productId]
        );
        
        if (!result.rows.length) {
            throw new NotFoundError('Product');
        }
        
        if (result.rows[0].stock_quantity < 0) {
            throw new ValidationError('Insufficient stock');
        }
        
        return result.rows[0];
    }
}
```

### 4. **Client-Friendly Error Responses**

```javascript
// Standard error response format
{
    "error": {
        "message": "Validation failed",
        "details": [
            {
                "field": "email",
                "message": "Invalid email format"
            },
            {
                "field": "price",
                "message": "Price must be greater than 0"
            }
        ],
        "code": "VALIDATION_ERROR",
        "timestamp": "2025-02-13T10:30:00Z",
        "path": "/api/v1/products",
        "requestId": "abc-123-def-456"
    }
}
```

---

## External API Integration

### 1. **API Client Structure**

```javascript
// external-api-client.js
const axios = require('axios');
const { ExternalAPIError, RateLimitError } = require('../errors');
const logger = require('../logger');
const cache = require('../cache');

class ExternalAPIClient {
    constructor(config) {
        this.baseURL = config.baseURL;
        this.apiKey = config.apiKey;
        this.timeout = config.timeout || 30000;
        this.retryAttempts = config.retryAttempts || 3;
        this.retryDelay = config.retryDelay || 1000;
        this.rateLimit = config.rateLimit || 100; // requests per minute
        this.rateLimitWindow = 60000; // 1 minute
        
        this.client = axios.create({
            baseURL: this.baseURL,
            timeout: this.timeout,
            headers: {
                'Authorization': `Bearer ${this.apiKey}`,
                'Content-Type': 'application/json',
                'User-Agent': 'YourApp/1.0'
            }
        });
        
        this.setupInterceptors();
        this.requestCount = 0;
        this.resetRateLimitTimer();
    }
    
    setupInterceptors() {
        // Request interceptor
        this.client.interceptors.request.use(
            async (config) => {
                // Check rate limit
                if (this.requestCount >= this.rateLimit) {
                    throw new RateLimitError(60);
                }
                
                this.requestCount++;
                
                // Add request ID for tracing
                config.headers['X-Request-ID'] = this.generateRequestId();
                
                // Log request
                logger.info({
                    type: 'external_api_request',
                    method: config.method,
                    url: config.url,
                    requestId: config.headers['X-Request-ID']
                });
                
                return config;
            },
            (error) => {
                return Promise.reject(error);
            }
        );
        
        // Response interceptor
        this.client.interceptors.response.use(
            (response) => {
                // Log successful response
                logger.info({
                    type: 'external_api_response',
                    status: response.status,
                    requestId: response.config.headers['X-Request-ID']
                });
                
                return response;
            },
            async (error) => {
                return this.handleError(error);
            }
        );
    }
    
    async handleError(error) {
        const config = error.config;
        
        // Don't retry if no config or already retried max times
        if (!config || config.__retryCount >= this.retryAttempts) {
            throw this.transformError(error);
        }
        
        config.__retryCount = config.__retryCount || 0;
        
        // Retry on network errors or 5xx errors
        if (!error.response || error.response.status >= 500) {
            config.__retryCount++;
            
            // Exponential backoff
            const delay = this.retryDelay * Math.pow(2, config.__retryCount - 1);
            
            logger.warn({
                type: 'external_api_retry',
                attempt: config.__retryCount,
                delay,
                error: error.message
            });
            
            await this.sleep(delay);
            return this.client(config);
        }
        
        throw this.transformError(error);
    }
    
    transformError(error) {
        if (error.response) {
            const status = error.response.status;
            const data = error.response.data;
            
            if (status === 429) {
                const retryAfter = error.response.headers['retry-after'] || 60;
                return new RateLimitError(parseInt(retryAfter));
            }
            
            return new ExternalAPIError(
                this.baseURL,
                data.message || error.message,
                error
            );
        }
        
        if (error.code === 'ECONNABORTED') {
            return new ExternalAPIError(
                this.baseURL,
                'Request timeout',
                error
            );
        }
        
        return new ExternalAPIError(
            this.baseURL,
            error.message,
            error
        );
    }
    
    async get(endpoint, params = {}, options = {}) {
        const cacheKey = options.cacheKey || `api:${endpoint}:${JSON.stringify(params)}`;
        const cacheTTL = options.cacheTTL || 300; // 5 minutes default
        
        // Check cache first
        if (options.cache !== false) {
            const cached = await cache.get(cacheKey);
            if (cached) {
                logger.debug({ type: 'cache_hit', key: cacheKey });
                return JSON.parse(cached);
            }
        }
        
        const response = await this.client.get(endpoint, { params });
        
        // Cache successful response
        if (options.cache !== false) {
            await cache.set(cacheKey, JSON.stringify(response.data), cacheTTL);
        }
        
        return response.data;
    }
    
    async post(endpoint, data, options = {}) {
        const response = await this.client.post(endpoint, data);
        return response.data;
    }
    
    async put(endpoint, data) {
        const response = await this.client.put(endpoint, data);
        return response.data;
    }
    
    async delete(endpoint) {
        const response = await this.client.delete(endpoint);
        return response.data;
    }
    
    resetRateLimitTimer() {
        setInterval(() => {
            this.requestCount = 0;
        }, this.rateLimitWindow);
    }
    
    generateRequestId() {
        return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }
    
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

module.exports = ExternalAPIClient;
```

### 2. **Service-Specific Implementation**

```javascript
// payment-api-service.js
const ExternalAPIClient = require('./external-api-client');

class PaymentAPIService {
    constructor() {
        this.client = new ExternalAPIClient({
            baseURL: process.env.PAYMENT_API_BASE_URL,
            apiKey: process.env.PAYMENT_API_KEY,
            timeout: 15000,
            retryAttempts: 2,
            rateLimit: 50
        });
    }
    
    async createPaymentIntent(amount, currency, metadata) {
        try {
            return await this.client.post('/payment-intents', {
                amount,
                currency,
                metadata,
                idempotency_key: this.generateIdempotencyKey(metadata.orderId)
            });
        } catch (error) {
            logger.error({
                type: 'payment_intent_failed',
                amount,
                currency,
                error: error.message
            });
            throw error;
        }
    }
    
    async getPaymentStatus(paymentIntentId) {
        return await this.client.get(`/payment-intents/${paymentIntentId}`, {}, {
            cache: false // Don't cache payment status
        });
    }
    
    async refundPayment(paymentIntentId, amount) {
        return await this.client.post('/refunds', {
            payment_intent: paymentIntentId,
            amount,
            idempotency_key: this.generateIdempotencyKey(`refund-${paymentIntentId}`)
        });
    }
    
    generateIdempotencyKey(identifier) {
        const crypto = require('crypto');
        return crypto.createHash('sha256')
            .update(identifier + process.env.IDEMPOTENCY_SALT)
            .digest('hex');
    }
}

module.exports = new PaymentAPIService();
```

### 3. **Webhook Handling**

```javascript
// webhook-handler.js
const crypto = require('crypto');
const { UnauthorizedError } = require('../errors');

class WebhookHandler {
    constructor(secret) {
        this.secret = secret;
    }
    
    verifySignature(payload, signature) {
        const expectedSignature = crypto
            .createHmac('sha256', this.secret)
            .update(payload)
            .digest('hex');
        
        if (signature !== expectedSignature) {
            throw new UnauthorizedError('Invalid webhook signature');
        }
    }
    
    async handlePaymentWebhook(req, res) {
        const signature = req.headers['x-webhook-signature'];
        const payload = JSON.stringify(req.body);
        
        this.verifySignature(payload, signature);
        
        const event = req.body;
        
        switch (event.type) {
            case 'payment.succeeded':
                await this.handlePaymentSucceeded(event.data);
                break;
                
            case 'payment.failed':
                await this.handlePaymentFailed(event.data);
                break;
                
            case 'refund.completed':
                await this.handleRefundCompleted(event.data);
                break;
                
            default:
                logger.warn({ type: 'unknown_webhook_event', eventType: event.type });
        }
        
        res.status(200).json({ received: true });
    }
    
    async handlePaymentSucceeded(data) {
        const orderId = data.metadata.order_id;
        
        await db.query(
            `UPDATE orders 
             SET payment_status = 'completed', 
                 status = 'processing' 
             WHERE id = $1`,
            [orderId]
        );
        
        logger.info({ type: 'payment_succeeded', orderId });
    }
    
    async handlePaymentFailed(data) {
        const orderId = data.metadata.order_id;
        
        await db.query(
            `UPDATE orders 
             SET payment_status = 'failed', 
                 status = 'cancelled' 
             WHERE id = $1`,
            [orderId]
        );
        
        logger.warn({ type: 'payment_failed', orderId, reason: data.failure_reason });
    }
    
    async handleRefundCompleted(data) {
        const orderId = data.metadata.order_id;
        
        await db.query(
            `UPDATE orders 
             SET payment_status = 'refunded', 
                 status = 'refunded' 
             WHERE id = $1`,
            [orderId]
        );
        
        logger.info({ type: 'refund_completed', orderId });
    }
}

module.exports = WebhookHandler;
```

### 4. **Circuit Breaker Pattern**

```javascript
// circuit-breaker.js
class CircuitBreaker {
    constructor(options = {}) {
        this.failureThreshold = options.failureThreshold || 5;
        this.resetTimeout = options.resetTimeout || 60000; // 1 minute
        this.monitoringPeriod = options.monitoringPeriod || 10000; // 10 seconds
        
        this.state = 'CLOSED'; // CLOSED, OPEN, HALF_OPEN
        this.failureCount = 0;
        this.lastFailureTime = null;
        this.successCount = 0;
    }
    
    async execute(fn) {
        if (this.state === 'OPEN') {
            if (Date.now() - this.lastFailureTime >= this.resetTimeout) {
                this.state = 'HALF_OPEN';
                this.successCount = 0;
            } else {
                throw new Error('Circuit breaker is OPEN');
            }
        }
        
        try {
            const result = await fn();
            this.onSuccess();
            return result;
        } catch (error) {
            this.onFailure();
            throw error;
        }
    }
    
    onSuccess() {
        this.failureCount = 0;
        
        if (this.state === 'HALF_OPEN') {
            this.successCount++;
            if (this.successCount >= 3) {
                this.state = 'CLOSED';
                logger.info('Circuit breaker closed');
            }
        }
    }
    
    onFailure() {
        this.failureCount++;
        this.lastFailureTime = Date.now();
        
        if (this.failureCount >= this.failureThreshold) {
            this.state = 'OPEN';
            logger.warn('Circuit breaker opened');
        }
    }
}

module.exports = CircuitBreaker;
```

---

## API Documentation

### OpenAPI/Swagger Specification

```yaml
openapi: 3.0.3
info:
  title: E-Commerce API
  description: RESTful API for e-commerce platform
  version: 1.0.0
  contact:
    name: API Support
    email: api@example.com
  license:
    name: MIT

servers:
  - url: https://api.example.com/v1
    description: Production server
  - url: https://staging-api.example.com/v1
    description: Staging server

tags:
  - name: Products
    description: Product management
  - name: Orders
    description: Order operations
  - name: Users
    description: User management

paths:
  /products:
    get:
      tags:
        - Products
      summary: List all products
      description: Retrieve paginated list of products with optional filtering
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            minimum: 1
            default: 1
        - name: limit
          in: query
          schema:
            type: integer
            minimum: 1
            maximum: 100
            default: 20
        - name: category
          in: query
          schema:
            type: string
        - name: minPrice
          in: query
          schema:
            type: number
            format: float
        - name: maxPrice
          in: query
          schema:
            type: number
            format: float
        - name: search
          in: query
          description: Full-text search in name and description
          schema:
            type: string
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/Product'
                  pagination:
                    $ref: '#/components/schemas/Pagination'
        '400':
          $ref: '#/components/responses/BadRequest'
        '500':
          $ref: '#/components/responses/InternalError'
    
    post:
      tags:
        - Products
      summary: Create a new product
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateProductRequest'
      responses:
        '201':
          description: Product created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Product'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '409':
          $ref: '#/components/responses/Conflict'

  /products/{id}:
    get:
      tags:
        - Products
      summary: Get product by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Product'
        '404':
          $ref: '#/components/responses/NotFound'
    
    put:
      tags:
        - Products
      summary: Update product
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateProductRequest'
      responses:
        '200':
          description: Product updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Product'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
    
    delete:
      tags:
        - Products
      summary: Delete product
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: Product deleted successfully
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'

  /orders:
    post:
      tags:
        - Orders
      summary: Create new order
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOrderRequest'
      responses:
        '201':
          description: Order created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Order'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'

  /users/register:
    post:
      tags:
        - Users
      summary: Register new user
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RegisterRequest'
      responses:
        '201':
          description: User registered successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
        '400':
          $ref: '#/components/responses/BadRequest'
        '409':
          $ref: '#/components/responses/Conflict'

  /users/login:
    post:
      tags:
        - Users
      summary: User login
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - email
                - password
              properties:
                email:
                  type: string
                  format: email
                password:
                  type: string
      responses:
        '200':
          description: Login successful
          content:
            application/json:
              schema:
                type: object
                properties:
                  token:
                    type: string
                  user:
                    $ref: '#/components/schemas/User'
        '401':
          $ref: '#/components/responses/Unauthorized'

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    Product:
      type: object
      properties:
        id:
          type: string
          format: uuid
        sku:
          type: string
        name:
          type: string
        description:
          type: string
        price:
          type: number
          format: float
        stockQuantity:
          type: integer
        category:
          type: string
        isActive:
          type: boolean
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    CreateProductRequest:
      type: object
      required:
        - sku
        - name
        - price
      properties:
        sku:
          type: string
          minLength: 3
          maxLength: 50
        name:
          type: string
          minLength: 3
          maxLength: 255
        description:
          type: string
        price:
          type: number
          format: float
          minimum: 0
        stockQuantity:
          type: integer
          minimum: 0
        category:
          type: string

    UpdateProductRequest:
      type: object
      properties:
        name:
          type: string
        description:
          type: string
        price:
          type: number
          format: float
        stockQuantity:
          type: integer
        isActive:
          type: boolean

    Order:
      type: object
      properties:
        id:
          type: string
          format: uuid
        orderNumber:
          type: string
        userId:
          type: string
          format: uuid
        status:
          type: string
          enum: [pending, processing, shipped, delivered, cancelled]
        items:
          type: array
          items:
            $ref: '#/components/schemas/OrderItem'
        subtotal:
          type: number
        tax:
          type: number
        shipping:
          type: number
        total:
          type: number
        createdAt:
          type: string
          format: date-time

    OrderItem:
      type: object
      properties:
        productId:
          type: string
          format: uuid
        quantity:
          type: integer
        unitPrice:
          type: number
        subtotal:
          type: number

    CreateOrderRequest:
      type: object
      required:
        - items
        - shippingAddress
      properties:
        items:
          type: array
          items:
            type: object
            properties:
              productId:
                type: string
                format: uuid
              quantity:
                type: integer
                minimum: 1
        shippingAddress:
          $ref: '#/components/schemas/Address'

    User:
      type: object
      properties:
        id:
          type: string
          format: uuid
        email:
          type: string
          format: email
        username:
          type: string
        firstName:
          type: string
        lastName:
          type: string
        role:
          type: string
          enum: [user, admin, moderator]
        createdAt:
          type: string
          format: date-time

    RegisterRequest:
      type: object
      required:
        - email
        - username
        - password
      properties:
        email:
          type: string
          format: email
        username:
          type: string
          minLength: 3
          maxLength: 50
        password:
          type: string
          minLength: 8
        firstName:
          type: string
        lastName:
          type: string

    Address:
      type: object
      properties:
        street:
          type: string
        city:
          type: string
        state:
          type: string
        zipCode:
          type: string
        country:
          type: string

    Pagination:
      type: object
      properties:
        page:
          type: integer
        limit:
          type: integer
        total:
          type: integer
        totalPages:
          type: integer

    Error:
      type: object
      properties:
        message:
          type: string
        details:
          type: array
          items:
            type: object
        code:
          type: string
        timestamp:
          type: string
          format: date-time

  responses:
    BadRequest:
      description: Bad request
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    
    Unauthorized:
      description: Unauthorized
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    
    Forbidden:
      description: Forbidden
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    
    NotFound:
      description: Resource not found
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    
    Conflict:
      description: Conflict
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    
    InternalError:
      description: Internal server error
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
```

### Rate Limiting Documentation

```markdown
## Rate Limiting

All API endpoints are rate limited to ensure fair usage and system stability.

### Limits
- **Authenticated requests**: 1000 requests per hour per user
- **Unauthenticated requests**: 100 requests per hour per IP
- **Specific endpoints**:
  - POST /orders: 10 requests per minute
  - POST /users/register: 5 requests per hour per IP

### Headers
Every response includes rate limit information:
- `X-RateLimit-Limit`: Maximum requests allowed
- `X-RateLimit-Remaining`: Remaining requests in window
- `X-RateLimit-Reset`: Unix timestamp when limit resets

### Handling Rate Limits
When rate limited (HTTP 429), the response includes:
- `Retry-After` header: Seconds to wait before retrying
- Error message with retry information

Example:
```json
{
  "error": {
    "message": "Too many requests",
    "retryAfter": 60
  }
}
```
```

This comprehensive documentation provides clear setup instructions, explains key architectural decisions, defines robust database schemas with integrity constraints, implements thorough error handling, shows efficient external API integration patterns, and includes complete API documentation.
