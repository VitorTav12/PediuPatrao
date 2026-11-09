package com.umc.pediupatrao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemPedidoDTO {
    
    @JsonProperty("produtoId")
    private String codigoDoProduto;
    
    @JsonProperty("quantidade")
    private int qtdSelecionada;

    public String getCodigoDoProduto() { return codigoDoProduto; }
    public void setCodigoDoProduto(String codigoDoProduto) { this.codigoDoProduto = codigoDoProduto; }

    public int getQtdSelecionada() { return qtdSelecionada; }
    public void setQtdSelecionada(int qtdSelecionada) { this.qtdSelecionada = qtdSelecionada; }
}