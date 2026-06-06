-- ============================================================================
-- V1 — initial schema.
--
-- Two tables live in this database:
--   1. `documents`     — owned by Flyway / JPA (created here).
--   2. `vector_store`  — owned by Spring AI's PgVectorStore. We DO NOT create
--                        it here; the starter does it on first run when the
--                        property spring.ai.vectorstore.pgvector.initialize-schema
--                        is true. Listing both ownership boundaries in one
--                        place to avoid surprise later.
--
-- The `vector` and `uuid-ossp` extensions are required up-front for either
-- table to work, so we create them here.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS documents (
    id                 uuid           PRIMARY KEY,
    title              varchar(256)   NOT NULL,
    type               varchar(32)    NOT NULL,
    source_uri         varchar(512),
    chunk_count        int            NOT NULL DEFAULT 0,
    ingested_at        timestamptz    NOT NULL,

    -- Flattened extracted metadata for cheap filtering. Optional fields, all
    -- nullable. If a field becomes high-cardinality / heavily queried,
    -- add an index in a later migration; don't index everything pre-emptively.
    vessel_name        varchar(128),
    bl_number          varchar(64),
    port_of_loading    varchar(128),
    port_of_discharge  varchar(128),
    incoterm           varchar(8),
    invoice_value      numeric(19, 4),
    currency           varchar(3),
    issue_date         date,
    shipper            varchar(256),
    consignee          varchar(256)
);

CREATE INDEX IF NOT EXISTS idx_documents_type         ON documents(type);
CREATE INDEX IF NOT EXISTS idx_documents_ingested_at  ON documents(ingested_at DESC);
CREATE INDEX IF NOT EXISTS idx_documents_incoterm     ON documents(incoterm) WHERE incoterm IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_documents_consignee    ON documents(lower(consignee));
