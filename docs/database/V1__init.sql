-- Schéma initial (PostgreSQL 18). À déplacer dans backend/src/main/resources/db/migration/.
-- Conventions : snake_case, UUID v7, timestamptz, contraintes CHECK plutôt que types ENUM.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- identity
-- ---------------------------------------------------------------------------
CREATE TABLE app_user (
    id                uuid         PRIMARY KEY DEFAULT uuidv7(),
    role              varchar(16)  NOT NULL CHECK (role IN ('ANONYMOUS', 'USER', 'ADMIN')),
    email             varchar(254),
    password_hash     varchar(255),
    preferred_locale  varchar(2)   NOT NULL DEFAULT 'fr' CHECK (preferred_locale IN ('fr', 'en')),
    created_at        timestamptz  NOT NULL DEFAULT now(),
    last_activity_at  timestamptz  NOT NULL DEFAULT now(),
    version           bigint       NOT NULL DEFAULT 0,
    CONSTRAINT app_user_credentials_chk CHECK (
        (role = 'ANONYMOUS' AND email IS NULL AND password_hash IS NULL)
        OR (role <> 'ANONYMOUS' AND email IS NOT NULL AND password_hash IS NOT NULL)
    )
);
CREATE UNIQUE INDEX app_user_email_uk ON app_user (lower(email)) WHERE email IS NOT NULL;
CREATE INDEX app_user_anonymous_activity_idx ON app_user (last_activity_at) WHERE role = 'ANONYMOUS';

