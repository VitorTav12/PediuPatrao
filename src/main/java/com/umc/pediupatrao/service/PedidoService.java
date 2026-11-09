package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.PedidoRequestDTO;
import com.umc.pediupatrao.dto.ItemPedidoDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.umc.pediupatrao.entity.Log;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private ProdutoRepository produtoRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private LogService logService;

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES_VALIDAS = Map.of(
            StatusPedido.RECEBIDO, Set.of(StatusPedido.EM_PREPARO),
            StatusPedido.EM_PREPARO, Set.of(StatusPedido.PRONTO),
            StatusPedido.PRONTO, Set.of(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO),
            StatusPedido.SAIU_PARA_ENTREGA, Set.of(StatusPedido.FINALIZADO),
            StatusPedido.RETIRADO, Set.of(StatusPedido.FINALIZADO),
            StatusPedido.FINALIZADO, Set.of(),
            StatusPedido.CANCELADO, Set.of()
    );

    public Pedido criarPedido(PedidoRequestDTO dadosRequisicao, Authentication authContexto) {
        if (dadosRequisicao == null) {
            throw new IllegalArgumentException("Os dados do pedido são obrigatórios");
        }

        if (dadosRequisicao.getIdentificadorCliente() == null || dadosRequisicao.getIdentificadorCliente().isBlank()) {
            throw new IllegalArgumentException("O cliente é obrigatório");
        }

        clienteRepository.findById(dadosRequisicao.getIdentificadorCliente())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

        if (dadosRequisicao.getListaDeItens() == null || dadosRequisicao.getListaDeItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido deve possuir pelo menos um item");
        }

        int somaTotalQuantidades = dadosRequisicao.getListaDeItens().stream().mapToInt(ItemPedidoDTO::getQtdSelecionada).sum();
        if (somaTotalQuantidades > 50) {
            throw new IllegalArgumentException("A quantidade total de itens num pedido não pode exceder 50 unidades.");
        }

        if (authContexto == null || authContexto.getName() == null) {
            throw new IllegalArgumentException("Não foi possível identificar o responsável pelo pedido");
        }

        List<ItemPedido> listaItensProcessados = new ArrayList<>();
        double valorAcumuladoTotal = 0;

        for (ItemPedidoDTO itemDto : dadosRequisicao.getListaDeItens()) {
            if (itemDto == null) {
                throw new IllegalArgumentException("O item do pedido é obrigatório");
            }

            if (itemDto.getCodigoDoProduto() == null || itemDto.getCodigoDoProduto().isBlank()) {
                throw new IllegalArgumentException("O produto é obrigatório");
            }

            if (itemDto.getQtdSelecionada() <= 0) {
                throw new IllegalArgumentException("A quantidade do produto deve ser maior que zero");
            }

            Produto produtoEncontrado = produtoRepository.findById(itemDto.getCodigoDoProduto())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + itemDto.getCodigoDoProduto()));

            if (!Boolean.TRUE.equals(produtoEncontrado.getAtivo())) {
                throw new IllegalArgumentException("Produto inativo: " + produtoEncontrado.getNome());
            }

            ItemPedido itemRegisto = new ItemPedido(
                    produtoEncontrado.getId(),
                    produtoEncontrado.getNome(),
                    itemDto.getQtdSelecionada(),
                    produtoEncontrado.getPreco()
            );

            listaItensProcessados.add(itemRegisto);
            valorAcumuladoTotal += itemRegisto.calcularValorTotalDoItem();
        }

        Pedido novoPedido = new Pedido();
        novoPedido.setClienteId(dadosRequisicao.getIdentificadorCliente());
        novoPedido.setItens(listaItensProcessados);
        novoPedido.setValorTotal(valorAcumuladoTotal);
        novoPedido.setResponsavelId(authContexto.getName());
        novoPedido.setHorarioEntrada(LocalDateTime.now());
        novoPedido.setStatus(StatusPedido.RECEBIDO);

        Pedido pedidoPersistido = pedidoRepository.save(novoPedido);

        List<Log.DetalheAlteracao> alteracoesCriacao = List.of(
                new Log.DetalheAlteracao("clienteId", null, pedidoPersistido.getClienteId()),
                new Log.DetalheAlteracao("itens", null, formatarDescricaoItens(pedidoPersistido.getItens())),
                new Log.DetalheAlteracao("valorTotal", null, String.valueOf(pedidoPersistido.getValorTotal())),
                new Log.DetalheAlteracao("responsavel", null, pedidoPersistido.getResponsavelId()),
                new Log.DetalheAlteracao("status", null, pedidoPersistido.getStatus().name())
        );

        logService.registrar(authContexto, "CRIACAO_PEDIDO", "Pedido", pedidoPersistido.getId(), alteracoesCriacao, null);
        return pedidoPersistido;
    }

    private String formatarDescricaoItens(List<ItemPedido> itensLista) {
        if (itensLista == null || itensLista.isEmpty()) {
            return "";
        }
        return itensLista.stream()
                .map(item -> item.getQtd() + "x " + item.getDescricaoProduto() + " @ " + item.getValorUnitario())
                .collect(Collectors.joining("; "));
    }

    private boolean estadoJaSaiu(StatusPedido estadoAtualStatus) {
        return estadoAtualStatus == StatusPedido.SAIU_PARA_ENTREGA
                || estadoAtualStatus == StatusPedido.RETIRADO
                || estadoAtualStatus == StatusPedido.FINALIZADO;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido atualizarStatus(String idPedido, StatusPedido proximoEstado, Authentication authContexto) {
        if (proximoEstado == null) {
            throw new IllegalArgumentException("O novo status é obrigatório");
        }

        if (authContexto == null || !authContexto.isAuthenticated()) {
            throw new SecurityException("É necessário identificar o usuário autenticado");
        }

        Pedido pedidoAlvo = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + idPedido));

        StatusPedido statusAtualDoPedido = pedidoAlvo.getStatus();
        Set<StatusPedido> destinosPermitidos = TRANSICOES_VALIDAS.get(statusAtualDoPedido);

        if (destinosPermitidos == null || !destinosPermitidos.contains(proximoEstado)) {
            throw new IllegalArgumentException("Transição inválida: não é possível ir de " + statusAtualDoPedido + " para " + proximoEstado);
        }

        boolean exigeRegistoSaida = proximoEstado == StatusPedido.SAIU_PARA_ENTREGA || proximoEstado == StatusPedido.RETIRADO;

        if (exigeRegistoSaida && authContexto.getName() == null) {
            throw new IllegalArgumentException("Não foi possível identificar o responsável pela saída");
        }

        pedidoAlvo.setStatus(proximoEstado);

        if (exigeRegistoSaida) {
            pedidoAlvo.setResponsavelSaidaId(authContexto.getName());
            pedidoAlvo.setHorarioSaida(LocalDateTime.now());
        }

        Pedido pedidoAtualizado = pedidoRepository.save(pedidoAlvo);

        logService.registrar(authContexto, "ALTERACAO_STATUS", "Pedido", pedidoAtualizado.getId(), 
                List.of(new Log.DetalheAlteracao("status", statusAtualDoPedido.name(), proximoEstado.name())), null);

        return pedidoAtualizado;
    }

    public Pedido cancelarPedido(String idPedido, String motivoCancelamento, Authentication authContexto) {
        if (authContexto == null || authContexto.getAuthorities().stream().noneMatch(autoridade -> autoridade.getAuthority().equals("ROLE_GERENTE"))) {
            throw new SecurityException("Somente o GERENTE pode cancelar pedidos");
        }

        if (motivoCancelamento == null || motivoCancelamento.isBlank()) {
            throw new IllegalArgumentException("A justificativa do cancelamento é obrigatória");
        }

        Pedido pedidoAlvo = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + idPedido));

        StatusPedido statusAtualDoPedido = pedidoAlvo.getStatus();

        if (estadoJaSaiu(statusAtualDoPedido) || statusAtualDoPedido == StatusPedido.CANCELADO) {
            throw new IllegalArgumentException("Não é possível cancelar um pedido que já saiu para entrega, foi retirado, finalizado ou cancelado");
        }

        pedidoAlvo.setStatus(StatusPedido.CANCELADO);
        pedidoAlvo.setCancelamentoPedido(motivoCancelamento.trim());

        Pedido pedidoCancelado = pedidoRepository.save(pedidoAlvo);

        logService.registrar(
                authContexto,
                "CANCELAMENTO_PEDIDO",
                "Pedido",
                pedidoCancelado.getId(),
                List.of(
                        new Log.DetalheAlteracao("status", statusAtualDoPedido.name(), StatusPedido.CANCELADO.name()),
                        new Log.DetalheAlteracao("cancelamentoPedido", null, motivoCancelamento.trim())
                ),
                motivoCancelamento.trim()
        );

        return pedidoCancelado;
    }

    public Pedido aplicarDesconto(String idPedido, double taxaDesconto, Authentication authContexto) {
        if (authContexto == null || authContexto.getAuthorities() == null || authContexto.getAuthorities().stream().noneMatch(autoridade -> autoridade.getAuthority().equals("ROLE_GERENTE"))) {
            throw new SecurityException("Somente o GERENTE pode aplicar descontos");
        }

        if (!Double.isFinite(taxaDesconto) || taxaDesconto <= 0 || taxaDesconto > 20) {
            throw new IllegalArgumentException("O desconto deve ser maior que zero e de, no máximo, 20%");
        }

        Pedido pedidoAlvo = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + idPedido));

        if (pedidoAlvo.getStatus() == StatusPedido.CANCELADO) {
            throw new IllegalArgumentException("Não é possível aplicar desconto em um pedido cancelado");
        }

        if (estadoJaSaiu(pedidoAlvo.getStatus())) {
            throw new IllegalArgumentException("Não é possível aplicar desconto após a saída para entrega ou a retirada");
        }

        if (pedidoAlvo.getItens() == null || pedidoAlvo.getItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido não possui itens para calcular o desconto");
        }

        Double percentualAnterior = pedidoAlvo.getPercentualDesconto();
        Double valorDescontoAnterior = pedidoAlvo.getValorDesconto();
        Double valorTotalAnterior = pedidoAlvo.getValorTotal();

        double subtotalOriginal = pedidoAlvo.getItens().stream().mapToDouble(ItemPedido::calcularValorTotalDoItem).sum();
        double valorDescontoCalculado = Math.round(subtotalOriginal * taxaDesconto / 100.0 * 100.0) / 100.0;
        double valorTotalAtualizado = Math.round((subtotalOriginal - valorDescontoCalculado) * 100.0) / 100.0;

        pedidoAlvo.setPercentualDesconto(taxaDesconto);
        pedidoAlvo.setValorDesconto(valorDescontoCalculado);
        pedidoAlvo.setValorTotal(valorTotalAtualizado);

        Pedido pedidoComDesconto = pedidoRepository.save(pedidoAlvo);

        logService.registrar(
                authContexto,
                "APLICACAO_DESCONTO",
                "Pedido",
                pedidoComDesconto.getId(),
                List.of(
                        new Log.DetalheAlteracao("percentualDesconto", String.valueOf(percentualAnterior), String.valueOf(taxaDesconto)),
                        new Log.DetalheAlteracao("valorDesconto", String.valueOf(valorDescontoAnterior), String.valueOf(valorDescontoCalculado)),
                        new Log.DetalheAlteracao("valorTotal", String.valueOf(valorTotalAnterior), String.valueOf(valorTotalAtualizado))
                ),
                null
        );

        return pedidoComDesconto;
    }
}