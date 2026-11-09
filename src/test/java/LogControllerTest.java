import com.umc.pediupatrao.PizzariaPedidosApplication;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.LogController;
import com.umc.pediupatrao.dto.FiltroLog;
import com.umc.pediupatrao.dto.PaginaLogDTO;
import com.umc.pediupatrao.service.LogService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogController.class)
@ContextConfiguration(classes = { PizzariaPedidosApplication.class, SecurityConfig.class })
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogService logService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private PaginaLogDTO paginaVazia() {
        return new PaginaLogDTO(List.of(), 0, 20, 0, 0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeConsultarLogs() throws Exception {
        when(logService.consultar(any(FiltroLog.class), anyInt(), anyInt())).thenReturn(paginaVazia());

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk());

        verify(logService).consultar(any(FiltroLog.class), eq(0), eq(20));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePodeConsultarLogs() throws Exception {
        when(logService.consultar(any(FiltroLog.class), anyInt(), anyInt())).thenReturn(paginaVazia());

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk());

        verify(logService).consultar(any(FiltroLog.class), eq(0), eq(20));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void filtrosDaConsultaSaoRepassadosAoServico() throws Exception {
        when(logService.consultar(any(FiltroLog.class), anyInt(), anyInt())).thenReturn(paginaVazia());

        mockMvc.perform(get("/api/logs")
                        .param("entidade", "Cliente")
                        .param("usuario", "atendente01")
                        .param("de", "2026-11-01"))
                .andExpect(status().isOk());

        verify(logService).consultar(
                argThat(filtro -> "Cliente".equals(filtro.entidade())
                        && "atendente01".equals(filtro.usuario())
                        && LocalDate.of(2026, 11, 1).equals(filtro.de())),
                eq(0),
                eq(20)
        );
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void dataInvalidaRetornaBadRequest() throws Exception {
        mockMvc.perform(get("/api/logs").param("de", "31-12-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("de")));

        verifyNoInteractions(logService);
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeConsultarLogs() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(logService);
    }

    @Test
    @WithMockUser(roles = "ENTREGADOR")
    void entregadorNaoPodeConsultarLogs() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNaoAutenticadoNaoPodeConsultarLogs() throws Exception {
        mockMvc.perform(get("/api/logs").with(anonymous()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ninguemPodeCriarRegistroDeAuditoriaPelaApi() throws Exception {
        mockMvc.perform(post("/api/logs").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(logService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ninguemPodeAlterarRegistroDeAuditoriaPelaApi() throws Exception {
        mockMvc.perform(put("/api/logs/1").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(logService);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void ninguemPodeExcluirRegistroDeAuditoriaPelaApi() throws Exception {
        mockMvc.perform(delete("/api/logs/1").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(logService);
    }
}