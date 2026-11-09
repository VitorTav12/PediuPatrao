package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteService servicoCliente;

    @GetMapping
    public List<Cliente> listarClientes(@RequestParam(required = false) String nome) {
        List<Cliente> listaClientes = servicoCliente.listarClientes();
        
        if (nome != null && !nome.isBlank()) {
            String filtroTexto = nome.toLowerCase().trim();
            return listaClientes.stream()
                    .filter(c -> c.getNome() != null && c.getNome().toLowerCase().contains(filtroTexto))
                    .collect(Collectors.toList());
        }
        
        return listaClientes;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCliente(@PathVariable String id, Authentication authContexto) {
        servicoCliente.excluir(id, authContexto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Cliente> salvarCliente(@RequestBody Cliente dadosCliente, Authentication authContexto) {
        Cliente registoSalvo = servicoCliente.salvar(dadosCliente, authContexto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registoSalvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizarCliente(@PathVariable String id, @RequestBody Cliente dadosCliente, Authentication authContexto) {
        dadosCliente.setId(id);
        Cliente registoAtualizado = servicoCliente.salvar(dadosCliente, authContexto);
        return ResponseEntity.ok(registoAtualizado);
    }
}