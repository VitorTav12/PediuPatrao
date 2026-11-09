package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.PedidoRequestDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService servicePedido;

    public PedidoController(PedidoService servicePedido) {
        this.servicePedido = servicePedido;
    }

    @PostMapping
    public Pedido criar(@RequestBody PedidoRequestDTO requestDTO, Authentication auth) {
        return servicePedido.criarPedido(requestDTO, auth);
    }

    @GetMapping
    public List<Pedido> listarTodos() {
        return servicePedido.listarPedidos();
    }

    @PutMapping("/{id}/status")
    public Pedido alterarStatus(@PathVariable String id, @RequestParam StatusPedido status, Authentication auth) {
        return servicePedido.atualizarStatus(id, status, auth);
    }

    @PutMapping("/{id}/desconto")
    public Pedido aplicarDescontoPedido(
            @PathVariable String id,
            @RequestParam double percentual,
            Authentication auth) {
        return servicePedido.aplicarDesconto(id, percentual, auth);
    }

    @PutMapping("/{id}/cancelamento")
    public Pedido cancelarPedidoPorId(
            @PathVariable String id,
            @RequestParam String justificativa,
            Authentication auth) {
        return servicePedido.cancelarPedido(id, justificativa, auth);
    }
}