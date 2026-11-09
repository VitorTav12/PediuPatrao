package com.umc.pediupatrao.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = { HomeController.class, AuditoriaController.class })
public class UsuarioLogadoAdvice {

    @ModelAttribute("usuarioLogado")
    public String usuarioLogado(Authentication authentication) {
        if (!estaAutenticado(authentication)) {
            return null;
        }
        return authentication.getName();
    }

    @ModelAttribute("perfilLogado")
    public String perfilLogado(Authentication authentication) {
        if (!estaAutenticado(authentication)) {
            return null;
        }
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridade -> autoridade.startsWith("ROLE_"))
                .map(autoridade -> autoridade.substring(5))
                .findFirst()
                .orElse(null);
    }

    private boolean estaAutenticado(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}