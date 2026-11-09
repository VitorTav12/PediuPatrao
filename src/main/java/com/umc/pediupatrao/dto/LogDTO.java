package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record LogDTO(
        String id,
        String usuario,
        String perfil,
        LocalDateTime dataHora,
        String operacao,
        String entidade,
        String entidadeId,
        List<AlteracaoDTO> alteracoes,
        String justificativa
) {

    private static final Set<String> CAMPOS_SENSIVEIS = Set.of("telefone");

    public record AlteracaoDTO(String campo, String valorAnterior, String valorNovo) {
    }

    public static LogDTO de(Log log) {
        List<AlteracaoDTO> alteracoes = log.getListaAlteracoes() == null
                ? List.of()
                : log.getListaAlteracoes().stream()
                        .map(alteracao -> new AlteracaoDTO(
                                alteracao.getCampo(),
                                mascarar(alteracao.getCampo(), alteracao.getValorAnterior()),
                                mascarar(alteracao.getCampo(), alteracao.getValorNovo())
                        ))
                        .toList();

        return new LogDTO(
                log.getId(),
                log.getUsuario(),
                log.getPerfil(),
                log.getMomentoRegistro(),
                log.getTipoAcao(),
                log.getEntidade(),
                log.getEntidadeId(),
                alteracoes,
                log.getJustificativa()
        );
    }

    public static String mascarar(String campo, String valor) {
        if (valor == null || campo == null || !CAMPOS_SENSIVEIS.contains(campo.toLowerCase())) {
            return valor;
        }

        String digitos = valor.replaceAll("\\D", "");

        if (digitos.length() <= 4) {
            return "****";
        }

        return "*".repeat(digitos.length() - 4) + digitos.substring(digitos.length() - 4);
    }
}