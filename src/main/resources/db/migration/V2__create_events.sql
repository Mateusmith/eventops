CREATE TABLE eventos (
    id UUID PRIMARY KEY,
    organizacao_id UUID NOT NULL REFERENCES organizacoes(id) ON DELETE RESTRICT,
    titulo VARCHAR(180) NOT NULL,
    slug VARCHAR(140) NOT NULL UNIQUE,
    descricao TEXT NOT NULL,
    local VARCHAR(220) NOT NULL,
    fuso_horario VARCHAR(60) NOT NULL,
    inicio_em TIMESTAMPTZ NOT NULL,
    fim_em TIMESTAMPTZ NOT NULL,
    capacidade INTEGER,
    vagas_ocupadas INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL CHECK (status IN ('RASCUNHO', 'PUBLICADO', 'INSCRICOES_ENCERRADAS', 'CANCELADO', 'FINALIZADO')),
    criado_por VARCHAR(254) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_evento_periodo CHECK (fim_em > inicio_em),
    CONSTRAINT ck_evento_capacidade CHECK (capacidade IS NULL OR capacidade > 0),
    CONSTRAINT ck_evento_vagas CHECK (vagas_ocupadas >= 0 AND (capacidade IS NULL OR vagas_ocupadas <= capacidade))
);

CREATE INDEX idx_evento_organizacao ON eventos(organizacao_id, criado_em DESC);
CREATE INDEX idx_evento_publico ON eventos(slug) WHERE status = 'PUBLICADO';
