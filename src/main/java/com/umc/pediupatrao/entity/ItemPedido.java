package com.umc.pediupatrao.entity;

public class ItemPedido {
    
    private String idProduto;
    private String descricaoProduto;
    private int qtd;
    private double valorUnitario;

    public ItemPedido() {}

    public ItemPedido(String idRef, String nome, int quantidadeSelecionada, double preco) {
        this.idProduto = idRef;
        this.descricaoProduto = nome;
        this.qtd = quantidadeSelecionada;
        this.valorUnitario = preco;
    }

    public String getIdProduto() { return idProduto; }
    public void setIdProduto(String idProduto) { this.idProduto = idProduto; }

    public String getDescricaoProduto() { return descricaoProduto; }
    public void setDescricaoProduto(String descricaoProduto) { this.descricaoProduto = descricaoProduto; }

    public int getQtd() { return qtd; }
    public void setQtd(int qtd) { this.qtd = qtd; }

    public double getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(double valorUnitario) { this.valorUnitario = valorUnitario; }

    public double calcularValorTotalDoItem() {
        return this.valorUnitario * this.qtd;
    }
}