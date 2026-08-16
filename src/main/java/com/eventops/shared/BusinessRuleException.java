package com.eventops.shared;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String codigo, String mensagem) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensagem);
    }
}
