package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Localiza um registo de utilizador pelo respetivo identificador único
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Localiza um utilizador com base no nome de acesso (username)
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Regista um novo utilizador aplicando hash de segurança na senha
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario salvarUsuario(Usuario usuario) {
        validarCamposObrigatorios(usuario);
        
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }
        
        String nomeUtilizador = usuario.getUsername().trim();

        if (usuarioRepository.existsByUsername(nomeUtilizador)) {
            throw new IllegalArgumentException("Já existe um usuário com esse username.");
        }
        
        usuario.setId(null);
        usuario.setUsername(nomeUtilizador);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        
        return usuarioRepository.save(usuario);
    }

    // Remove um utilizador da base de dados com validações de autoproteção
    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id, String usernameLogado) {
        Usuario usuarioAlvo = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
                
        if (usuarioAlvo.getUsername().equals(usernameLogado)) {
            throw new IllegalArgumentException("Você não pode excluir o seu próprio usuário.");
        }
        
        usuarioRepository.deleteById(id);
    }

    // Retorna todos os utilizadores registados no sistema
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza os dados cadastrais de um utilizador existente
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario atualizarUsuario(String id, Usuario usuarioAtualizado, String usernameLogado) {
        Usuario usuarioEncontrado = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
                
        validarCamposObrigatorios(usuarioAtualizado);
        
        boolean alteracaoDePerfil = usuarioEncontrado.getRole() != usuarioAtualizado.getRole();

        if (alteracaoDePerfil && usuarioEncontrado.getUsername().equals(usernameLogado)) {
            throw new IllegalArgumentException("Você não pode alterar o seu próprio perfil.");
        }

        String novoNomeUsuario = usuarioAtualizado.getUsername().trim();

        if (!usuarioEncontrado.getUsername().equals(novoNomeUsuario) && usuarioRepository.existsByUsername(novoNomeUsuario)) {
            throw new IllegalArgumentException("Já existe um usuário com esse username.");
        }

        usuarioEncontrado.setUsername(novoNomeUsuario);
        usuarioEncontrado.setRole(usuarioAtualizado.getRole());

        // Atualiza a palavra-passe apenas se foi fornecida uma nova
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            usuarioEncontrado.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        return usuarioRepository.save(usuarioEncontrado);
    }

    // Método auxiliar privado para validação de integridade dos dados
    private void validarCamposObrigatorios(Usuario usuario) {
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new IllegalArgumentException("O username é obrigatório.");
        }
        if (usuario.getRole() == null) {
            throw new IllegalArgumentException("O perfil é obrigatório.");
        }
    }
}