CREATE TABLE refresh_token (
    id              uuid         PRIMARY KEY DEFAULT uuidv7(),
    user_id         uuid         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    family_id       uuid         NOT NULL,
    token_hash      char(64)     NOT NULL UNIQUE, -- SHA-256 hexadécimal, jamais le jeton en clair
    expires_at      timestamptz  NOT NULL,
    revoked_at      timestamptz,
    replaced_by_id  uuid         REFERENCES refresh_token (id) ON DELETE SET NULL,
    created_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX refresh_token_user_idx ON refresh_token (user_id);
CREATE INDEX refresh_token_family_idx ON refresh_token (family_id);
CREATE INDEX refresh_token_expires_idx ON refresh_token (expires_at);

-- ---------------------------------------------------------------------------
-- plan
-- ---------------------------------------------------------------------------
CREATE TABLE oneshot (
    id               uuid          PRIMARY KEY DEFAULT uuidv7(),
    owner_id         uuid          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    title            varchar(200), -- NULL tant que la première génération n'est pas terminée
    language         varchar(2)    NOT NULL CHECK (language IN ('fr', 'en')),
    parameters       jsonb         NOT NULL, -- GenerationRequest (OpenAPI)
    current_version  integer,
    expires_at       timestamptz,  -- renseigné uniquement pour les one-shots anonymes
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now(),
    version          bigint        NOT NULL DEFAULT 0
);
CREATE INDEX oneshot_owner_updated_idx ON oneshot (owner_id, updated_at DESC);
CREATE INDEX oneshot_expires_idx ON oneshot (expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX oneshot_title_trgm_idx ON oneshot USING gin (lower(title) gin_trgm_ops);

CREATE TABLE oneshot_version (
    id              uuid          PRIMARY KEY DEFAULT uuidv7(),
    oneshot_id      uuid          NOT NULL REFERENCES oneshot (id) ON DELETE CASCADE,
    number          integer       NOT NULL CHECK (number >= 1),
    operation       varchar(20)   NOT NULL CHECK (operation IN ('GENERATE', 'REROLL', 'APPLY_COMMENTS', 'CHAT', 'RESTORE')),
    content         jsonb         NOT NULL, -- OneShotPlan (docs/schemas/plan.schema.json)
    seed            bigint,
    engine_version  varchar(20)   NOT NULL,
    model           varchar(100),
    summary         varchar(500),
    created_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT oneshot_version_number_uk UNIQUE (oneshot_id, number)
);

ALTER TABLE oneshot
    ADD CONSTRAINT oneshot_current_version_fk
    FOREIGN KEY (id, current_version) REFERENCES oneshot_version (oneshot_id, number)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE element_lock (
    oneshot_id  uuid         NOT NULL REFERENCES oneshot (id) ON DELETE CASCADE,
    element_id  varchar(12)  NOT NULL CHECK (element_id ~ '^(scene|enc|npc|loot|hook|aid)-[0-9]{1,3}$'),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (oneshot_id, element_id)
);

CREATE TABLE plan_comment (
    id                  uuid           PRIMARY KEY DEFAULT uuidv7(),
    oneshot_id          uuid           NOT NULL REFERENCES oneshot (id) ON DELETE CASCADE,
    element_id          varchar(12)    NOT NULL CHECK (element_id ~ '^(scene|enc|npc|loot|hook|aid)-[0-9]{1,3}$'),
    body                varchar(1000)  NOT NULL CHECK (length(trim(body)) > 0),
    status              varchar(10)    NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPLIED')),
    applied_in_version  integer,
    created_at          timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT plan_comment_applied_chk CHECK ((status = 'APPLIED') = (applied_in_version IS NOT NULL))
);
CREATE INDEX plan_comment_pending_idx ON plan_comment (oneshot_id) WHERE status = 'PENDING';

CREATE TABLE chat_message (
    id              uuid          PRIMARY KEY DEFAULT uuidv7(),
    oneshot_id      uuid          NOT NULL REFERENCES oneshot (id) ON DELETE CASCADE,
    role            varchar(10)   NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content         text          NOT NULL CHECK (length(content) BETWEEN 1 AND 10000),
    version_number  integer,      -- version produite par la réponse de l'assistant, le cas échéant
    created_at      timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX chat_message_oneshot_idx ON chat_message (oneshot_id, created_at);

-- ---------------------------------------------------------------------------
-- generation : file de traitements IA
-- ---------------------------------------------------------------------------
CREATE TABLE ai_job (
    id                uuid          PRIMARY KEY DEFAULT uuidv7(),
    oneshot_id        uuid          NOT NULL REFERENCES oneshot (id) ON DELETE CASCADE,
    owner_id          uuid          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    type              varchar(20)   NOT NULL CHECK (type IN ('GENERATE', 'REROLL', 'APPLY_COMMENTS', 'CHAT')),
    status            varchar(12)   NOT NULL DEFAULT 'QUEUED'
                                    CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    payload           jsonb         NOT NULL DEFAULT '{}'::jsonb,
    step              varchar(30),
    progress          smallint      NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    attempts          smallint      NOT NULL DEFAULT 0,
    cancel_requested  boolean       NOT NULL DEFAULT false,
    error_code        varchar(50),
    error_message     varchar(500),
    result_version    integer,
    idempotency_key   uuid,
    client_ip_hash    char(64),     -- HMAC-SHA256 de l'IP, uniquement pour les quotas anonymes
    created_at        timestamptz   NOT NULL DEFAULT now(),
    started_at        timestamptz,
    heartbeat_at      timestamptz,
    finished_at       timestamptz
);
CREATE INDEX ai_job_queue_idx ON ai_job (created_at) WHERE status = 'QUEUED';
CREATE UNIQUE INDEX ai_job_one_active_per_oneshot_uk ON ai_job (oneshot_id) WHERE status IN ('QUEUED', 'RUNNING');
CREATE UNIQUE INDEX ai_job_idempotency_uk ON ai_job (owner_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX ai_job_owner_quota_idx ON ai_job (owner_id, created_at);
CREATE INDEX ai_job_ip_quota_idx ON ai_job (client_ip_hash, created_at) WHERE client_ip_hash IS NOT NULL;

-- ---------------------------------------------------------------------------
-- library : contenu personnel
-- ---------------------------------------------------------------------------
CREATE TABLE custom_monster (
    id                uuid           PRIMARY KEY DEFAULT uuidv7(),
    owner_id          uuid           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name              varchar(100)   NOT NULL,
    description       varchar(2000)  NOT NULL,
    challenge_rating  varchar(4)     CHECK (challenge_rating ~ '^(0|1/8|1/4|1/2|[1-9]|[12][0-9]|30)$'),
    stat_block        jsonb,         -- StatBlock (plan.schema.json#/$defs/StatBlock)
    created_at        timestamptz    NOT NULL DEFAULT now(),
    updated_at        timestamptz    NOT NULL DEFAULT now(),
    version           bigint         NOT NULL DEFAULT 0
);
CREATE INDEX custom_monster_owner_idx ON custom_monster (owner_id, lower(name));

CREATE TABLE custom_npc (
    id           uuid           PRIMARY KEY DEFAULT uuidv7(),
    owner_id     uuid           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name         varchar(100)   NOT NULL,
    role         varchar(100)   NOT NULL,
    description  varchar(2000)  NOT NULL,
    motivation   varchar(600),
    stat_block   jsonb,
    created_at   timestamptz    NOT NULL DEFAULT now(),
    updated_at   timestamptz    NOT NULL DEFAULT now(),
    version      bigint         NOT NULL DEFAULT 0
);
CREATE INDEX custom_npc_owner_idx ON custom_npc (owner_id, lower(name));

CREATE TABLE random_table (
    id           uuid          PRIMARY KEY DEFAULT uuidv7(),
    owner_id     uuid          NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name         varchar(100)  NOT NULL,
    description  varchar(500),
    created_at   timestamptz   NOT NULL DEFAULT now(),
    updated_at   timestamptz   NOT NULL DEFAULT now(),
    version      bigint        NOT NULL DEFAULT 0
);
CREATE INDEX random_table_owner_idx ON random_table (owner_id, lower(name));

CREATE TABLE random_table_entry (
    id        uuid          PRIMARY KEY DEFAULT uuidv7(),
    table_id  uuid          NOT NULL REFERENCES random_table (id) ON DELETE CASCADE,
    position  smallint      NOT NULL CHECK (position >= 1),
    weight    smallint      NOT NULL CHECK (weight BETWEEN 1 AND 100),
    content   varchar(500)  NOT NULL,
    CONSTRAINT random_table_entry_position_uk UNIQUE (table_id, position)
);
