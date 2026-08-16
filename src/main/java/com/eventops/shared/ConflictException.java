package com.eventops.shared;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {

    public ConflictException(String codigo, String mensagem) {
        super(HttpStatus.CONFLICT, codigo, mensagem);
    }
}
