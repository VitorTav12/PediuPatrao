import com.umc.pediupatrao.PizzariaPedidosApplication;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.ClienteController;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@ContextConfiguration(classes = { PizzariaPedidosApplication.class, SecurityConfig.class })
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteExcluiClienteEOServicoRecebeOUsuarioAutenticado() throws Exception {
        mockMvc.perform(delete("/api/clientes/c1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(clienteService).excluir(eq("c1"), any());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeExcluirCliente() throws Exception {
        mockMvc.perform(delete("/api/clientes/c1").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Acesso negado")));

        verifyNoInteractions(clienteService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoPodeExcluirCliente() throws Exception {
        mockMvc.perform(delete("/api/clientes/c1").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(clienteService);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void exclusaoSemTokenCsrfEhBloqueadaComMensagem() throws Exception {
        mockMvc.perform(delete("/api/clientes/c1"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("CSRF")));

        verifyNoInteractions(clienteService);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void exclusaoDeClienteInexistenteRetornaBadRequestComMensagem() throws Exception {
        doThrow(new IllegalArgumentException("Cliente não encontrado para exclusão."))
                .when(clienteService)
                .excluir(eq("x"), any());

        mockMvc.perform(delete("/api/clientes/x").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cliente não encontrado para exclusão."));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendentePodeCadastrarCliente() throws Exception {
        when(clienteService.salvar(any(Cliente.class), any())).thenReturn(new Cliente());

        mockMvc.perform(post("/api/clientes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Joao\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoPodeCadastrarCliente() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Joao\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(clienteService);
    }
}