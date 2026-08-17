package com.eventops.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "eventops.observabilidade")
public record ObservabilityProperties(String usuario, String senha) {

    public ObservabilityProperties {
        Assert.hasText(usuario, "O usuario de observabilidade deve ser informado.");
        Assert.hasText(senha, "A senha de observabilidade deve ser informada.");
    }
}
