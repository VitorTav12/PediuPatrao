package com.umc.pediupatrao.dto;

import java.time.LocalDate;

public final class DashboardDTO {

    private DashboardDTO() {
    }

    public record Resumo(
            LocalDate de,
            LocalDate ate,
            long totalPedidos,
            long pedidosValidos,
            long pedidosCancelados,
            double faturamento,
            double ticketMedio,
            double totalDescontos,
            double percentualCancelamento
    ) {
    }

    public record VendaDiaria(LocalDate data, long pedidos, double faturamento) {
    }

    public record VendaHora(int hora, long pedidos, double faturamento) {
    }

    public record StatusQuantidade(String status, long quantidade) {
    }

    public record ProdutoVendido(String produtoId, String nome, long quantidade, double faturamento) {
    }
}