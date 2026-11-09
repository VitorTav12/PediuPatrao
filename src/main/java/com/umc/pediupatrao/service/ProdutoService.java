package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    // Regista um novo artigo de produto diretamente na base de dados
    public Produto novoProduto(Produto itemProduto) {
        return produtoRepository.save(itemProduto);
    }

    // Retorna a listagem completa de todos os produtos registados
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    // Remove um registo de produto com base no respetivo ID único
    public void excluir(String idProduto) {
        produtoRepository.deleteById(idProduto);
    }

    // Executa a operação de inserção ou atualização de um produto de forma segura
    public Produto salvar(Produto itemProduto) {
        if (itemProduto.getId() == null) {
            return produtoRepository.save(itemProduto);
        } else {
            if (produtoRepository.existsById(itemProduto.getId())) {
                return produtoRepository.save(itemProduto);
            } else {
                throw new IllegalArgumentException("Cliente não encontrado para atualização.");
            }
        }
    }
}