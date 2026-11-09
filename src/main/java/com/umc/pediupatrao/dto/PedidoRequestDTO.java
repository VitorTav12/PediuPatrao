package com.umc.pediupatrao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PedidoRequestDTO {
    
    @JsonProperty("clienteId")
    private String identificadorCliente;
    
    @JsonProperty("itens")
    private List<ItemPedidoDTO> listaDeItens;

    public String getIdentificadorCliente() { return identificadorCliente; }
    public void setIdentificadorCliente(String identificadorCliente) { this.identificadorCliente = identificadorCliente; }

    public List<ItemPedidoDTO> getListaDeItens() { return listaDeItens; }
    public void setListaDeItens(List<ItemPedidoDTO> listaDeItens) { this.listaDeItens = listaDeItens; }
}