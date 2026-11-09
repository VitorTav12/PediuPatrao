import com.umc.pediupatrao.dto.ItemPedidoDTO;
import com.umc.pediupatrao.dto.PedidoRequestDTO;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.LogService;
import com.umc.pediupatrao.service.PedidoService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoCriacaoAuditoriaTest {

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

    @Captor
    private ArgumentCaptor<List<Log.DetalheAlteracao>> alteracoesCaptor;

    private Produto produto(String id, String nome, double preco) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }

    private ItemPedidoDTO item(String produtoId, int quantidade) {
        ItemPedidoDTO item = new ItemPedidoDTO();
        item.setCodigoDoProduto(produtoId);
        item.setQtdSelecionada(quantidade);
        return item;
    }

    @Test
    void criacaoDePedidoRegistraClienteItensTotalResponsavelEStatus() {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setIdentificadorCliente("cliente1");
        dto.setListaDeItens(List.of(item("produto1", 2), item("produto2", 1)));

        when(clienteRepository.findById("cliente1")).thenReturn(Optional.of(new Cliente()));
        when(produtoRepository.findById("produto1")).thenReturn(Optional.of(produto("produto1", "Pizza", 30.0)));
        when(produtoRepository.findById("produto2")).thenReturn(Optional.of(produto("produto2", "Refrigerante", 10.0)));
        when(authentication.getName()).thenReturn("atendente01");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        pedidoService.criarPedido(dto, authentication);

        verify(logService).registrar(
                eq(authentication),
                eq("CRIACAO_PEDIDO"),
                eq("Pedido"),
                any(),
                alteracoesCaptor.capture(),
                isNull()
        );

        List<Log.DetalheAlteracao> alteracoes = alteracoesCaptor.getValue();

        assertEquals(
                List.of("clienteId", "itens", "valorTotal", "responsavel", "status"),
                alteracoes.stream().map(Log.DetalheAlteracao::getCampo).toList()
        );
        assertEquals("cliente1", alteracoes.get(0).getValorNovo());
        assertEquals("2x Pizza @ 30.0; 1x Refrigerante @ 10.0", alteracoes.get(1).getValorNovo());
        assertEquals("70.0", alteracoes.get(2).getValorNovo());
        assertEquals("atendente01", alteracoes.get(3).getValorNovo());
        assertEquals("RECEBIDO", alteracoes.get(4).getValorNovo());
    }
}