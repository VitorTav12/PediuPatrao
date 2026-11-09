import com.umc.pediupatrao.PizzariaPedidosApplication;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.DashboardApiController;
import com.umc.pediupatrao.dto.DashboardDTO;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardApiController.class)
@ContextConfiguration(classes = { PizzariaPedidosApplication.class, SecurityConfig.class })
class DashboardApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private DashboardDTO.Resumo resumoExemplo() {
        return new DashboardDTO.Resumo(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3),
                4, 3, 1,
                190.0, 63.33, 10.0, 25.0
        );
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteConsultaOResumoComOsIndicadores() throws Exception {
        when(dashboardService.resumo(any(), any(), any())).thenReturn(resumoExemplo());

        mockMvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(4))
                .andExpect(jsonPath("$.faturamento").value(190.0))
                .andExpect(jsonPath("$.ticketMedio").value(63.33))
                .andExpect(jsonPath("$.percentualCancelamento").value(25.0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminTambemConsultaOResumo() throws Exception {
        when(dashboardService.resumo(any(), any(), any())).thenReturn(resumoExemplo());

        mockMvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void todosOsEndpointsDeConsultaRespondemOk() throws Exception {
        mockMvc.perform(get("/api/dashboard/vendas-por-dia")).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/vendas-por-hora")).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/pedidos-por-status")).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/produtos-mais-vendidos")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoAcessaOsDadosDoDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Acesso negado")));

        mockMvc.perform(get("/api/dashboard/produtos-mais-vendidos"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    void usuarioNaoAutenticadoEhRedirecionadoParaOLogin() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo").with(anonymous()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardNaoAceitaEscritaPelaApi() throws Exception {
        mockMvc.perform(post("/api/dashboard/resumo").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void filtrosDeDatasEStatusSaoRepassadosAoServico() throws Exception {
        when(dashboardService.resumo(any(), any(), any())).thenReturn(resumoExemplo());

        mockMvc.perform(get("/api/dashboard/resumo")
                        .param("de", "2026-11-01")
                        .param("ate", "2026-11-03")
                        .param("status", "FINALIZADO"))
                .andExpect(status().isOk());

        verify(dashboardService).resumo(
                eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 11, 3)),
                eq(StatusPedido.FINALIZADO)
        );
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void limiteDoRankingEhRepassadoAoServico() throws Exception {
        mockMvc.perform(get("/api/dashboard/produtos-mais-vendidos").param("limite", "3"))
                .andExpect(status().isOk());

        verify(dashboardService).produtosMaisVendidos(any(), any(), any(), eq(3));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void dataEmFormatoInvalidoRetornaBadRequest() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo").param("de", "01/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("de")));

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void statusInexistenteRetornaBadRequestComOsValoresAceitos() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo").param("status", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("FINALIZADO")));

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void regraDoServicoViolada_retornaBadRequestComMensagem() throws Exception {
        when(dashboardService.resumo(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("A data inicial não pode ser posterior à data final."));

        mockMvc.perform(get("/api/dashboard/resumo")
                        .param("de", "2026-11-05")
                        .param("ate", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("A data inicial não pode ser posterior à data final."));
    }
}