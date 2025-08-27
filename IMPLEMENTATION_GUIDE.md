Knowledge-Aware LLM Middleware: End-to-End Implementation Guide
Goal: Local-first development with seamless deployment to Oracle Cloud (Autonomous Database + Object Storage) and Google Cloud (Cloud Run + Memorystore + Cloud Scheduler).
1. Goals and Guardrails
    • Local-first: All services run via Docker Compose on your laptop; no cloud accounts required during development.
    • Cloud-ready: Same code and container image deploy to Cloud Run, using Oracle ADB + OCI Object Storage, configurable via Spring profiles and environment variables.
    • Token-efficient context: Summaries-first; only small micro-quotes when necessary. Hard token budgets enforced. Exact token accounting per request.
    • OpenAI-compatible: Drop-in proxy supporting existing SDKs, including streaming.
    • Multi-tenant, observable, rate-limited, and billable.
2. Architecture and Interfaces
API:
    • /v1/chat/completions (OpenAI-compatible)
    • Optional embeddings endpoint
    • Admin/job endpoints (/jobs/...)
Services / Interfaces:
    • VectorStoreService: Semantic search & storage (PostgresPgvector locally, Oracle Vector Search in prod)
    • MetadataStore: JPA repositories (DB-portable schema)
    • BlobStorageService: Externalize large content (LocalDisk, OCI Object Storage; optional GCS)
    • EmbeddingService: Local container in dev; remote/co-located in prod
    • LLMClientService: Pluggable adapters (OpenAI, Ollama, vLLM)
    • TokenCountingService: Tokenization per model
    • ContextBuilderService: Retrieval + packing (MMR/novelty) with hard token budgets
    • MemoryExtractionService: Structured JSON facts/entities/tasks
    • UsageTrackingService: Track tokens, costs per request
    • Background work: HTTP-triggered jobs; local profile may use @Scheduled
3. Repository Layout
knowledge-middleware/
├─ src/main/java/middleware/
│  ├─ api/           # OpenAI proxy, job endpoints, health checks
│  ├─ service/       # context, memory, billing, packing, caching
│  ├─ provider/      # LLM and embedding adapters
│  ├─ vector/        # PostgresPgvectorAdapter, OracleVectorAdapter
│  ├─ storage/       # LocalDiskBlobStorage, OCIObjectStorage, optional GCS
│  ├─ model/         # Entities + DTOs
│  ├─ repository/    # Spring Data
│  └─ config/        # profiles, wallet loader, metrics
├─ docker/            # docker-compose.yml, Postgres init, embeddings config
├─ deploy/            # k8s (optional), Cloud Run deploy scripts
└─ testdata/          # seed knowledge, golden tests
4. Dependencies
    • Database: Oracle JDBC Thin + UCP (Universal Connection Pool)
    • Tokenization: jtokkit or equivalent
    • Observability: Micrometer + Prometheus, OTEL exporter
    • Rate limiting: Bucket4j
    • Resilience: Resilience4j (retries, circuit breakers)
    • Migrations: Flyway (with vendor-specific folders) or Liquibase
5. Spring Profiles and Configuration
Profiles:
    • local: Postgres + pgvector, Redis, Local embeddings, LocalDisk blob storage
    • gcp/production: Oracle ADB, Memorystore Redis, OCI Object Storage, Cloud Run
    • Optional oci: for OCI worker nodes
Environment Variables:
# Database
DB_URL, DB_USER, DB_PASSWORD, ORACLE_WALLET_B64, ORACLE_TNS_NAME

# Redis
REDIS_HOST, REDIS_PORT (or fallback to Caffeine)

# Embeddings
EMBED_URL, EMBED_API_KEY

# LLM
OPENAI_API_KEY or LLM_BASE_URL/LLM_MODEL

# Blob storage (OCI S3-compatible)
OCI_S3_ENDPOINT, OCI_S3_NAMESPACE, OCI_S3_REGION, OCI_S3_ACCESS_KEY, OCI_S3_SECRET_KEY, OCI_S3_BUCKET

# General
SPRING_PROFILES_ACTIVE, PRICING_TABLE_JSON (optional), RATE_LIMITS_JSON
6. Local Development Stack (Docker Compose)
    • middleware: exposes 8080, SPRING_PROFILES_ACTIVE=local
    • postgres: pgvector/pgvector:pg15, mounts init.sql
    • redis: redis:7-alpine
    • embeddings: ollama/ollama:latest (nomic-embed-text model)
    • Optional: Ollama or vLLM for local LLM testing
Network: default bridge; .env file for non-secret defaults.
7. Data Model (Summary-First, Multi-Tenant)
Entities (key fields):
    • KnowledgeObject: id, tenantId, type (TURN, FILE_CHUNK, SUMMARY, EXTRACTED_FACT, SESSION_MEMORY), sessionId, userId, parentId, tags, metadata, archived, createdAt, originalTokens
    • ContentVariant: id, knowledgeObjectId, variant (RAW, SHORT, MEDIUM, BULLET_FACTS), content (nullable if stored in blob), tokens, embeddingId, createdAt, storageUri
    • KnowledgeRelationship: id, sourceId, targetId, type, confidence, evidence, detectedBy, createdAt
    • DialogueState: id, tenantId, sessionId, userId, summaryShort, summaryBullets, topics, lastUpdatedAt, cumulativeTokens
    • UsageLog: tenantId, userId, sessionId, requestId, knowledgeTokensUsed, llmInputTokens, llmOutputTokens, costEstimate, model, timestamp
    • Tenant, ApiKey: authentication, plan, quotas
