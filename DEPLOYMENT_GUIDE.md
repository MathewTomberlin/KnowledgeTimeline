# Deployment Guide - Knowledge-Aware LLM Middleware

## Overview
This guide provides comprehensive deployment instructions for the Knowledge-Aware LLM Middleware across different environments, from local development to production.

## Prerequisites

### System Requirements
- **Java**: OpenJDK 17 or later
- **Maven**: 3.8+ for building
- **Docker**: 20.10+ for containerized deployment
- **Docker Compose**: 2.0+ for local development
- **PostgreSQL**: 14+ with pgvector extension
- **Redis**: 6.0+ for caching
- **Memory**: Minimum 4GB RAM, 8GB+ recommended
- **Storage**: 20GB+ available disk space

### Required Services
- **LLM Service**: OpenAI API, Ollama, or compatible service
- **Embedding Service**: OpenAI Embeddings, Ollama, or compatible service
- **Vector Database**: PostgreSQL with pgvector or Oracle Vector
- **Object Storage**: AWS S3, Google Cloud Storage, or OCI Object Storage

## Local Development Deployment

### 1. Docker Compose Setup

#### Start All Services
```bash
# Clone the repository
git clone https://github.com/MathewTomberlin/KnowledgeTimeline.git
cd KnowledgeTimeline

# Start all services
docker-compose up -d

# Check service status
docker-compose ps
```

#### Verify Services
```bash
# Check PostgreSQL
docker exec knowledge-postgres pg_isready -U postgres -d knowledge_middleware

# Check Redis
docker exec knowledge-redis redis-cli ping

# Check Embeddings service
curl -f http://localhost:11434/api/embeddings

# Check Ollama (if enabled)
curl -f http://localhost:11434/api/tags
```

#### Application Health Check
```bash
# Wait for application to start (30-60 seconds)
sleep 60

# Check application health
curl -f http://localhost:8080/actuator/health

# Check API endpoints
curl -X GET http://localhost:8080/v1/models
```

### 2. Manual Setup (Alternative)

#### Database Setup
```bash
# Install PostgreSQL with pgvector
# Ubuntu/Debian
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo apt install postgresql-14-pgvector

# macOS
brew install postgresql
brew install pgvector

# Create database and user
sudo -u postgres psql
CREATE DATABASE knowledge_middleware;
CREATE USER knowledge_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE knowledge_middleware TO knowledge_user;
\c knowledge_middleware
CREATE EXTENSION IF NOT EXISTS vector;
\q
```

#### Redis Setup
```bash
# Ubuntu/Debian
sudo apt install redis-server

# macOS
brew install redis

# Start Redis
sudo systemctl start redis-server
# or
brew services start redis
```

#### Application Setup
```bash
# Build the application
./mvnw clean package

# Run with local profile
java -jar target/knowledge-middleware-1.0.0.jar --spring.profiles.active=local
```

## Docker Deployment

### 1. Single Container Deployment

#### Build Docker Image
```bash
# Build image
docker build -t knowledge-middleware:latest .

# Run container
docker run -d \
  --name knowledge-middleware \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/knowledge_middleware \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -e SPRING_REDIS_HOST=host.docker.internal \
  -e SPRING_REDIS_PORT=6379 \
  knowledge-middleware:latest
```

#### Docker Compose for Production
```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  middleware:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/knowledge_middleware
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - LLM_SERVICE_URL=${LLM_SERVICE_URL}
      - EMBEDDING_SERVICE_URL=${EMBEDDING_SERVICE_URL}
    depends_on:
      - postgres
      - redis
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: pgvector/pgvector:pg14
    environment:
      - POSTGRES_DB=knowledge_middleware
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
```

### 2. Multi-Container Orchestration

#### Docker Swarm
```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.prod.yml knowledge-middleware

# Check services
docker service ls
docker service logs knowledge-middleware_middleware
```

#### Kubernetes Deployment
```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: knowledge-middleware
spec:
  replicas: 3
  selector:
    matchLabels:
      app: knowledge-middleware
  template:
    metadata:
      labels:
        app: knowledge-middleware
    spec:
      containers:
      - name: middleware
        image: knowledge-middleware:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: knowledge-middleware-service
spec:
  selector:
    app: knowledge-middleware
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

## Cloud Deployment

### 1. Google Cloud Platform (GCP)

#### Prerequisites
```bash
# Install Google Cloud CLI
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init

# Enable required APIs
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable sqladmin.googleapis.com
gcloud services enable redis.googleapis.com
```

#### Cloud SQL Setup
```bash
# Create PostgreSQL instance
gcloud sql instances create knowledge-middleware-db \
  --database-version=POSTGRES_14 \
  --tier=db-f1-micro \
  --region=us-central1 \
  --storage-type=SSD \
  --storage-size=10GB

# Create database
gcloud sql databases create knowledge_middleware \
  --instance=knowledge-middleware-db

