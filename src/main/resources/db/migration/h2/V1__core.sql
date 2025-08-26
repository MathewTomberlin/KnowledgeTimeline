-- Core schema for Knowledge-Aware LLM Middleware
-- H2 database for testing

-- Tenants table
CREATE TABLE tenants (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- API Keys table
CREATE TABLE api_keys (
    id VARCHAR(36) PRIMARY KEY,
    key_hash VARCHAR(255) UNIQUE NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

-- Knowledge Objects table
CREATE TABLE knowledge_objects (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    session_id VARCHAR(255),
    user_id VARCHAR(255),
    parent_id VARCHAR(36),
    tags CLOB, -- JSON array of tags
    metadata CLOB, -- JSON object
    archived BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    original_tokens INTEGER,
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

-- Content Variants table
CREATE TABLE content_variants (
    id VARCHAR(36) PRIMARY KEY,
    knowledge_object_id VARCHAR(36) NOT NULL,
    variant VARCHAR(50) NOT NULL,
    content CLOB, -- nullable if stored in blob
    tokens INTEGER,
    embedding_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    storage_uri VARCHAR(500), -- for blob storage
    FOREIGN KEY (knowledge_object_id) REFERENCES knowledge_objects(id)
);

-- Knowledge Relationships table
CREATE TABLE knowledge_relationships (
    id VARCHAR(36) PRIMARY KEY,
    source_id VARCHAR(36) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL,
    confidence DOUBLE,
    evidence CLOB, -- JSON object
    detected_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Dialogue States table
CREATE TABLE dialogue_states (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    summary_short CLOB, -- ≤250 tokens
    summary_bullets CLOB, -- ≤120 tokens
    topics CLOB, -- JSON array of topics
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cumulative_tokens INTEGER,
    turn_count INTEGER,
    metadata CLOB, -- JSON object
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

-- Usage Logs table
CREATE TABLE usage_logs (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    request_id VARCHAR(255),
    knowledge_tokens_used INTEGER,
    llm_input_tokens INTEGER,
    llm_output_tokens INTEGER,
    cost_estimate DECIMAL(10,6),
    model VARCHAR(100),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

-- Embeddings table for vector storage (simplified for H2)
CREATE TABLE embeddings (
    id VARCHAR(36) PRIMARY KEY,
    variant_id VARCHAR(36) NOT NULL,
    text_snippet CLOB NOT NULL,
    embedding_vector CLOB, -- Store as JSON array for H2
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (variant_id) REFERENCES content_variants(id)
);

-- Indexes
CREATE INDEX idx_knowledge_objects_tenant_type ON knowledge_objects(tenant_id, type);
CREATE INDEX idx_knowledge_objects_session ON knowledge_objects(tenant_id, session_id);
CREATE INDEX idx_knowledge_objects_user ON knowledge_objects(tenant_id, user_id);
CREATE INDEX idx_knowledge_objects_created ON knowledge_objects(created_at);
CREATE INDEX idx_knowledge_objects_archived ON knowledge_objects(archived);

CREATE INDEX idx_content_variants_knowledge_object ON content_variants(knowledge_object_id);
CREATE INDEX idx_content_variants_variant ON content_variants(variant);
CREATE INDEX idx_content_variants_embedding ON content_variants(embedding_id);

CREATE INDEX idx_dialogue_states_tenant_session ON dialogue_states(tenant_id, session_id);
CREATE INDEX idx_dialogue_states_user ON dialogue_states(tenant_id, user_id);
CREATE INDEX idx_dialogue_states_updated ON dialogue_states(last_updated_at);

CREATE INDEX idx_usage_logs_tenant ON usage_logs(tenant_id);
CREATE INDEX idx_usage_logs_user ON usage_logs(tenant_id, user_id);
CREATE INDEX idx_usage_logs_session ON usage_logs(tenant_id, session_id);
CREATE INDEX idx_usage_logs_timestamp ON usage_logs(timestamp);

CREATE INDEX idx_embeddings_variant ON embeddings(variant_id);

-- Composite indexes for common queries
CREATE INDEX idx_knowledge_objects_tenant_type_archived ON knowledge_objects(tenant_id, type, archived);
CREATE INDEX idx_content_variants_knowledge_variant ON content_variants(knowledge_object_id, variant);
