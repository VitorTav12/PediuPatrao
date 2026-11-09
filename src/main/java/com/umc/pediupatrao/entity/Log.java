package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "log")
public class Log {
    
    @Id
    private String id;
    private String usuario;
    private String perfil;
    
    @Field("dataHora")
    private LocalDateTime momentoRegistro;
    
    @Field("operacao")
    private String tipoAcao;
    
    private String entidade;
    private String entidadeId;
    private String justificativa;

    @Field("alteracoes")
    private List<DetalheAlteracao> listaAlteracoes = new ArrayList<>();

    public Log() {
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public LocalDateTime getMomentoRegistro() { return momentoRegistro; }
    public void setMomentoRegistro(LocalDateTime momentoRegistro) { this.momentoRegistro = momentoRegistro; }

    public String getTipoAcao() { return tipoAcao; }
    public void setTipoAcao(String tipoAcao) { this.tipoAcao = tipoAcao; }

    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }

    public String getEntidadeId() { return entidadeId; }
    public void setEntidadeId(String entidadeId) { this.entidadeId = entidadeId; }

    public List<DetalheAlteracao> getListaAlteracoes() { return listaAlteracoes; }
    public void setListaAlteracoes(List<DetalheAlteracao> listaAlteracoes) { this.listaAlteracoes = listaAlteracoes; }

    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }

    // Classe interna renomeada
    public static class DetalheAlteracao {
        private String campo;
        private String valorAnterior;
        private String valorNovo;

        public DetalheAlteracao() {}

        public DetalheAlteracao(String nomeDoCampo, String valorAntigo, String valorAtualizado) {
            this.campo = nomeDoCampo;
            this.valorAnterior = valorAntigo;
            this.valorNovo = valorAtualizado;
        }

        public String getCampo() { return campo; }
        public void setCampo(String campo) { this.campo = campo; }

        public String getValorAnterior() { return valorAnterior; }
        public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }

        public String getValorNovo() { return valorNovo; }
        public void setValorNovo(String valorNovo) { this.valorNovo = valorNovo; }
    }
}