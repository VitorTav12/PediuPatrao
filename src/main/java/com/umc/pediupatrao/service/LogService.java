package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.FiltroLog;
import com.umc.pediupatrao.dto.LogDTO;
import com.umc.pediupatrao.dto.PaginaLogDTO;
import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.repository.LogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class LogService {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final LogRepository logRepository;
    private final MongoTemplate mongoTemplate;

    public LogService(LogRepository logRepository, MongoTemplate mongoTemplate) {
        this.logRepository = logRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Log registrar(Authentication authentication, String operacao, String entidade, String entidadeId, List<Log.DetalheAlteracao> alteracoes, String justificativa) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("É necessário identificar o usuário para registrar a auditoria.");
        }

        Log log = new Log();

        log.setUsuario(authentication.getName());
        log.setPerfil(obterPerfil(authentication));
        log.setMomentoRegistro(LocalDateTime.now());
        log.setTipoAcao(operacao);
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        log.setListaAlteracoes(alteracoes == null ? List.of() : alteracoes);
        log.setJustificativa(justificativa);

        return logRepository.save(log);
    }

    public PaginaLogDTO consultar(FiltroLog filtro, int pagina, int tamanho) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_PAGINA);

        Query consulta = new Query();

        if (filtro != null) {
            adicionarIgualdade(consulta, "entidade", filtro.entidade());
            adicionarIgualdade(consulta, "entidadeId", filtro.entidadeId());
            adicionarIgualdade(consulta, "operacao", filtro.operacao());

            if (temTexto(filtro.usuario())) {
                consulta.addCriteria(
                        Criteria.where("usuario").regex("^" + Pattern.quote(filtro.usuario().trim()) + "$", "i")
                );
            }

            if (filtro.de() != null || filtro.ate() != null) {
                Criteria periodo = Criteria.where("dataHora");

                if (filtro.de() != null) {
                    periodo = periodo.gte(filtro.de().atStartOfDay());
                }

                if (filtro.ate() != null) {
                    periodo = periodo.lte(filtro.ate().atTime(LocalTime.MAX));
                }

                consulta.addCriteria(periodo);
            }
        }

        long total = mongoTemplate.count(consulta, Log.class);

        consulta.with(Sort.by(Sort.Direction.DESC, "dataHora"));
        consulta.skip((long) paginaSegura * tamanhoSeguro);
        consulta.limit(tamanhoSeguro);

        List<LogDTO> itens = mongoTemplate.find(consulta, Log.class)
                .stream()
                .map(LogDTO::de)
                .toList();

        int totalPaginas = (int) Math.ceil((double) total / tamanhoSeguro);

        return new PaginaLogDTO(itens, paginaSegura, tamanhoSeguro, total, totalPaginas);
    }

    private void adicionarIgualdade(Query consulta, String campo, String valor) {
        if (temTexto(valor)) {
            consulta.addCriteria(Criteria.where(campo).is(valor.trim()));
        }
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String obterPerfil(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridade -> autoridade.startsWith("ROLE_"))
                .map(autoridade -> autoridade.substring(5))
                .findFirst()
                .orElse("NAO_IDENTIFICADO");
    }
}