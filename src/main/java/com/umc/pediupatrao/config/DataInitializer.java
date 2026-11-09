package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner carregarDadosIniciais(UsuarioRepository usuarioRepository) {
        return args -> {
            if (!usuarioRepository.existsByRole(Perfil.ADMIN)) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

                Usuario administrador = new Usuario();
                administrador.setUsername("admin");
                administrador.setPassword(passwordEncoder.encode("teste123"));
                administrador.setRole(Perfil.ADMIN);

                usuarioRepository.save(administrador);

                System.out.println("-> Perfil de Administrador padrão injetado com sucesso no banco de dados.");
            }
        };
    }
}