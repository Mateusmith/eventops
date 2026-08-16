package com.eventops.shared;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends DomainException {

    public ForbiddenException(String codigo, String mensagem) {
        super(HttpStatus.FORBIDDEN, codigo, mensagem);
    }
}
