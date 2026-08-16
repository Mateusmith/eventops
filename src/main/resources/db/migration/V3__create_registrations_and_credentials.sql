CREATE TABLE inscricoes (
    id UUID PRIMARY KEY,
    evento_id UUID NOT NULL REFERENCES eventos(id) ON DELETE RESTRICT,
    nome VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    email_normalizado VARCHAR(254) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('CONFIRMADA', 'LISTA_ESPERA', 'CANCELADA')),
    origem VARCHAR(20) NOT NULL CHECK (origem IN ('DIRETA', 'CONVITE', 'INDICACAO')),
    indicador_inscricao_id UUID REFERENCES inscricoes(id) ON DELETE SET NULL,
    codigo_indicacao VARCHAR(24) NOT NULL UNIQUE,
    token_cancelamento_hash VARCHAR(64) NOT NULL UNIQUE,
    confirmada_em TIMESTAMPTZ,
    cancelada_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inscricao_evento_email UNIQUE (evento_id, email_normalizado)
);

CREATE TABLE lista_espera (
    id UUID PRIMARY KEY,
    inscricao_id UUID NOT NULL UNIQUE REFERENCES inscricoes(id) ON DELETE CASCADE,
    evento_id UUID NOT NULL REFERENCES eventos(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('AGUARDANDO', 'PROMOVIDA', 'CANCELADA')),
    entrou_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finalizada_em TIMESTAMPTZ
);

CREATE TABLE convites (
    id UUID PRIMARY KEY,
    evento_id UUID NOT NULL REFERENCES eventos(id) ON DELETE CASCADE,
    nome VARCHAR(160),
    email VARCHAR(254) NOT NULL,
    email_normalizado VARCHAR(254) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE', 'ACEITO', 'EXPIRADO', 'CANCELADO')),
    expira_em TIMESTAMPTZ NOT NULL,
    inscricao_aceita_id UUID REFERENCES inscricoes(id) ON DELETE SET NULL,
    criado_por VARCHAR(254) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE credenciais (
    id UUID PRIMARY KEY,
    inscricao_id UUID NOT NULL UNIQUE REFERENCES inscricoes(id) ON DELETE CASCADE,
    evento_id UUID NOT NULL REFERENCES eventos(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ATIVA', 'UTILIZADA', 'REVOGADA')),
    emitida_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    utilizada_em TIMESTAMPTZ,
    revogada_em TIMESTAMPTZ
);

CREATE INDEX idx_inscricao_evento_status ON inscricoes(evento_id, status, criado_em);
CREATE INDEX idx_inscricao_indicador ON inscricoes(indicador_inscricao_id) WHERE indicador_inscricao_id IS NOT NULL;
CREATE INDEX idx_lista_espera_fila ON lista_espera(evento_id, entrou_em) WHERE status = 'AGUARDANDO';
CREATE INDEX idx_convite_evento_email ON convites(evento_id, email_normalizado, status);
