package com.umc.pediupatrao.entity;

public enum StatusPedido {
    
    RECEBIDO("Pedido recebido pelo estabelecimento"),
    EM_PREPARO("Pedido em fase de preparação na cozinha"),
    PRONTO("Pedido finalizado e aguardando despacho"),
    SAIU_PARA_ENTREGA("Pedido em rota com o entregador"),
    RETIRADO("Pedido retirado no balcão pelo cliente"),
    FINALIZADO("Processo concluído e pedido entregue"),
    CANCELADO("Pedido cancelado");

    private final String descricaoAmigavel;

    StatusPedido(String descricaoAmigavel) {
        this.descricaoAmigavel = descricaoAmigavel;
    }

    public String getDescricaoAmigavel() {
        return this.descricaoAmigavel;
    }
}