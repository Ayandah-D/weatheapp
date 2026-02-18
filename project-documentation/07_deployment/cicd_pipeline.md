# CI/CD Pipeline Documentation

## Overview

This document describes the Continuous Integration and Continuous Deployment (CI/CD) pipeline for the Weather Data Integration Platform. The pipeline automates testing, building, and deployment processes to ensure code quality, security, and reliable deployments across different environments.

## Pipeline Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Code Commit   │───▶│   CI Pipeline   │───▶│   CD Pipeline   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  GitHub/GitLab  │    │  Build & Test   │    │  Deploy to      │
│   Repository    │    │  & Security     │    │  Environment    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## CI Pipeline (Continuous Integration)

### 1. Trigger Conditions

The CI pipeline is triggered by:

- **Push to main/develop branches**
- **Pull requests to main/develop branches**
- **Manual triggers for specific branches**
- **Scheduled runs for dependency updates**

### 2. CI Pipeline Stages

#### Stage 1: Code Quality and Security
```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      # Frontend Code Quality
      - name: Install frontend dependencies
        run: npm ci
      
      - name: Run ESLint
        run: npm run lint
      
      - name: Run TypeScript check
        run: npm run typecheck
      
      - name: Run Prettier check
        run: npm run format:check
      
      # Backend Code Quality
      - name: Run backend linting
        run: mvn checkstyle:check
      
      - name: Run security scan
        run: mvn dependency-check:check
      
      - name: SonarQube analysis
        uses: sonarqube-quality-gate@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

#### Stage 2: Unit Testing
```yaml
  unit-tests:
    runs-on: ubuntu-latest
    services:
      mongodb:
        image: mongo:7.0
        ports:
          - 27017:27017
        options: --health-cmd "mongo --eval 'db.adminCommand(\"ismaster\")'" --health-interval 10s --health-timeout 5s --health-retries 5
      
      redis:
        image: redis:7.0-alpine
        ports:
          - 6379:6379
        options: --health-cmd "redis-cli ping" --health-interval 10s --health-timeout 5s --health-retries 5
    
    steps:
      - uses: actions/checkout@v3
      
      # Frontend Unit Tests
      - name: Run frontend unit tests
        run: npm run test:unit -- --coverage
      
      # Backend Unit Tests
      - name: Run backend unit tests
        run: mvn test
      
      - name: Upload frontend coverage
        uses: codecov/codecov-action@v3
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: ./coverage/lcov.info
      
      - name: Upload backend coverage
        uses: codecov/codecov-action@v3
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: ./weather-api-backend/target/site/jacoco/jacoco.xml
```

#### Stage 3: Integration Testing
```yaml
  integration-tests:
    runs-on: ubuntu-latest
    needs: [code-quality, unit-tests]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Build Docker images
        run: |
          docker-compose -f docker-compose.test.yml build
      
      - name: Start test environment
        run: docker-compose -f docker-compose.test.yml up -d
      
      - name: Wait for services
        run: |
          sleep 30
          docker-compose -f docker-compose.test.yml exec -T backend curl -f http://localhost:8080/actuator/health
      
      - name: Run integration tests
        run: |
          npm run test:integration
          mvn test -P integration-test
      
      - name: Stop test environment
        if: always()
        run: docker-compose -f docker-compose.test.yml down
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: integration-test-results
          path: |
            test-results/
            weather-api-backend/target/surefire-reports/
```

#### Stage 4: Security Scanning
```yaml
  security-scanning:
    runs-on: ubuntu-latest
    needs: [integration-tests]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Run Trivy vulnerability scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'
      
      - name: Upload Trivy scan results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
      
      - name: Run Snyk security scan
        uses: snyk/actions/node@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
        with:
          args: --severity-threshold=high
```

#### Stage 5: Build and Package
```yaml
  build-and-package:
    runs-on: ubuntu-latest
    needs: [security-scanning]
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2
      
      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Build and push frontend image
        uses: docker/build-push-action@v4
        with:
          context: .
          file: ./Dockerfile.frontend
          push: true
          tags: |
            weatherapp/frontend:${{ github.sha }}
            weatherapp/frontend:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
      
      - name: Build and push backend image
        uses: docker/build-push-action@v4
        with:
          context: ./weather-api-backend
          file: ./Dockerfile
          push: true
          tags: |
            weatherapp/backend:${{ github.sha }}
            weatherapp/backend:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

