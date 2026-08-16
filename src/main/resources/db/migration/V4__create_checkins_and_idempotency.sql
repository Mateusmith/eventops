CREATE TABLE check_ins (
    id UUID PRIMARY KEY,
    inscricao_id UUID NOT NULL UNIQUE REFERENCES inscricoes(id) ON DELETE RESTRICT,
    credencial_id UUID NOT NULL UNIQUE REFERENCES credenciais(id) ON DELETE RESTRICT,
    evento_id UUID NOT NULL REFERENCES eventos(id) ON DELETE RESTRICT,
    operador VARCHAR(254) NOT NULL,
    realizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE requisicoes_idempotentes (
    id UUID PRIMARY KEY,
    ator VARCHAR(254) NOT NULL,
    operacao VARCHAR(80) NOT NULL,
    chave VARCHAR(120) NOT NULL,
    hash_requisicao VARCHAR(64) NOT NULL,
    codigo_resposta INTEGER NOT NULL,
    corpo_resposta JSONB NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_idempotencia UNIQUE (ator, operacao, chave)
);

CREATE INDEX idx_idempotencia_expiracao ON requisicoes_idempotentes(expira_em);
