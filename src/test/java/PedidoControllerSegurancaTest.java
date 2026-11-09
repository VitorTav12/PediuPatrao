import com.umc.pediupatrao.PizzariaPedidosApplication;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.PedidoController;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoController.class)
@ContextConfiguration(classes = { PizzariaPedidosApplication.class, SecurityConfig.class })
class PedidoControllerSegurancaTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService pedidoService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeAplicarDesconto() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/desconto").param("percentual", "10").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Acesso negado")));

        verify(pedidoService, never()).aplicarDesconto(any(), anyDouble(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoPodeCancelarPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/cancelamento").param("justificativa", "teste").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Acesso negado")));

        verify(pedidoService, never()).cancelarPedido(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void regraDeNegocioViolada_retornaBadRequestComMensagem() throws Exception {
        when(pedidoService.aplicarDesconto(eq("1"), eq(30.0), any()))
                .thenThrow(new IllegalArgumentException("O desconto deve ser maior que zero e de, no máximo, 20%"));

        mockMvc.perform(put("/api/pedidos/1/desconto").param("percentual", "30").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("O desconto deve ser maior que zero e de, no máximo, 20%"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void requisicaoSemTokenCsrf_ehBloqueadaComMensagem() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/desconto").param("percentual", "10"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("CSRF")));

        verify(pedidoService, never()).aplicarDesconto(any(), anyDouble(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void statusInvalido_retornaBadRequest() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/status").param("status", "INVALIDO").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("status")));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendentePodeAvancarStatus() throws Exception {
        when(pedidoService.atualizarStatus(eq("1"), eq(StatusPedido.EM_PREPARO), any()))
                .thenReturn(new Pedido());

        mockMvc.perform(put("/api/pedidos/1/status").param("status", "EM_PREPARO").with(csrf()))
                .andExpect(status().isOk());
    }
}