## CD Pipeline (Continuous Deployment)

### 1. Environment Strategy

#### Environment Promotion Flow
```
Development → Staging → Production
    ↑           ↑           ↑
   Local     Automated   Manual
   Testing   Deployment  Approval
```

#### Environment Configuration
```yaml
# environments/staging/values.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: staging
data:
  SPRING_PROFILES_ACTIVE: "staging"
  LOG_LEVEL: "INFO"
  FEATURE_FLAGS: "beta-features:true,new-ui:true"
  
---
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: staging
stringData:
  JWT_SECRET: "${{ secrets.STAGING_JWT_SECRET }}"
  DATABASE_URL: "${{ secrets.STAGING_DATABASE_URL }}"
```

### 2. CD Pipeline Stages

#### Stage 1: Staging Deployment
```yaml
# .github/workflows/cd-staging.yml
name: Deploy to Staging

on:
  push:
    branches: [ develop ]

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    environment: staging
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup kubectl
        uses: azure/setup-kubectl@v3
        with:
          version: 'v1.24.0'
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1
      
      - name: Update kubeconfig
        run: aws eks update-kubeconfig --name staging-cluster
      
      - name: Deploy to staging
        run: |
          kubectl apply -f k8s/staging/namespace.yaml
          kubectl apply -f k8s/staging/configmap.yaml
          kubectl apply -f k8s/staging/secrets.yaml
          kubectl apply -f k8s/staging/mongodb.yaml
          kubectl apply -f k8s/staging/backend.yaml
          kubectl apply -f k8s/staging/frontend.yaml
          kubectl apply -f k8s/staging/ingress.yaml
      
      - name: Wait for deployment
        run: |
          kubectl rollout status deployment/backend -n staging
          kubectl rollout status deployment/frontend -n staging
      
      - name: Run smoke tests
        run: |
          npm run test:smoke -- --baseUrl=https://staging.weatherapp.com
      
      - name: Notify deployment status
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          channel: '#deployments'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
        if: always()
```

#### Stage 2: Production Deployment
```yaml
# .github/workflows/cd-production.yml
name: Deploy to Production

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to deploy'
        required: true
        default: 'latest'
      environment:
        description: 'Environment to deploy to'
        required: true
        default: 'production'

jobs:
  approval:
    runs-on: ubuntu-latest
    environment: production
    outputs:
      approved: ${{ steps.approval.outputs.approved }}
    
    steps:
      - name: Deploy Approval
        id: approval
        uses: trstringer/manual-approval@v1
        with:
          secret: ${{ secrets.MANUAL_APPROVAL_SECRET }}
          approval-timeout: 30
          reviewers: team-leads, devops-team
  
  deploy-production:
    needs: approval
    if: needs.approval.outputs.approved == 'true'
    runs-on: ubuntu-latest
    environment: production
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup kubectl
        uses: azure/setup-kubectl@v3
        with:
          version: 'v1.24.0'
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1
      
      - name: Update kubeconfig
        run: aws eks update-kubeconfig --name production-cluster
      
      - name: Backup current deployment
        run: |
          kubectl get deployment backend -n production -o yaml > backup-backend-$(date +%Y%m%d).yaml
          kubectl get deployment frontend -n production -o yaml > backup-frontend-$(date +%Y%m%d).yaml
      
      - name: Deploy with blue-green strategy
        run: |
          # Deploy new version (green)
          kubectl apply -f k8s/production/backend-green.yaml
          kubectl apply -f k8s/production/frontend-green.yaml
          
          # Wait for green deployment
          kubectl rollout status deployment/backend-green -n production
          kubectl rollout status deployment/frontend-green -n production
          
          # Run health checks
          kubectl exec -n production deployment/backend-green -- curl -f http://localhost:8080/actuator/health
          
          # Switch traffic to green
          kubectl patch service backend-service -n production -p '{"spec":{"selector":{"version":"green"}}}'
          kubectl patch service frontend-service -n production -p '{"spec":{"selector":{"version":"green"}}}'
          
          # Clean up blue deployment
          kubectl delete deployment backend-blue frontend-blue -n production
      
      - name: Run production smoke tests
        run: |
          npm run test:smoke -- --baseUrl=https://weatherapp.com
      
      - name: Monitor deployment
        run: |
          # Monitor for 10 minutes
          for i in {1..20}; do
            kubectl get pods -n production
            sleep 30
          done
      
      - name: Rollback if needed
        if: failure()
        run: |
          # Rollback to previous version
          kubectl rollout undo deployment/backend -n production
          kubectl rollout undo deployment/frontend -n production
          kubectl rollout status deployment/backend -n production
          kubectl rollout status deployment/frontend -n production
      
      - name: Notify deployment status
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          channel: '#production-deployments'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
        if: always()
```

