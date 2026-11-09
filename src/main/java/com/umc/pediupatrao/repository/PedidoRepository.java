package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {

    List<Pedido> findByClienteId(String clienteId);

    @Query("{ 'horarioEntrada' : { '$gte' : ?0, '$lt' : ?1 } }")
    List<Pedido> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fimExclusivo);

    @Query("{ 'horarioEntrada' : { '$gte' : ?0, '$lt' : ?1 }, 'status' : ?2 }")
    List<Pedido> buscarPorPeriodoEStatus(LocalDateTime inicio, LocalDateTime fimExclusivo, StatusPedido status);
}