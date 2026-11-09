package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.AcessoNegadoHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestControllerAdvice(annotations = RestController.class)
public class TratadorErrosApi {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarRegraDeNegocio(IllegalArgumentException e) {
        return resposta(HttpStatus.BAD_REQUEST, mensagemOuPadrao(e.getMessage(), "Requisição inválida."));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> tratarSeguranca(SecurityException e, HttpServletRequest request) {
        AcessoNegadoHandler.registrar(request, e.getMessage());
        return resposta(HttpStatus.FORBIDDEN, mensagemOuPadrao(e.getMessage(), AcessoNegadoHandler.MENSAGEM_ACESSO_NEGADO));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> tratarAcessoNegado(AccessDeniedException e, HttpServletRequest request) {
        AcessoNegadoHandler.registrar(request, "perfil sem permissao");
        return resposta(HttpStatus.FORBIDDEN, AcessoNegadoHandler.MENSAGEM_ACESSO_NEGADO);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> tratarParametroInvalido(MethodArgumentTypeMismatchException e) {
        Class<?> tipo = e.getRequiredType();

        if (tipo != null && tipo.isEnum()) {
            return resposta(
                    HttpStatus.BAD_REQUEST,
                    "Valor inválido para o parâmetro " + e.getName() + ". Valores aceitos: " + Arrays.toString(tipo.getEnumConstants())
            );
        }

        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro: " + e.getName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> tratarParametroAusente(MissingServletRequestParameterException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> tratarCorpoInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.");
    }

    private ResponseEntity<String> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity
                .status(status)
                .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                .body(mensagem);
    }

    private String mensagemOuPadrao(String mensagem, String padrao) {
        return mensagem == null || mensagem.isBlank() ? padrao : mensagem;
    }
}