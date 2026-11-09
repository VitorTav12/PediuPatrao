package com.umc.pediupatrao.dto;

import java.util.List;

public record PaginaLogDTO(
        List<LogDTO> itens,
        int pagina,
        int tamanho,
        long total,
        int totalPaginas
) {
}