8. Database and Migrations
Flyway layout:
src/main/resources/db/migration/postgres/V1__core.sql
src/main/resources/db/migration/postgres/V2__indexes.sql
src/main/resources/db/migration/oracle/V1__core.sql
src/main/resources/db/migration/oracle/V2__indexes.sql
    • Use TEXT for JSON payloads; add functional indexes per vendor.
    • Separate embeddings table referencing ContentVariant.id.
    • Postgres: pgvector + ivfflat index; Oracle: VECTOR column + ANN index.
    • Keep identical logical schema across vendors.
Sample DDL (Postgres):
embeddings(
  id uuid pk,
  variant_id uuid fk,
  text_snippet text,
  embedding vector(384)
)
CREATE INDEX embeddings_ivfflat ON embeddings USING ivfflat (embedding) WITH (lists=100);
Oracle equivalent uses CLOB and VECTOR datatype.
9. VectorStoreService Adapters
Interface:
    • storeEmbedding(objectId, variantId, text) -> embeddingId
    • findSimilar(queryText, k, filters, withMMR=true, diversity=0.3) -> List<SimilarityMatch>
    • deleteEmbedding(embeddingId)
Local (PostgresPgvectorAdapter): Embeds via EmbeddingService, inserts/UPSERTs, cosine similarity.
Prod (OracleVectorAdapter): Uses VECTOR column and ANN index; MMR and recency penalties applied in app layer.
Scoring: combined = α*cosine + β*recencyBoost + γ*sourceTrust − δ*redundancy
10. BlobStorageService
    • LocalDiskBlobStorage: file://./data/blobs/{tenant}/{id}
    • OCIObjectStorage: AWS S3 SDK with custom endpoint, namespace, bucket from env
    • Optional GCS for specific assets
    • Store large RAW content; ContentVariant.content may be null if storageUri is set
11. EmbeddingService
    • LocalEmbeddings: HTTP to local embeddings container
    • RemoteEmbeddings: same API on Cloud Run (or OCI/Vertex AI later)
    • Maintain consistent vector dimensionality across environments
12. LLMClientService and Proxy Endpoints
Endpoints:
    • POST /v1/chat/completions (supports streaming)
    • GET /v1/models
    • POST /v1/embeddings (optional passthrough)
    • GET /health
    • Admin/jobs: /jobs/relationship-discovery, /jobs/session-summarize
Request Flow:
    1. Authenticate API key → resolve tenant → rate-limit (Bucket4j)
    2. Build EnhancedContext via ContextBuilderService (summaries-first)
    3. Inject compact “context facts” as system message
    4. Call LLM adapter → stream or return response
    5. Async enqueue memory extraction job
    6. Log usage with exact token counts; map bullets → source objectId
Streaming: SSE; flush per chunk; compute output tokens at end
13. Context Building and Token Policy
    • Hard knowledge token budget (default 2000)
    • Inputs: DialogueState.summaryBullets, retrieval candidates (SHORT/BULLET_FACTS), optional micro-quotes (≤120 tokens)
    • Selection: query = user prompt + dialogue topics
    • Retrieve k=40 filtered by tenant, tags, types, recency
    • Cluster similar candidates; prefer BULLET_FACTS/SHORT variants
    • MMR packing maximizes utility per token
    • Output: small bullet list with [src:objectId] provenance
    • TokenCountingService ensures precise knowledge token count
14. Summarization and DialogueState Maintenance
    • After each exchange, update DialogueState:
        ◦ summaryShort ≤250 tokens, summaryBullets ≤120 tokens
        ◦ Update topics (entities/keywords)
        ◦ Every N turns or 3000 tokens → create SESSION_MEMORY object, link parents
15. Memory Extraction and Relationships
    • MemoryExtractionService: returns JSON {facts[], entities[], tasks[]}
    • Validate, deduplicate, persist as EXTRACTED_FACT
    • RelationshipDiscovery (job endpoint): SUPPORTS/REFERENCES via similarity, CONTRADICTS classifier
    • Store evidence and confidence
16. Caching
    • Redis keys:
ctx:{tenant}:{session}:{hash(promptNorm)} → context bundle (TTL 30–60 min)
ds:{session} → DialogueState cache
sum:{clusterKey} → cluster summaries
    • Fallback: Caffeine in-memory cache if Redis unavailable (local profile)
17. Auth, Multi-Tenancy, Billing, Quotas
    • ApiKey: hashed secret, tenantId, plan (FREE/SUBSCRIPTION/TOKEN_BILLED), limits
    • Enforce tenantId in DB queries
    • Rate-limit via Bucket4j per tenant/key
    • UsageTrackingService: tracks knowledgeTokens, LLM input/output tokens, costEstimate
    • Expose per-tenant usage endpoint (admin only)
