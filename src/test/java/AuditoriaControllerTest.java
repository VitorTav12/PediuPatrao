import com.umc.pediupatrao.PizzariaPedidosApplication;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.AuditoriaController;
import com.umc.pediupatrao.dto.FiltroLog;
import com.umc.pediupatrao.dto.PaginaLogDTO;
import com.umc.pediupatrao.service.LogService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditoriaController.class)
@ContextConfiguration(classes = { PizzariaPedidosApplication.class, SecurityConfig.class })
class AuditoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogService logService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @BeforeEach
    void configurarServico() {
        when(logService.consultar(any(FiltroLog.class), anyInt(), anyInt()))
                .thenReturn(new PaginaLogDTO(List.of(), 0, 15, 0, 0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAcessaATelaDeAuditoria() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Auditoria")));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteAcessaATelaDeAuditoria() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Auditoria")));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoAcessaATelaDeAuditoria() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(logService);
    }

    @Test
    void usuarioNaoAutenticadoEhRedirecionadoParaOLogin() throws Exception {
        mockMvc.perform(get("/auditoria").with(anonymous()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void telaDeAuditoriaNaoAceitaEnvioDeDados() throws Exception {
        mockMvc.perform(post("/auditoria").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(logService);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void filtrosDaTelaSaoRepassadosAoServico() throws Exception {
        mockMvc.perform(get("/auditoria")
                        .param("entidade", "Pedido")
                        .param("de", "2026-11-01"))
                .andExpect(status().isOk());

        verify(logService).consultar(
                argThat(filtro -> "Pedido".equals(filtro.entidade())
                        && LocalDate.of(2026, 11, 1).equals(filtro.de())),
                eq(0),
                eq(15)
        );
    }
}