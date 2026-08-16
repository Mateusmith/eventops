package com.eventops.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @Test
    void deveNormalizarEmailESlug() {
        assertThat(TextNormalizer.email("  Pessoa@Exemplo.COM ")).isEqualTo("pessoa@exemplo.com");
        assertThat(TextNormalizer.slug("Conferencia Java & Spring 2026"))
                .isEqualTo("conferencia-java-spring-2026");
    }
}
