package com.eventops.registration;

import java.util.UUID;

public record RankingItemResponse(int posicao, UUID inscricaoId, String participante, long indicacoesConfirmadas) {
}
