package com.eventops.shared;

import java.util.Locale;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentActor {

    public Actor obter() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken token) || !autenticacao.isAuthenticated()) {
            throw new ForbiddenException("AUTENTICACAO_NECESSARIA", "Autenticacao necessaria para esta operacao.");
        }

        String email = valor(token, "email", token.getName());
        String nome = valor(token, "name", valor(token, "preferred_username", email));
        MDC.put("usuario", email);
        return new Actor(token.getToken().getSubject(), nome, email.toLowerCase(Locale.ROOT));
    }

    private String valor(JwtAuthenticationToken token, String chave, String padrao) {
        Object valor = token.getTokenAttributes().get(chave);
        return valor == null ? padrao : valor.toString();
    }
}