### 3. Deployment Strategies

#### Blue-Green Deployment
```yaml
# k8s/production/blue-green.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-blue
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
      version: blue
  template:
    metadata:
      labels:
        app: backend
        version: blue
    spec:
      containers:
      - name: backend
        image: weatherapp/backend:${{ github.sha }}
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: app-config
        - secretRef:
            name: app-secrets

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-green
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
      version: green
  template:
    metadata:
      labels:
        app: backend
        version: green
    spec:
      containers:
      - name: backend
        image: weatherapp/backend:${{ github.sha }}
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: app-config
        - secretRef:
            name: app-secrets

---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: production
spec:
  selector:
    app: backend
    version: blue  # Initially point to blue
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
```

#### Canary Deployment
```yaml
# k8s/production/canary.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: backend-rollout
  namespace: production
spec:
  replicas: 10
  strategy:
    canary:
      steps:
      - setWeight: 10    # 10% traffic to new version
      - pause: {duration: 10m}  # Wait 10 minutes
      - setWeight: 50    # 50% traffic to new version
      - pause: {duration: 10m}  # Wait 10 minutes
      - setWeight: 100   # 100% traffic to new version
      trafficRouting:
        istio:
          virtualService:
            name: backend-vs
            routes:
            - primary
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: weatherapp/backend:${{ github.sha }}
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: app-config
        - secretRef:
            name: app-secrets
```

## Monitoring and Observability

### 1. Pipeline Monitoring

#### Metrics Collection
```yaml
# monitoring/pipeline-metrics.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: pipeline-metrics
  namespace: monitoring
data:
  pipeline.rules.yml: |
    groups:
    - name: pipeline.rules
      rules:
      - alert: PipelineFailure
        expr: pipeline_build_status{status="failed"} == 1
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "Pipeline failed"
          description: "Pipeline {{ $labels.pipeline }} failed"
      
      - alert: DeploymentFailure
        expr: deployment_status{status="failed"} == 1
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: "Deployment failed"
          description: "Deployment {{ $labels.deployment }} failed"
```

#### Dashboard Configuration
```json
{
  "dashboard": {
    "title": "CI/CD Pipeline Monitoring",
    "panels": [
      {
        "title": "Pipeline Success Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(pipeline_build_status{status=\"success\"}[5m]) / rate(pipeline_build_status[5m]) * 100"
          }
        ]
      },
      {
        "title": "Build Duration",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(pipeline_build_duration_seconds_bucket[5m]))"
          }
        ]
      },
      {
        "title": "Deployment Frequency",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(deployment_count[1h])"
          }
        ]
      }
    ]
  }
}
```

### 2. Application Monitoring

#### Health Checks
```yaml
# k8s/monitoring/health-checks.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: health-checks
  namespace: monitoring
data:
  health.rules.yml: |
    groups:
    - name: health.rules
      rules:
      - alert: ApplicationDown
        expr: up{job="weatherapp"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Application is down"
          description: "Application {{ $labels.instance }} is down"
      
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate"
          description: "Error rate is {{ $value }} errors per second"
      
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time"
          description: "95th percentile response time is {{ $value }} seconds"
```

## Security in CI/CD

### 1. Secrets Management
```yaml
# .github/workflows/secrets.yml
name: Secrets Management

on:
  workflow_dispatch

jobs:
  validate-secrets:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: TruffleHog OSS
        uses: trufflesecurity/trufflehog@main
        with:
          path: ./
          base: main
          head: HEAD
          extra_args: --debug --only-verified
```

