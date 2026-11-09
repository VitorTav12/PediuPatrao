import com.umc.pediupatrao.dto.DashboardDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final LocalDate DE = LocalDate.of(2026, 11, 1);
    private static final LocalDate ATE = LocalDate.of(2026, 11, 3);
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 1, 0, 0);
    private static final LocalDateTime FIM_EXCLUSIVO = LocalDateTime.of(2026, 11, 4, 0, 0);

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private List<Pedido> pedidos;

    private Pedido pedido(StatusPedido status, LocalDateTime horario, double valorTotal, double valorDesconto, ItemPedido... itens) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setHorarioEntrada(horario);
        pedido.setValorTotal(valorTotal);
        pedido.setValorDesconto(valorDesconto);
        pedido.setItens(List.of(itens));
        return pedido;
    }

    @BeforeEach
    void montarCenario() {
        ItemPedido pizza2 = new ItemPedido("prod1", "Pizza", 2, 50.0);
        ItemPedido pizza1 = new ItemPedido("prod1", "Pizza", 1, 50.0);
        ItemPedido refri4 = new ItemPedido("prod2", "Refrigerante", 4, 10.0);
        ItemPedido refri1 = new ItemPedido("prod2", "Refrigerante", 1, 10.0);

        pedidos = List.of(
                pedido(StatusPedido.FINALIZADO, LocalDateTime.of(2026, 11, 1, 12, 10), 90.0, 10.0, pizza2),
                pedido(StatusPedido.RECEBIDO, LocalDateTime.of(2026, 11, 1, 19, 30), 40.0, 0.0, refri4),
                pedido(StatusPedido.CANCELADO, LocalDateTime.of(2026, 11, 2, 20, 0), 50.0, 0.0, pizza1),
                pedido(StatusPedido.PRONTO, LocalDateTime.of(2026, 11, 3, 12, 45), 60.0, 0.0, pizza1, refri1)
        );
    }

    private void simularPeriodoCompleto() {
        when(pedidoRepository.buscarPorPeriodo(INICIO, FIM_EXCLUSIVO)).thenReturn(pedidos);
    }

    @Test
    void resumoCalculaIndicadoresIgnorandoPedidosCancelados() {
        simularPeriodoCompleto();

        DashboardDTO.Resumo resumo = dashboardService.resumo(DE, ATE, null);

        assertEquals(DE, resumo.de());
        assertEquals(ATE, resumo.ate());
        assertEquals(4, resumo.totalPedidos());
        assertEquals(3, resumo.pedidosValidos());
        assertEquals(1, resumo.pedidosCancelados());
        assertEquals(190.0, resumo.faturamento(), 0.001);
        assertEquals(63.33, resumo.ticketMedio(), 0.001);
        assertEquals(10.0, resumo.totalDescontos(), 0.001);
        assertEquals(25.0, resumo.percentualCancelamento(), 0.001);
    }

    @Test
    void vendasPorDiaPreencheDiasSemPedidosComZero() {
        simularPeriodoCompleto();

        List<DashboardDTO.VendaDiaria> dias = dashboardService.vendasPorDia(DE, ATE, null);

        assertEquals(3, dias.size());
        assertEquals(LocalDate.of(2026, 11, 1), dias.get(0).data());
        assertEquals(2, dias.get(0).pedidos());
        assertEquals(130.0, dias.get(0).faturamento(), 0.001);
        assertEquals(0, dias.get(1).pedidos());
        assertEquals(0.0, dias.get(1).faturamento(), 0.001);
        assertEquals(1, dias.get(2).pedidos());
        assertEquals(60.0, dias.get(2).faturamento(), 0.001);
    }

    @Test
    void vendasPorHoraAgrupaEmVinteEQuatroFaixas() {
        simularPeriodoCompleto();

        List<DashboardDTO.VendaHora> horas = dashboardService.vendasPorHora(DE, ATE, null);

        assertEquals(24, horas.size());
        assertEquals(2, horas.get(12).pedidos());
        assertEquals(150.0, horas.get(12).faturamento(), 0.001);
        assertEquals(1, horas.get(19).pedidos());
        assertEquals(40.0, horas.get(19).faturamento(), 0.001);
        assertEquals(0, horas.get(0).pedidos());
        assertEquals(0, horas.get(20).pedidos());
    }

    @Test
    void pedidosPorStatusListaTodosOsStatusNaOrdemDoFluxo() {
        simularPeriodoCompleto();

        List<DashboardDTO.StatusQuantidade> status = dashboardService.pedidosPorStatus(DE, ATE, null);

        assertEquals(
                List.of("RECEBIDO", "EM_PREPARO", "PRONTO", "SAIU_PARA_ENTREGA", "RETIRADO", "FINALIZADO", "CANCELADO"),
                status.stream().map(DashboardDTO.StatusQuantidade::status).toList()
        );
        assertEquals(
                List.of(1L, 0L, 1L, 0L, 0L, 1L, 1L),
                status.stream().map(DashboardDTO.StatusQuantidade::quantidade).toList()
        );
    }

    @Test
    void produtosMaisVendidosOrdenaPorQuantidadeEIgnoraCancelados() {
        simularPeriodoCompleto();

        List<DashboardDTO.ProdutoVendido> produtos = dashboardService.produtosMaisVendidos(DE, ATE, null, 5);

        assertEquals(2, produtos.size());
        assertEquals("Refrigerante", produtos.get(0).nome());
        assertEquals(5, produtos.get(0).quantidade());
        assertEquals(50.0, produtos.get(0).faturamento(), 0.001);
        assertEquals("Pizza", produtos.get(1).nome());
        assertEquals(3, produtos.get(1).quantidade());
        assertEquals(150.0, produtos.get(1).faturamento(), 0.001);
    }

    @Test
    void produtosMaisVendidosRespeitaOLimite() {
        simularPeriodoCompleto();

        List<DashboardDTO.ProdutoVendido> produtos = dashboardService.produtosMaisVendidos(DE, ATE, null, 1);

        assertEquals(1, produtos.size());
        assertEquals("Refrigerante", produtos.get(0).nome());
    }

    @Test
    void periodoSemPedidosRetornaIndicadoresZerados() {
        when(pedidoRepository.buscarPorPeriodo(INICIO, FIM_EXCLUSIVO)).thenReturn(List.of());

        DashboardDTO.Resumo resumo = dashboardService.resumo(DE, ATE, null);

        assertEquals(0, resumo.totalPedidos());
        assertEquals(0.0, resumo.faturamento(), 0.001);
        assertEquals(0.0, resumo.ticketMedio(), 0.001);
        assertEquals(0.0, resumo.percentualCancelamento(), 0.001);
    }

    @Test
    void semDatasUsaOsUltimosTrintaDias() {
        when(pedidoRepository.buscarPorPeriodo(any(), any())).thenReturn(List.of());

        DashboardDTO.Resumo resumo = dashboardService.resumo(null, null, null);

        assertEquals(29, ChronoUnit.DAYS.between(resumo.de(), resumo.ate()));
    }

    @Test
    void filtroPorStatusUsaAConsultaComStatus() {
        when(pedidoRepository.buscarPorPeriodoEStatus(INICIO, FIM_EXCLUSIVO, StatusPedido.FINALIZADO))
                .thenReturn(List.of(pedidos.get(0)));

        DashboardDTO.Resumo resumo = dashboardService.resumo(DE, ATE, StatusPedido.FINALIZADO);

        assertEquals(1, resumo.totalPedidos());
        assertEquals(90.0, resumo.faturamento(), 0.001);
        verify(pedidoRepository, never()).buscarPorPeriodo(any(), any());
    }

    @Test
    void dataInicialPosteriorADataFinalEhRejeitada() {
        assertThrows(
                IllegalArgumentException.class,
                () -> dashboardService.resumo(ATE, DE, null)
        );

        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void periodoMaiorQueUmAnoEhRejeitado() {
        assertThrows(
                IllegalArgumentException.class,
                () -> dashboardService.vendasPorDia(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 11, 1), null)
        );

        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void limiteInvalidoDeProdutosEhRejeitado() {
        assertThrows(
                IllegalArgumentException.class,
                () -> dashboardService.produtosMaisVendidos(DE, ATE, null, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> dashboardService.produtosMaisVendidos(DE, ATE, null, 21)
        );

        verifyNoInteractions(pedidoRepository);
    }
}