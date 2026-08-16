package com.eventops.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenGeneratorTest {

    private final TokenGenerator gerador = new TokenGenerator();

    @Test
    void deveGerarTokensImprevisiveisEHashDeterministico() {
        String primeiro = gerador.gerar();
        String segundo = gerador.gerar();

        assertThat(primeiro).hasSizeGreaterThanOrEqualTo(40).isNotEqualTo(segundo);
        assertThat(gerador.hash(primeiro)).hasSize(64).isEqualTo(gerador.hash(primeiro));
        assertThat(gerador.hash(primeiro)).isNotEqualTo(gerador.hash(segundo));
    }
}
