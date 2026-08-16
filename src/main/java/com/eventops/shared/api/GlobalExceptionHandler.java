package com.eventops.shared.api;

import com.eventops.shared.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> tratarDominio(DomainException excecao, HttpServletRequest requisicao) {
        return resposta(excecao.getStatus(), excecao.getCodigo(), excecao.getMessage(), requisicao, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> tratarValidacao(MethodArgumentNotValidException excecao, HttpServletRequest requisicao) {
        List<ApiError.FieldViolation> violacoes = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ApiError.FieldViolation(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Existem campos invalidos.", requisicao, violacoes);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> tratarIntegridade(DataIntegrityViolationException excecao, HttpServletRequest requisicao) {
        return resposta(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS",
                "A operacao conflita com um registro existente.", requisicao, List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> tratarRequisicaoInvalida(Exception excecao, HttpServletRequest requisicao) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA",
                "A requisicao esta ausente ou possui formato invalido.", requisicao, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> tratarConcorrencia(ObjectOptimisticLockingFailureException excecao,
            HttpServletRequest requisicao) {
        return resposta(HttpStatus.CONFLICT, "ALTERACAO_CONCORRENTE",
                "O registro foi alterado por outra operacao. Consulte-o novamente.", requisicao, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> tratarAcessoNegado(AccessDeniedException excecao, HttpServletRequest requisicao) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                "Voce nao possui permissao para esta operacao.", requisicao, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> tratarRecursoInexistente(
            NoResourceFoundException excecao,
            HttpServletRequest requisicao) {
        return resposta(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                "O recurso solicitado nao foi encontrado.", requisicao, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> tratarInesperado(Exception excecao, HttpServletRequest requisicao) {
        LOGGER.error("Erro inesperado em {}", requisicao.getRequestURI(), excecao);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Ocorreu um erro interno inesperado.", requisicao, List.of());
    }

    private ResponseEntity<ApiError> resposta(
            HttpStatus status,
            String codigo,
            String mensagem,
            HttpServletRequest requisicao,
            List<ApiError.FieldViolation> violacoes) {
        ApiError erro = new ApiError(
                Instant.now(), status.value(), codigo, mensagem, requisicao.getRequestURI(),
                MDC.get("idCorrelacao"), violacoes);
        return ResponseEntity.status(status).body(erro);
    }
}
