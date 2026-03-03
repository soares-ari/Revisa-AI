package com.revisaai.user;

import com.revisaai.auth.oauth2.OAuth2FailureHandler;
import com.revisaai.auth.oauth2.OAuth2SuccessHandler;
import com.revisaai.auth.oauth2.OAuth2UserServiceImpl;
import com.revisaai.shared.security.JwtService;
import com.revisaai.shared.security.SecurityConfig;
import com.revisaai.shared.security.UserDetailsServiceImpl;
import com.revisaai.study.Resultado;
import com.revisaai.study.SessionModo;
import com.revisaai.user.dto.SessionSummary;
import com.revisaai.user.dto.UserStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private OAuth2UserServiceImpl oauth2UserService;

    @MockBean
    private OAuth2SuccessHandler oauth2SuccessHandler;

    @MockBean
    private OAuth2FailureHandler oauth2FailureHandler;

    @Test
    @DisplayName("GET /users/me/stats sem autenticação retorna 403")
    void getStats_semAutenticacao_retorna403() throws Exception {
        mockMvc.perform(get("/users/me/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "userId1")
    @DisplayName("GET /users/me/stats autenticado retorna 200 com UserStatsResponse")
    void getStats_autenticado_retorna200ComUserStatsResponse() throws Exception {
        var stats = new UserStatsResponse(40, 75.0, 2, Map.of("Informática", 75.0));
        given(userService.getStats("userId1")).willReturn(stats);

        mockMvc.perform(get("/users/me/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuestoes").value(40))
                .andExpect(jsonPath("$.percentualAcertos").value(75.0))
                .andExpect(jsonPath("$.totalSessoes").value(2))
                .andExpect(jsonPath("$.desempenhoPorArea['Informática']").value(75.0));
    }

    @Test
    @DisplayName("GET /users/me/history sem autenticação retorna 403")
    void getHistory_semAutenticacao_retorna403() throws Exception {
        mockMvc.perform(get("/users/me/history"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "userId1")
    @DisplayName("GET /users/me/history autenticado retorna 200 com lista de sessões")
    void getHistory_autenticado_retorna200ComListaSessoes() throws Exception {
        var summary = new SessionSummary(
                "s1", null, List.of("Informática"), 10, SessionModo.ESTUDO,
                new Resultado(10, 7, 70.0, Map.of("Informática", 70.0)),
                Instant.parse("2026-01-15T10:00:00Z")
        );
        given(userService.getHistory("userId1")).willReturn(List.of(summary));

        mockMvc.perform(get("/users/me/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("s1"))
                .andExpect(jsonPath("$[0].quantidade").value(10))
                .andExpect(jsonPath("$[0].resultado.acertos").value(7));
    }
}
