package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.service.ProdutoService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {
    
    @Autowired
    private ProdutoService servicoProduto;
    
    @GetMapping
    public List<Produto> listarProdutos() {
        return servicoProduto.listarProdutos();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirProduto(@PathVariable String id) {
        servicoProduto.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Produto> salvarProduto(@RequestBody Produto dadosProduto) {
        Produto registoSalvo = servicoProduto.salvar(dadosProduto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registoSalvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> atualizarProduto(@PathVariable String id, @RequestBody Produto dadosProduto) {
        dadosProduto.setId(id);
        Produto registoAtualizado = servicoProduto.salvar(dadosProduto);
        return ResponseEntity.ok(registoAtualizado);
    }
}