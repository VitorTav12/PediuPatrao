import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.LogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private LogService logService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ClienteService clienteService;

    @Captor
    private ArgumentCaptor<List<Log.DetalheAlteracao>> alteracoesCaptor;

    private List<String> nomesDosCampos(List<Log.DetalheAlteracao> alteracoes) {
        return alteracoes.stream().map(Log.DetalheAlteracao::getCampo).toList();
    }

    @Test
    void criacaoRegistraTodosOsCamposPreenchidos() {
        Cliente cliente = new Cliente();
        cliente.setNome("Joao");
        cliente.setTelefone("11987654321");
        cliente.setLogradouro("Rua A");

        when(clienteRepository.save(cliente)).thenAnswer(invocacao -> {
            cliente.setId("c1");
            return cliente;
        });

        clienteService.salvar(cliente, authentication);

        verify(logService).registrar(
                eq(authentication),
                eq("CRIACAO_CLIENTE"),
                eq("Cliente"),
                eq("c1"),
                alteracoesCaptor.capture(),
                isNull()
        );

        List<Log.DetalheAlteracao> alteracoes = alteracoesCaptor.getValue();

        assertEquals(List.of("nome", "telefone", "logradouro"), nomesDosCampos(alteracoes));
        assertNull(alteracoes.get(0).getValorAnterior());
        assertEquals("Joao", alteracoes.get(0).getValorNovo());
        assertEquals("Rua A", alteracoes.get(2).getValorNovo());
    }

    @Test
    void atualizacaoRegistraSomenteOsCamposAlterados() {
        Cliente anterior = new Cliente();
        anterior.setId("c1");
        anterior.setNome("Joao");
        anterior.setTelefone("1111");

        Cliente novo = new Cliente();
        novo.setId("c1");
        novo.setNome("Joao");
        novo.setTelefone("2222");

        when(clienteRepository.findById("c1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(novo)).thenReturn(novo);

        clienteService.salvar(novo, authentication);

        verify(logService).registrar(
                eq(authentication),
                eq("ALTERACAO_CLIENTE"),
                eq("Cliente"),
                eq("c1"),
                alteracoesCaptor.capture(),
                isNull()
        );

        List<Log.DetalheAlteracao> alteracoes = alteracoesCaptor.getValue();

        assertEquals(List.of("telefone"), nomesDosCampos(alteracoes));
        assertEquals("1111", alteracoes.get(0).getValorAnterior());
        assertEquals("2222", alteracoes.get(0).getValorNovo());
    }

    @Test
    void atualizacaoSemMudancaNaoGeraRegistroDeAuditoria() {
        Cliente anterior = new Cliente();
        anterior.setId("c1");
        anterior.setNome("Joao");

        Cliente novo = new Cliente();
        novo.setId("c1");
        novo.setNome("Joao");

        when(clienteRepository.findById("c1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(novo)).thenReturn(novo);

        clienteService.salvar(novo, authentication);

        verify(logService, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void exclusaoRemoveOClienteERegistraOsDadosAnteriores() {
        Cliente existente = new Cliente();
        existente.setId("c1");
        existente.setNome("Joao");
        existente.setTelefone("1111");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(existente));

        clienteService.excluir("c1", authentication);

        verify(clienteRepository).deleteById("c1");
        verify(logService).registrar(
                eq(authentication),
                eq("EXCLUSAO_CLIENTE"),
                eq("Cliente"),
                eq("c1"),
                alteracoesCaptor.capture(),
                isNull()
        );

        List<Log.DetalheAlteracao> alteracoes = alteracoesCaptor.getValue();

        assertEquals(List.of("nome", "telefone"), nomesDosCampos(alteracoes));
        assertEquals("Joao", alteracoes.get(0).getValorAnterior());
        assertNull(alteracoes.get(0).getValorNovo());
    }

    @Test
    void exclusaoDeClienteInexistenteNaoApagaNemRegistraAuditoria() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(clienteRepository.findById("x")).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> clienteService.excluir("x", authentication)
        );

        verify(clienteRepository, never()).deleteById(any());
        verify(logService, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void exclusaoSemUsuarioIdentificadoEhBloqueada() {
        assertThrows(
                SecurityException.class,
                () -> clienteService.excluir("c1", null)
        );

        verify(clienteRepository, never()).findById(any());
        verify(clienteRepository, never()).deleteById(any());
    }
}