package com.eventops.shared;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima) {

    public static <T> PageResponse<T> de(Page<T> pagina) {
        return new PageResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages(), pagina.isFirst(), pagina.isLast());
    }
}
