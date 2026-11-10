package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@EnableMethodSecurity
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioDetailsService servicoUtilizadorDetalhes;

    public SecurityConfig(UsuarioDetailsService servicoUtilizadorDetalhes) {
        this.servicoUtilizadorDetalhes = servicoUtilizadorDetalhes;
    }

    @Bean
    public PasswordEncoder codificadorPalavraPasse() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider fornecedorAutenticacao() {
        DaoAuthenticationProvider provedorDao = new DaoAuthenticationProvider();
        provedorDao.setUserDetailsService(servicoUtilizadorDetalhes);
        provedorDao.setPasswordEncoder(codificadorPalavraPasse());
        return provedorDao;
    }

    @Bean
    public AuthenticationManager gestorAutenticacao(AuthenticationConfiguration configAutenticacao) throws Exception {
        return configAutenticacao.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain cadeiaFiltrosSeguranca(HttpSecurity httpSeguranca) throws Exception {
        httpSeguranca
                .csrf(csrfConfig -> csrfConfig
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .authorizeHttpRequests(autorizacoes -> autorizacoes
                        .requestMatchers("/login", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/usuarios/**", "/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/desconto", "/api/pedidos/*/cancelamento").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/api/clientes/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/clientes").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/clientes/novo", "/clientes/salvar", "/clientes/editar/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**", "/produtos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers("/api/produtos/**").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/logs/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/api/logs/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/auditoria").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/auditoria/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/api/dashboard/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/dashboard").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/dashboard/**").denyAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(tratamentoExcecao -> tratamentoExcecao
                        .accessDeniedHandler(new AcessoNegadoHandler())
                )
                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(manipuladorSucessoLogin())
                )
                .logout(terminoSessao -> terminoSessao
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return httpSeguranca.build();
    }

    @Bean
    public AuthenticationSuccessHandler manipuladorSucessoLogin() {
        return (requisicao, resposta, autenticacao) -> {
            resposta.sendRedirect("/");
        };
    }
}