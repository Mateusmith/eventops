package com.eventops.event.internal;

import com.eventops.event.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eventos")
public class EventEntity {

    @Id
    private UUID id;
    @Column(name = "organizacao_id")
    private UUID organizacaoId;
    private String titulo;
    private String slug;
    private String descricao;
    private String local;
    @Column(name = "fuso_horario")
    private String fusoHorario;
    @Column(name = "inicio_em")
    private Instant inicioEm;
    @Column(name = "fim_em")
    private Instant fimEm;
    private Integer capacidade;
    @Column(name = "vagas_ocupadas")
    private int vagasOcupadas;
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    @Column(name = "criado_por")
    private String criadoPor;
    @Column(name = "criado_em")
    private Instant criadoEm;
    @Column(name = "atualizado_em")
    private Instant atualizadoEm;
    @Version
    private long versao;

    protected EventEntity() {
    }

    public EventEntity(UUID organizacaoId, String titulo, String slug, String descricao, String local,
            String fusoHorario, Instant inicioEm, Instant fimEm, Integer capacidade,
            String criadoPor, Instant agora) {
        this.id = UUID.randomUUID();
        this.organizacaoId = organizacaoId;
        this.titulo = titulo;
        this.slug = slug;
        this.descricao = descricao;
        this.local = local;
        this.fusoHorario = fusoHorario;
        this.inicioEm = inicioEm;
        this.fimEm = fimEm;
        this.capacidade = capacidade;
        this.vagasOcupadas = 0;
        this.status = EventStatus.RASCUNHO;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public void atualizar(String titulo, String slug, String descricao, String local, String fusoHorario,
            Instant inicioEm, Instant fimEm, Integer capacidade, Instant agora) {
        this.titulo = titulo;
        this.slug = slug;
        this.descricao = descricao;
        this.local = local;
        this.fusoHorario = fusoHorario;
        this.inicioEm = inicioEm;
        this.fimEm = fimEm;
        this.capacidade = capacidade;
        this.atualizadoEm = agora;
    }

    public void publicar(Instant agora) {
        this.status = EventStatus.PUBLICADO;
        this.atualizadoEm = agora;
    }

    public void encerrarInscricoes(Instant agora) {
        this.status = EventStatus.INSCRICOES_ENCERRADAS;
        this.atualizadoEm = agora;
    }

    public void cancelar(Instant agora) {
        this.status = EventStatus.CANCELADO;
        this.atualizadoEm = agora;
    }

    public void finalizar(Instant agora) {
        this.status = EventStatus.FINALIZADO;
        this.atualizadoEm = agora;
    }

    public UUID getId() { return id; }
    public UUID getOrganizacaoId() { return organizacaoId; }
    public String getTitulo() { return titulo; }
    public String getSlug() { return slug; }
    public String getDescricao() { return descricao; }
    public String getLocal() { return local; }
    public String getFusoHorario() { return fusoHorario; }
    public Instant getInicioEm() { return inicioEm; }
    public Instant getFimEm() { return fimEm; }
    public Integer getCapacidade() { return capacidade; }
    public int getVagasOcupadas() { return vagasOcupadas; }
    public EventStatus getStatus() { return status; }
    public long getVersao() { return versao; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
