package com.umc.pediupatrao.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;

public class AcessoNegadoHandler implements AccessDeniedHandler {

    public static final String MENSAGEM_ACESSO_NEGADO =
            "Acesso negado: seu perfil não tem permissão para executar esta operação.";

    public static final String MENSAGEM_CSRF =
            "Requisição bloqueada: token CSRF ausente ou inválido. Recarregue a página e tente novamente.";

    private static final Logger LOG = LoggerFactory.getLogger(AcessoNegadoHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        boolean falhaCsrf = accessDeniedException instanceof CsrfException;
        String mensagem = falhaCsrf ? MENSAGEM_CSRF : MENSAGEM_ACESSO_NEGADO;

        registrar(request, falhaCsrf ? "token CSRF ausente ou invalido" : "perfil sem permissao");

        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(mensagem);
            return;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, mensagem);
    }

    public static void registrar(HttpServletRequest request, String motivo) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        String usuario = autenticacao != null ? autenticacao.getName() : "anonimo";
        String perfis = autenticacao != null ? autenticacao.getAuthorities().toString() : "[]";

        LOG.warn("Acesso negado: usuario={} perfis={} metodo={} caminho={} motivo={}",
                limpar(usuario),
                limpar(perfis),
                limpar(request.getMethod()),
                limpar(request.getRequestURI()),
                limpar(motivo));
    }

    private static String limpar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("[\\r\\n\\t]", "_");
    }
}