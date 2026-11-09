package com.umc.pediupatrao.dto;

import java.time.LocalDate;

public record FiltroLog(
        String entidade,
        String entidadeId,
        String usuario,
        String operacao,
        LocalDate de,
        LocalDate ate
) {
}