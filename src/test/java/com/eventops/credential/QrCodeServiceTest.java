package com.eventops.credential;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QrCodeServiceTest {

    @Test
    void deveGerarImagemPng() {
        byte[] imagem = new QrCodeService().gerar("credencial-segura-de-teste");

        assertThat(imagem).hasSizeGreaterThan(300);
        assertThat(imagem[0]).isEqualTo((byte) 0x89);
        assertThat(imagem[1]).isEqualTo((byte) 0x50);
        assertThat(imagem[2]).isEqualTo((byte) 0x4E);
        assertThat(imagem[3]).isEqualTo((byte) 0x47);
    }
}