# Create user
gcloud sql users create knowledge_user \
  --instance=knowledge-middleware-db \
  --password=your_secure_password

# Enable pgvector extension
gcloud sql connect knowledge-middleware-db --user=postgres
CREATE EXTENSION IF NOT EXISTS vector;
\q
```

#### Cloud Run Deployment
```bash
# Build and push image
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/knowledge-middleware

# Deploy to Cloud Run
gcloud run deploy knowledge-middleware \
  --image gcr.io/YOUR_PROJECT_ID/knowledge-middleware \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=gcp \
  --set-env-vars SPRING_DATASOURCE_URL=jdbc:postgresql:///knowledge_middleware?cloudSqlInstance=YOUR_PROJECT_ID:us-central1:knowledge-middleware-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory \
  --set-env-vars SPRING_DATASOURCE_USERNAME=knowledge_user \
  --set-env-vars SPRING_DATASOURCE_PASSWORD=your_secure_password
```

### 2. AWS Deployment

#### ECS Fargate Setup
```json
// task-definition.json
{
  "family": "knowledge-middleware",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::YOUR_ACCOUNT:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "middleware",
      "image": "YOUR_ACCOUNT.dkr.ecr.us-east-1.amazonaws.com/knowledge-middleware:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "aws"
        },
        {
          "name": "SPRING_DATASOURCE_URL",
          "value": "jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/knowledge_middleware"
        }
      ],
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:YOUR_ACCOUNT:secret:db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/knowledge-middleware",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

#### RDS Setup
```bash
# Create RDS instance
aws rds create-db-instance \
  --db-instance-identifier knowledge-middleware-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 14.7 \
  --master-username postgres \
  --master-user-password your_secure_password \
  --allocated-storage 20 \
  --storage-type gp2 \
  --vpc-security-group-ids sg-xxxxxxxxx \
  --db-subnet-group-name your-subnet-group

# Enable pgvector extension
aws rds modify-db-instance \
  --db-instance-identifier knowledge-middleware-db \
  --db-parameter-group-name custom-postgres14-pgvector
```

### 3. Oracle Cloud Infrastructure (OCI)

#### Container Instance Deployment
```bash
# Build and push to OCI Registry
docker build -t iad.ocir.io/YOUR_TENANCY/knowledge-middleware:latest .
docker push iad.ocir.io/YOUR_TENANCY/knowledge-middleware:latest

# Create container instance
oci container-instances container-instance create \
  --compartment-id ocid1.compartment.oc1..example \
  --display-name knowledge-middleware \
  --containers '[{
    "displayName": "middleware",
    "imageUrl": "iad.ocir.io/YOUR_TENANCY/knowledge-middleware:latest",
    "environmentVariables": {
      "SPRING_PROFILES_ACTIVE": "oci",
      "SPRING_DATASOURCE_URL": "jdbc:postgresql://YOUR_DB_ENDPOINT:5432/knowledge_middleware"
    },
    "ports": [{
      "port": 8080,
      "protocol": "TCP"
    }]
  }]' \
  --shape CI.Standard.E4.Flex \
  --shape-config '{"ocpus": 1, "memoryInGBs": 8}'
```

## Configuration Management

### Environment-Specific Configuration

#### Local Development
```yaml
# application-local.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/knowledge_middleware
    username: postgres
    password: postgres
  redis:
    host: localhost
    port: 6379

llm:
  service:
    url: http://localhost:11434
    type: ollama
  embedding:
    service:
      url: http://localhost:11434
      type: ollama

logging:
  level:
    middleware: DEBUG
    org.springframework.security: DEBUG
```

#### Docker Environment
```yaml
# application-docker.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/knowledge_middleware
    username: postgres
    password: ${DB_PASSWORD}
  redis:
    host: redis
    port: 6379

llm:
  service:
    url: ${LLM_SERVICE_URL}
    type: ${LLM_SERVICE_TYPE}
  embedding:
    service:
      url: ${EMBEDDING_SERVICE_URL}
      type: ${EMBEDDING_SERVICE_TYPE}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### Production Environment
```yaml
# application-production.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/knowledge_middleware
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  redis:
    host: ${REDIS_HOST}
    port: 6379
    password: ${REDIS_PASSWORD}
    timeout: 5000ms

llm:
  service:
    url: ${LLM_SERVICE_URL}
    type: ${LLM_SERVICE_TYPE}
    api-key: ${LLM_API_KEY}
  embedding:
    service:
      url: ${EMBEDDING_SERVICE_URL}
      type: ${EMBEDDING_SERVICE_TYPE}
      api-key: ${EMBEDDING_API_KEY}

blob:
  storage:
    type: ${BLOB_STORAGE_TYPE}
    bucket: ${BLOB_STORAGE_BUCKET}
    region: ${BLOB_STORAGE_REGION}

logging:
  level:
    middleware: INFO
    org.springframework.security: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/knowledge-middleware.log
    max-size: 100MB
    max-history: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