18. Observability
    • Micrometer + Prometheus; OTEL traces
    • Metrics: context_tokens_injected, llm_tokens_in/out, retrieval_latency_ms, db_latency_ms, cache_hit_rate, p95_latency, contradictions_detected
    • Logging: requestId, tenantId, sessionId, vectorScoreStats, selectedObjectIds
    • Health endpoint: DB, vector store, cache, embeddings, LLM provider
19. Background Jobs
    • Cloud Run: POST /jobs/* endpoints (idempotent)
    • Local: @Scheduled may trigger jobs
    • GCP: Cloud Scheduler → HTTPS/ Pub/Sub → handler
    • Long-running batches: Cloud Run Jobs
20. Security and Privacy
    • Store secrets as env vars via secret managers
    • Cloud Run → ADB: mutual TLS using wallet
    • Encrypt at rest (ADB + Object Storage); optional client-side encryption
    • Scrub prompts to limit PII if required
21. CI/CD
    • Build: Jib or Buildpacks (single immutable image)
    • Test stages: Unit → Integration → Optional Oracle integration
    • Push to Artifact Registry → Deploy to Cloud Run
    • Cloud Run settings: min instances 0–1, concurrency 50–80, CPU allocated for streaming
22. Cloud Deployment Steps (Once, Then Automated)
Oracle ADB: create DB, user/schema, wallet, IP allowlist, run Flyway migrations
OCI Object Storage: create bucket, user/API key, store secrets, set S3 endpoint
GCP: enable APIs, Artifact Registry, Cloud Run, Secret Manager, Memorystore, VPC, Cloud Scheduler jobs
Deploy: gcloud run deploy knowledge-middleware ... with env vars and secrets
23. Oracle Wallet Loader (GCP)
    • Decode ORACLE_WALLET_B64 → /tmp/wallet
    • Set system properties: oracle.net.tns_admin=/tmp/wallet, oracle.net.ssl_server_dn_match=true
    • Configure UCP: minPoolSize=0, maxPoolSize=10–20, inactiveConnectionTimeout=60
    • Validate connectivity on /health
24. Local-to-Prod Parity
    • application-local.yml: Postgres JDBC, Redis localhost, Local embeddings, LocalDisk blob storage
    • application-gcp.yml: Oracle JDBC, Memorystore, OCI blob storage
    • application-production.yml: shared production flags (timeouts, retries, CB thresholds)
    • Never commit real secrets; use .env.example
25. Testing Strategy
    • Golden tests for ContextBuilder: enforce 0 raw-turn injection unless micro-quote triggers; verify knowledgeTokens ≤ budget; check provenance/diversity/novelty penalties
    • Integration tests with Docker Compose (retrieval, embeddings, Redis, OpenAI proxy)
    • Oracle integration tests: dev ADB, similarity queries, migrations
    • Load tests: p95 < 2s with cache warm and local LLM; profile retrieval + provider latency
26. Operational Defaults and Knobs
    • Knowledge token budget: 2000 (configurable per tenant)
    • Retrieval k: 40; pack 6–12 items typical
    • MMR diversity λ: 0.3
    • Recency decay λ: 0.03 (~30-day half-life)
    • Micro-quote cap: 120 tokens
    • Session summarize every 10 turns or 3000 tokens
    • Rate limits: default 60 req/min/key, burst 120
    • Pricing: knowledge $0.001/1K tokens, LLM pricing by model
27. Rollout, Migration, Rollback
    • Flyway target version for safe schema changes; use Vx__backfill for large operations
    • Blue/green deploy on Cloud Run
    • Keep ContentVariant populated for SHORT/BULLET_FACTS; RAW may be blob-only
    • Backfill embeddings via background jobs
28. Day-2 Operations
    • Dashboards: context token %, cache hit rate, ADB query latency, Cloud Run concurrency, error rate
    • Alarms: 5xx spikes, time_to_first_token >1s, ADB p95>150ms, Redis miss rate high
    • Cost guardrails: disable context enrichment if ADB or provider degraded; flag degraded mode
Quick Start (Local-first)
    1. Copy project; add provider/, vector/, storage/ modules; add ContentVariant, DialogueState entities
    2. Implement: LocalDiskBlobStorage, PostgresPgvectorAdapter, LocalEmbeddings, OpenAIAdapter, TokenCountingService
    3. Wire OpenAI-compatible /v1/chat/completions with streaming
    4. Implement ContextBuilder: summaries-first, MMR packing
    5. Run docker compose up; point OpenAI SDK to http://localhost:8080/v1
    6. Add OracleVectorAdapter, OCIObjectStorage behind profiles
    7. Create GCP infra: Artifact Registry, Cloud Run, Memorystore, VPC + NAT, Secret Manager
    8. Create ADB + bucket, load wallet secret
    9. Deploy with SPRING_PROFILES_ACTIVE=gcp,production
