import com.umc.pediupatrao.dto.LogDTO;
import com.umc.pediupatrao.entity.Log;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogDTOTest {

    private Log registroCom(String campo, String valorAnterior, String valorNovo) {
        Log log = new Log();
        log.setId("abc");
        log.setUsuario("atendente01");
        log.setPerfil("ATENDENTE");
        log.setMomentoRegistro(LocalDateTime.of(2026, 11, 7, 10, 30));
        log.setTipoAcao("ALTERACAO_CLIENTE");
        log.setEntidade("Cliente");
        log.setEntidadeId("123");
        log.setListaAlteracoes(List.of(new Log.DetalheAlteracao(campo, valorAnterior, valorNovo)));
        return log;
    }

    @Test
    void telefoneApareceMascaradoNaConsulta() {
        LogDTO dto = LogDTO.de(registroCom("telefone", "(11) 98765-4321", "11999998888"));

        assertEquals("*******4321", dto.alteracoes().get(0).valorAnterior());
        assertEquals("*******8888", dto.alteracoes().get(0).valorNovo());
    }

    @Test
    void telefoneCurtoFicaTotalmenteMascarado() {
        assertEquals("****", LogDTO.mascarar("telefone", "123"));
    }

    @Test
    void camposNaoSensiveisContinuamVisiveis() {
        LogDTO dto = LogDTO.de(registroCom("logradouro", "Rua A", "Rua B"));

        assertEquals("Rua A", dto.alteracoes().get(0).valorAnterior());
        assertEquals("Rua B", dto.alteracoes().get(0).valorNovo());
    }

    @Test
    void valorNuloContinuaNulo() {
        LogDTO dto = LogDTO.de(registroCom("telefone", null, "11999998888"));

        assertNull(dto.alteracoes().get(0).valorAnterior());
    }

    @Test
    void dadosDoRegistroSaoCopiados() {
        LogDTO dto = LogDTO.de(registroCom("nome", "Joao", "Joao Silva"));

        assertEquals("atendente01", dto.usuario());
        assertEquals("ATENDENTE", dto.perfil());
        assertEquals("ALTERACAO_CLIENTE", dto.operacao());
        assertEquals("Cliente", dto.entidade());
        assertEquals("123", dto.entidadeId());
        assertEquals("Joao Silva", dto.alteracoes().get(0).valorNovo());
    }
}