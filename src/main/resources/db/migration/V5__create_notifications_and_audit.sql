CREATE TABLE notificacoes (
    id UUID PRIMARY KEY,
    tipo VARCHAR(60) NOT NULL,
    destinatario VARCHAR(254) NOT NULL,
    assunto VARCHAR(180) NOT NULL,
    conteudo TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE', 'ENVIANDO', 'ENVIADA', 'FALHA')),
    tentativas INTEGER NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_erro TEXT,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviada_em TIMESTAMPTZ
);

CREATE TABLE eventos_outbox (
    id UUID PRIMARY KEY,
    tipo VARCHAR(80) NOT NULL,
    agregado_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE', 'PROCESSANDO', 'PROCESSADO', 'FALHA')),
    tentativas INTEGER NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processado_em TIMESTAMPTZ,
    ultimo_erro TEXT
);

CREATE TABLE auditorias (
    id UUID PRIMARY KEY,
    organizacao_id UUID REFERENCES organizacoes(id) ON DELETE SET NULL,
    ator VARCHAR(254) NOT NULL,
    acao VARCHAR(80) NOT NULL,
    recurso VARCHAR(80) NOT NULL,
    recurso_id VARCHAR(100) NOT NULL,
    dados JSONB NOT NULL DEFAULT '{}'::jsonb,
    id_correlacao VARCHAR(100),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notificacao_processamento ON notificacoes(status, proxima_tentativa_em);
CREATE INDEX idx_outbox_processamento ON eventos_outbox(status, proxima_tentativa_em);
CREATE INDEX idx_auditoria_recurso ON auditorias(recurso, recurso_id, criado_em DESC);
CREATE INDEX idx_auditoria_organizacao ON auditorias(organizacao_id, criado_em DESC);
