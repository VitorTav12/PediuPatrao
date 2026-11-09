package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servicoUtilizador;

    public UsuarioController(UsuarioService servicoUtilizador) {
        this.servicoUtilizador = servicoUtilizador;
    }

    @GetMapping
    public ResponseEntity<Iterable<Usuario>> listarTodos() {
        Iterable<Usuario> listaUtilizadores = servicoUtilizador.listarTodos();
        return ResponseEntity.ok(listaUtilizadores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable String id) {
        Optional<Usuario> registoEncontrado = servicoUtilizador.buscarPorId(id);
        return registoEncontrado.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Usuario> criarUsuario(@RequestBody Usuario dadosUsuario) {
        Usuario registoCriado = servicoUtilizador.salvarUsuario(dadosUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(registoCriado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizarUsuario(@PathVariable String id, @RequestBody Usuario dadosAtualizados, Principal principalLogado) {
        return ResponseEntity.ok(servicoUtilizador.atualizarUsuario(id, dadosAtualizados, principalLogado.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable String id, Principal principalLogado) {
        servicoUtilizador.deletarUsuario(id, principalLogado.getName());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErroExcecao(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}