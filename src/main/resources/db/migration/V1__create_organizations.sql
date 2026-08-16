CREATE TABLE organizacoes (
    id UUID PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    documento VARCHAR(30),
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE membros_organizacao (
    id UUID PRIMARY KEY,
    organizacao_id UUID NOT NULL REFERENCES organizacoes(id) ON DELETE CASCADE,
    nome VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    email_normalizado VARCHAR(254) NOT NULL,
    papel VARCHAR(30) NOT NULL CHECK (papel IN ('PROPRIETARIO', 'GESTOR_EVENTO', 'OPERADOR_CHECKIN')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_membro_organizacao_email UNIQUE (organizacao_id, email_normalizado)
);

CREATE INDEX idx_membro_email ON membros_organizacao(email_normalizado) WHERE ativo = TRUE;
