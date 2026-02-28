package com.revisaai.ingestion;

import com.revisaai.auth.oauth2.OAuth2FailureHandler;
import com.revisaai.auth.oauth2.OAuth2SuccessHandler;
import com.revisaai.auth.oauth2.OAuth2UserServiceImpl;
import com.revisaai.shared.security.JwtService;
import com.revisaai.shared.security.SecurityConfig;
import com.revisaai.shared.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IngestionController.class)
@Import(SecurityConfig.class)
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngestionService ingestionService;

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

    private static final byte[] PDF_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46};

    @Test
    @DisplayName("POST /ingestion/jobs sem autenticação retorna 403")
    void post_semAutenticacao_retorna403() throws Exception {
        mockMvc.perform(multipart("/ingestion/jobs")
                        .file(new MockMultipartFile("provaArquivo", "prova.pdf",
                                "application/pdf", PDF_BYTES))
                        .file(new MockMultipartFile("gabaritoArquivo", "gabarito.pdf",
                                "application/pdf", PDF_BYTES))
                        .param("banca", "CEBRASPE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /ingestion/jobs com arquivos autenticado retorna 201 com job")
    void post_comArquivos_autenticado_retorna201ComJob() throws Exception {
        var job = new IngestionJob();
        job.setStatus(IngestionStatus.COMPLETED);

        given(ingestionService.process(anyString(), any(), any(),
                any(), any(), any(), any()))
                .willReturn(job);

        mockMvc.perform(multipart("/ingestion/jobs")
                        .file(new MockMultipartFile("provaArquivo", "prova.pdf",
                                "application/pdf", PDF_BYTES))
                        .file(new MockMultipartFile("gabaritoArquivo", "gabarito.pdf",
                                "application/pdf", PDF_BYTES))
                        .param("banca", "CEBRASPE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /ingestion/jobs sem banca retorna 400")
    void post_semBanca_retorna400() throws Exception {
        mockMvc.perform(multipart("/ingestion/jobs")
                        .file(new MockMultipartFile("provaArquivo", "prova.pdf",
                                "application/pdf", PDF_BYTES))
                        .file(new MockMultipartFile("gabaritoArquivo", "gabarito.pdf",
                                "application/pdf", PDF_BYTES)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /ingestion/jobs sem fonte da prova retorna 400")
    void post_semFonteDaProva_retorna400() throws Exception {
        given(ingestionService.process(anyString(), any(), any(),
                any(), any(), any(), any()))
                .willThrow(new IllegalArgumentException("Fonte da prova obrigatória"));

        mockMvc.perform(multipart("/ingestion/jobs")
                        .file(new MockMultipartFile("gabaritoArquivo", "gabarito.pdf",
                                "application/pdf", PDF_BYTES))
                        .param("banca", "CEBRASPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /ingestion/jobs sem fonte do gabarito retorna 400")
    void post_semFonteDoGabarito_retorna400() throws Exception {
        given(ingestionService.process(anyString(), any(), any(),
                any(), any(), any(), any()))
                .willThrow(new IllegalArgumentException("Fonte do gabarito obrigatória"));

        mockMvc.perform(multipart("/ingestion/jobs")
                        .file(new MockMultipartFile("provaArquivo", "prova.pdf",
                                "application/pdf", PDF_BYTES))
                        .param("banca", "CEBRASPE"))
                .andExpect(status().isBadRequest());
    }
}
