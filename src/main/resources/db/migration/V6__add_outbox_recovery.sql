ALTER TABLE eventos_outbox ADD COLUMN bloqueado_em TIMESTAMPTZ;

CREATE INDEX idx_outbox_recuperacao
    ON eventos_outbox(status, bloqueado_em)
    WHERE status = 'PROCESSANDO';
