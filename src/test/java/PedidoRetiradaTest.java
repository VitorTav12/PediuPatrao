import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.LogService;
import com.umc.pediupatrao.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoRetiradaTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private LogService logService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PedidoService pedidoService;

    @BeforeEach
    void configurarAutenticacao() {
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("atendente01");
    }

    @Test
    void registrarRetiradaComResponsavelEHorario() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.PRONTO);

        when(pedidoRepository.findById("123")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido atualizado = pedidoService.atualizarStatus("123", StatusPedido.RETIRADO, authentication);

        assertEquals(StatusPedido.RETIRADO, atualizado.getStatus());
        assertEquals("atendente01", atualizado.getResponsavelSaidaId());
        assertNotNull(atualizado.getHorarioSaida());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void finalizarPedidoRetirado() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.RETIRADO);

        when(pedidoRepository.findById("123")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido atualizado = pedidoService.atualizarStatus("123", StatusPedido.FINALIZADO, authentication);

        assertEquals(StatusPedido.FINALIZADO, atualizado.getStatus());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void naoPermitirRetiradaSemEstarPronto() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.EM_PREPARO);

        when(pedidoRepository.findById("123")).thenReturn(Optional.of(pedido));

        assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus("123", StatusPedido.RETIRADO, authentication)
        );

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoPermitirRetiradaDepoisDeSairParaEntrega() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.SAIU_PARA_ENTREGA);

        when(pedidoRepository.findById("123")).thenReturn(Optional.of(pedido));

        assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus("123", StatusPedido.RETIRADO, authentication)
        );

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void impedirDescontoAposRetirada() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.RETIRADO);
        pedido.setItens(List.of(new ItemPedido("produto1", "Pizza", 2, 50.0)));

        doReturn(List.of(new SimpleGrantedAuthority("ROLE_GERENTE")))
                .when(authentication)
                .getAuthorities();

        when(pedidoRepository.findById("123")).thenReturn(Optional.of(pedido));

        assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.aplicarDesconto("123", 10.0, authentication)
        );

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void impedirCancelamentoAposRetirada() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.RETIRADO);

        doReturn(List.of(new SimpleGrantedAuthority("ROLE_GERENTE")))
                .when(authentication)
                .getAuthorities();

        when(pedidoRepository.findById("123")).thenReturn(Optional.of(pedido));

        assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.cancelarPedido("123", "cliente desistiu", authentication)
        );

        verify(pedidoRepository, never()).save(any());
    }
}