### Environment Variables

#### Required Variables
```bash
# Database
DB_HOST=your-db-host
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password

# Redis
REDIS_HOST=your-redis-host
REDIS_PASSWORD=your-redis-password

# LLM Services
LLM_SERVICE_URL=https://api.openai.com/v1
LLM_SERVICE_TYPE=openai
LLM_API_KEY=your-openai-api-key

# Embedding Services
EMBEDDING_SERVICE_URL=https://api.openai.com/v1
EMBEDDING_SERVICE_TYPE=openai
EMBEDDING_API_KEY=your-openai-api-key

# Blob Storage
BLOB_STORAGE_TYPE=s3
BLOB_STORAGE_BUCKET=your-bucket-name
BLOB_STORAGE_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
```

## Monitoring and Observability

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health check
curl http://localhost:8080/actuator/health -H "Authorization: Bearer your-api-key"

# Custom health check
curl http://localhost:8080/jobs/health
```

### Metrics and Monitoring
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Application info
curl http://localhost:8080/actuator/info

# Custom metrics
curl http://localhost:8080/actuator/metrics/knowledge.objects.count
```

### Logging
```bash
# View application logs
docker logs knowledge-middleware

# Follow logs
docker logs -f knowledge-middleware

# View specific log level
docker logs knowledge-middleware | grep "ERROR"
```

## Security Configuration

### SSL/TLS Setup
```yaml
# application-production.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: knowledge-middleware
  port: 8443
```

### API Key Management
```bash
# Generate secure API key
openssl rand -hex 32

# Store in secure location
echo "your-generated-api-key" | base64

# Use in application
API_KEY_HASH=$(echo -n "your-generated-api-key" | sha256sum | cut -d' ' -f1)
```

### Network Security
```bash
# Firewall rules (example for UFW)
sudo ufw allow 8080/tcp
sudo ufw allow 5432/tcp
sudo ufw allow 6379/tcp

# Docker network security
docker network create --driver bridge --subnet=172.20.0.0/16 knowledge-network
```

## Backup and Recovery

### Database Backup
```bash
# PostgreSQL backup
pg_dump -h localhost -U postgres -d knowledge_middleware > backup_$(date +%Y%m%d_%H%M%S).sql

# Automated backup script
#!/bin/bash
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump -h $DB_HOST -U $DB_USERNAME -d knowledge_middleware > $BACKUP_DIR/backup_$DATE.sql
gzip $BACKUP_DIR/backup_$DATE.sql
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +7 -delete
```

### Application Backup
```bash
# Configuration backup
tar -czf config_backup_$(date +%Y%m%d).tar.gz \
  src/main/resources/application*.yml \
  docker-compose*.yml \
  Dockerfile

# Docker image backup
docker save knowledge-middleware:latest | gzip > knowledge-middleware_$(date +%Y%m%d).tar.gz
```

## Troubleshooting

### Common Issues

#### Application Won't Start
```bash
# Check logs
docker logs knowledge-middleware

# Check database connection
docker exec knowledge-middleware nc -zv postgres 5432

# Check Redis connection
docker exec knowledge-middleware nc -zv redis 6379

# Check environment variables
docker exec knowledge-middleware env | grep SPRING
```

#### Database Connection Issues
```bash
# Test database connectivity
psql -h localhost -U postgres -d knowledge_middleware -c "SELECT 1;"

# Check pgvector extension
psql -h localhost -U postgres -d knowledge_middleware -c "SELECT * FROM pg_extension WHERE extname = 'vector';"

# Check database permissions
psql -h localhost -U postgres -d knowledge_middleware -c "\du"
```

#### Performance Issues
```bash
# Check memory usage
docker stats knowledge-middleware

# Check database performance
psql -h localhost -U postgres -d knowledge_middleware -c "SELECT * FROM pg_stat_activity;"

# Check Redis performance
redis-cli info memory
redis-cli info stats
```

### Debug Mode
```bash
# Enable debug logging
docker run -e LOGGING_LEVEL_MIDDLEWARE=DEBUG knowledge-middleware:latest

# Enable SQL logging
docker run -e SPRING_JPA_SHOW_SQL=true knowledge-middleware:latest

# Enable security debug
docker run -e LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG knowledge-middleware:latest
```

## Scaling

### Horizontal Scaling
```bash
# Docker Swarm scaling
docker service scale knowledge-middleware_middleware=3

# Kubernetes scaling
kubectl scale deployment knowledge-middleware --replicas=5

# Load balancer configuration
# Configure nginx or cloud load balancer to distribute traffic
```

### Vertical Scaling
```bash
# Increase container resources
docker run -m 4g -c 2 knowledge-middleware:latest

# Database scaling
# Upgrade RDS instance class or add read replicas

# Redis scaling
# Use Redis Cluster or Redis Sentinel for high availability
```

This deployment guide should be updated as the project evolves and new deployment patterns emerge.
