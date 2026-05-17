CREATE SCHEMA IF NOT EXISTS orchestration;

CREATE TYPE orchestration.scheduler_jobs AS ENUM ('COLLECTION_BATCH', 'RETENTION');

CREATE TABLE IF NOT EXISTS orchestration.scheduler
(
    uuid            uuid    DEFAULT gen_random_uuid()              NOT NULL,
    job_type        orchestration.scheduler_jobs                   NOT NULL,
    next_run        timestamp with time zone                       NOT NULL,
    interval        varchar DEFAULT 'PT1H'::character varying      NOT NULL
);

ALTER TABLE orchestration.scheduler
    ADD CONSTRAINT scheduler_pk PRIMARY KEY (uuid);

ALTER TABLE orchestration.scheduler
    ADD CONSTRAINT scheduler_idx_unique_job_type UNIQUE (job_type);
