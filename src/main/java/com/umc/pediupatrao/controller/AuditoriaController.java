package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.FiltroLog;
import com.umc.pediupatrao.dto.PaginaLogDTO;
import com.umc.pediupatrao.service.LogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
public class AuditoriaController {

    private static final int TAMANHO_PAGINA = 15;

    private final LogService logService;

    public AuditoriaController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public String auditoria(
            @RequestParam(defaultValue = "") String entidade,
            @RequestParam(defaultValue = "") String operacao,
            @RequestParam(defaultValue = "") String usuario,
            @RequestParam(defaultValue = "") String entidadeId,
            @RequestParam(defaultValue = "") String de,
            @RequestParam(defaultValue = "") String ate,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {

        LocalDate dataInicial = lerData(de);
        LocalDate dataFinal = lerData(ate);

        if ((!de.isBlank() && dataInicial == null) || (!ate.isBlank() && dataFinal == null)) {
            model.addAttribute("erro", "Data inválida. Use o formato AAAA-MM-DD.");
        } else if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            model.addAttribute("erro", "A data inicial não pode ser posterior à data final.");
            dataInicial = null;
            dataFinal = null;
        }

        FiltroLog filtro = new FiltroLog(entidade, entidadeId, usuario, operacao, dataInicial, dataFinal);
        PaginaLogDTO resultado = logService.consultar(filtro, pagina, TAMANHO_PAGINA);

        model.addAttribute("resultado", resultado);
        model.addAttribute("entidade", entidade);
        model.addAttribute("operacao", operacao);
        model.addAttribute("usuario", usuario);
        model.addAttribute("entidadeId", entidadeId);
        model.addAttribute("de", de);
        model.addAttribute("ate", ate);
        model.addAttribute("content", "auditoria :: content");

        return "layout";
    }

    private LocalDate lerData(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}