### 2. Image Security Scanning
```yaml
# .github/workflows/image-security.yml
name: Image Security Scan

on:
  push:
    branches: [ main ]

jobs:
  image-security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Build image
        run: docker build -t weatherapp/backend:latest .
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'weatherapp/backend:latest'
          format: 'sarif'
          output: 'trivy-results.sarif'
      
      - name: Upload Trivy scan results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
      
      - name: Check for critical vulnerabilities
        run: |
          trivy image --exit-code 1 --severity CRITICAL weatherapp/backend:latest
```

### 3. Compliance Checks
```yaml
# .github/workflows/compliance.yml
name: Compliance Checks

on:
  push:
    branches: [ main ]

jobs:
  compliance:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run OWASP ZAP baseline scan
        uses: zaproxy/action-baseline@v0.7.0
        with:
          target: 'https://weatherapp.com'
          rules_file_name: '.zap/rules.tsv'
          cmd_options: '-a'
      
      - name: Run infrastructure compliance
        run: |
          # Check for security best practices in Kubernetes manifests
          kubesec scan k8s/production/*.yaml
```

## Rollback Strategies

### 1. Automated Rollback
```yaml
# .github/workflows/automated-rollback.yml
name: Automated Rollback

on:
  schedule:
    - cron: '*/5 * * * *'  # Check every 5 minutes

jobs:
  health-check:
    runs-on: ubuntu-latest
    steps:
      - name: Check application health
        run: |
          HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://weatherapp.com/api/health)
          if [ "$HEALTH_STATUS" != "200" ]; then
            echo "Application is unhealthy, triggering rollback"
            exit 1
          fi
      
      - name: Check error rate
        run: |
          ERROR_RATE=$(curl -s "http://prometheus:9090/api/v1/query?query=rate(http_requests_total{status=~'5..'}[5m])" | jq '.data.result[0].value[1]')
          if (( $(echo "$ERROR_RATE > 0.1" | bc -l) )); then
            echo "Error rate is too high, triggering rollback"
            exit 1
          fi
      
      - name: Trigger rollback
        if: failure()
        uses: peter-evans/repository-dispatch@v2
        with:
          token: ${{ secrets.GITHUB_TOKEN }}
          event-type: rollback-required
          client-payload: '{"reason": "health-check-failed"}'
```

### 2. Manual Rollback
```bash
#!/bin/bash
# rollback.sh

ENVIRONMENT=$1
VERSION=$2

if [ -z "$ENVIRONMENT" ] || [ -z "$VERSION" ]; then
    echo "Usage: $0 <environment> <version>"
    exit 1
fi

echo "Rolling back $ENVIRONMENT to version $VERSION"

# Rollback backend
kubectl set image deployment/backend backend=weatherapp/backend:$VERSION -n $ENVIRONMENT

# Rollback frontend
kubectl set image deployment/frontend frontend=weatherapp/frontend:$VERSION -n $ENVIRONMENT

# Wait for rollout
kubectl rollout status deployment/backend -n $ENVIRONMENT
kubectl rollout status deployment/frontend -n $ENVIRONMENT

# Verify rollback
kubectl get pods -n $ENVIRONMENT

echo "Rollback completed"
```

## Performance Optimization

### 1. Pipeline Caching
```yaml
# .github/workflows/caching.yml
name: Optimized Build

on:
  push:
    branches: [ main ]

jobs:
  optimized-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-m2-
      
      - name: Cache Node modules
        uses: actions/cache@v3
        with:
          path: ~/.npm
          key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
          restore-keys: |
            ${{ runner.os }}-node-
      
      - name: Build with cache
        run: |
          mvn clean compile
          npm install
          npm run build
```

### 2. Parallel Execution
```yaml
# .github/workflows/parallel.yml
name: Parallel Pipeline

on:
  push:
    branches: [ main ]

jobs:
  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - run: npm test
      - run: npm run build

  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - run: mvn test
      - run: mvn package

  security-scan:
    runs-on: ubuntu-latest
    needs: [frontend-tests, backend-tests]
    steps:
      - uses: actions/checkout@v3
      - run: docker build -t app .
      - run: trivy image app
```

This comprehensive CI/CD pipeline ensures that the Weather Data Integration Platform maintains high code quality, security standards, and reliable deployments across all environments while providing comprehensive monitoring and rollback capabilities.