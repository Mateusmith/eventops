package com.eventops.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            FilterChain cadeia) throws ServletException, IOException {
        String recebido = requisicao.getHeader(CABECALHO);
        String id = recebido != null && recebido.matches("[A-Za-z0-9._-]{1,100}")
                ? recebido
                : UUID.randomUUID().toString();
        try {
            MDC.put("idCorrelacao", id);
            resposta.setHeader(CABECALHO, id);
            cadeia.doFilter(requisicao, resposta);
        } finally {
            MDC.remove("idCorrelacao");
            MDC.remove("usuario");
        }
    }
}
