package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DashboardService {

    private static final int DIAS_PADRAO = 30;
    private static final int DIAS_MAXIMOS = 366;
    private static final int LIMITE_MAXIMO_PRODUTOS = 20;

    private final PedidoRepository pedidoRepository;

    public DashboardService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public DashboardDTO.Resumo resumo(LocalDate de, LocalDate ate, StatusPedido status) {
        Periodo periodo = definirPeriodo(de, ate);
        List<Pedido> pedidos = buscar(periodo, status);
        List<Pedido> vendas = apenasVendas(pedidos);

        long total = pedidos.size();
        long validos = vendas.size();
        long cancelados = pedidos.stream().filter(p -> p.getStatus() == StatusPedido.CANCELADO).count();

        double faturamento = arredondar(vendas.stream().mapToDouble(Pedido::getValorTotal).sum());
        double descontos = arredondar(vendas.stream().mapToDouble(Pedido::getValorDesconto).sum());
        double ticketMedio = validos > 0 ? arredondar(faturamento / validos) : 0.0;
        double percentualCancelamento = total > 0 ? arredondar(cancelados * 100.0 / total) : 0.0;

        return new DashboardDTO.Resumo(
                periodo.de(),
                periodo.ate(),
                total,
                validos,
                cancelados,
                faturamento,
                ticketMedio,
                descontos,
                percentualCancelamento
        );
    }

    public List<DashboardDTO.VendaDiaria> vendasPorDia(LocalDate de, LocalDate ate, StatusPedido status) {
        Periodo periodo = definirPeriodo(de, ate);
        List<Pedido> vendas = apenasVendas(buscar(periodo, status));

        Map<LocalDate, List<Pedido>> porDia = vendas.stream()
                .filter(p -> p.getHorarioEntrada() != null)
                .collect(Collectors.groupingBy(p -> p.getHorarioEntrada().toLocalDate()));

        List<DashboardDTO.VendaDiaria> resultado = new ArrayList<>();

        for (LocalDate dia = periodo.de(); !dia.isAfter(periodo.ate()); dia = dia.plusDays(1)) {
            List<Pedido> doDia = porDia.getOrDefault(dia, List.of());
            double faturamento = arredondar(doDia.stream().mapToDouble(Pedido::getValorTotal).sum());
            resultado.add(new DashboardDTO.VendaDiaria(dia, doDia.size(), faturamento));
        }

        return resultado;
    }

    public List<DashboardDTO.VendaHora> vendasPorHora(LocalDate de, LocalDate ate, StatusPedido status) {
        Periodo periodo = definirPeriodo(de, ate);
        List<Pedido> vendas = apenasVendas(buscar(periodo, status));

        long[] pedidosPorHora = new long[24];
        double[] faturamentoPorHora = new double[24];

        for (Pedido pedido : vendas) {
            if (pedido.getHorarioEntrada() == null) {
                continue;
            }

            int hora = pedido.getHorarioEntrada().getHour();
            pedidosPorHora[hora]++;
            faturamentoPorHora[hora] += pedido.getValorTotal();
        }

        return IntStream.range(0, 24)
                .mapToObj(hora -> new DashboardDTO.VendaHora(
                        hora,
                        pedidosPorHora[hora],
                        arredondar(faturamentoPorHora[hora])
                ))
                .toList();
    }

    public List<DashboardDTO.StatusQuantidade> pedidosPorStatus(LocalDate de, LocalDate ate, StatusPedido status) {
        Periodo periodo = definirPeriodo(de, ate);
        List<Pedido> pedidos = buscar(periodo, status);

        Map<StatusPedido, Long> contagem = pedidos.stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.groupingBy(Pedido::getStatus, Collectors.counting()));

        return Arrays.stream(StatusPedido.values())
                .map(s -> new DashboardDTO.StatusQuantidade(s.name(), contagem.getOrDefault(s, 0L)))
                .toList();
    }

    public List<DashboardDTO.ProdutoVendido> produtosMaisVendidos(LocalDate de, LocalDate ate, StatusPedido status, int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO_PRODUTOS) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + LIMITE_MAXIMO_PRODUTOS + ".");
        }

        Periodo periodo = definirPeriodo(de, ate);
        List<Pedido> vendas = apenasVendas(buscar(periodo, status));

        Map<String, AcumuladorProduto> porProduto = new LinkedHashMap<>();

        for (Pedido pedido : vendas) {
            if (pedido.getItens() == null) {
                continue;
            }

            for (ItemPedido item : pedido.getItens()) {
                String chave = item.getIdProduto() != null ? item.getIdProduto() : item.getDescricaoProduto();

                AcumuladorProduto acumulador = porProduto.computeIfAbsent(
                        chave,
                        k -> new AcumuladorProduto(item.getIdProduto(), item.getDescricaoProduto())
                );

                acumulador.quantidade += item.getQtd();
                acumulador.faturamento += item.calcularValorTotalDoItem();
            }
        }

        Comparator<AcumuladorProduto> ordem = Comparator
                .comparingLong((AcumuladorProduto a) -> a.quantidade).reversed()
                .thenComparing(Comparator.comparingDouble((AcumuladorProduto a) -> a.faturamento).reversed())
                .thenComparing(a -> a.nome, Comparator.nullsLast(Comparator.<String>naturalOrder()));

        return porProduto.values().stream()
                .sorted(ordem)
                .limit(limite)
                .map(a -> new DashboardDTO.ProdutoVendido(a.produtoId, a.nome, a.quantidade, arredondar(a.faturamento)))
                .toList();
    }

    private Periodo definirPeriodo(LocalDate de, LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now();
        LocalDate inicio = de != null ? de : fim.minusDays(DIAS_PADRAO - 1L);

        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }

        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > DIAS_MAXIMOS) {
            throw new IllegalArgumentException("O período máximo permitido é de " + DIAS_MAXIMOS + " dias.");
        }

        return new Periodo(inicio, fim);
    }

    private List<Pedido> buscar(Periodo periodo, StatusPedido status) {
        LocalDateTime inicio = periodo.de().atStartOfDay();
        LocalDateTime fimExclusivo = periodo.ate().plusDays(1).atStartOfDay();

        if (status == null) {
            return pedidoRepository.buscarPorPeriodo(inicio, fimExclusivo);
        }

        return pedidoRepository.buscarPorPeriodoEStatus(inicio, fimExclusivo, status);
    }

    private List<Pedido> apenasVendas(List<Pedido> pedidos) {
        return pedidos.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() != StatusPedido.CANCELADO)
                .toList();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private record Periodo(LocalDate de, LocalDate ate) {
    }

    private static final class AcumuladorProduto {
        private final String produtoId;
        private final String nome;
        private long quantidade;
        private double faturamento;

        private AcumuladorProduto(String produtoId, String nome) {
            this.produtoId = produtoId;
            this.nome = nome;
        }
    }
}