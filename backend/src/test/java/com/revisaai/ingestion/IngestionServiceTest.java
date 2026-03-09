package com.revisaai.ingestion;

import com.revisaai.question.Banca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private IngestionJobRepository repository;

    @Mock
    private ProvaRepository provaRepository;

    @Mock
    private DocumentDownloader downloader;

    @Mock
    private PdfTextExtractor extractor;

    @Mock
    private QuestionParserService questionParserService;

    private IngestionService service;

    private static final byte[] PDF_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF header

    @BeforeEach
    void setUp() {
        service = new IngestionService(repository, provaRepository, downloader, extractor, questionParserService);
        lenient().when(questionParserService.parse(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ParseResult(0, 0));
    }

    @Test
    @DisplayName("process com dois arquivos retorna job COMPLETED")
    void process_comDoisArquivos_retornaJobCompleted() throws IOException {
        var provaFile = new MockMultipartFile("provaArquivo", "prova.pdf",
                "application/pdf", PDF_BYTES);
        var gabaritoFile = new MockMultipartFile("gabaritoArquivo", "gabarito.pdf",
                "application/pdf", PDF_BYTES);

        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(extractor.extract(PDF_BYTES)).willReturn("texto extraído");

        var job = service.process("CEBRASPE", null, "Órgão", null,
                provaFile, null, gabaritoFile, null);

        assertThat(job.getStatus()).isEqualTo(IngestionStatus.COMPLETED);
        assertThat(job.getBanca()).isEqualTo(Banca.CEBRASPE);
        assertThat(job.getTextProva()).isEqualTo("texto extraído");
        assertThat(job.getTextGabarito()).isEqualTo("texto extraído");
        verify(downloader, never()).download(anyString());
        verify(extractor, times(2)).extract(PDF_BYTES);
    }

    @Test
    @DisplayName("process com duas URLs baixa e extrai")
    void process_comDuasUrls_baixaEExtrai() throws IOException {
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(downloader.download("http://prova.pdf")).willReturn(PDF_BYTES);
        given(downloader.download("http://gabarito.pdf")).willReturn(PDF_BYTES);
        given(extractor.extract(PDF_BYTES)).willReturn("texto via url");

        var job = service.process("FGV", 2023, "Órgão", "Analista",
                null, "http://prova.pdf", null, "http://gabarito.pdf");

        assertThat(job.getStatus()).isEqualTo(IngestionStatus.COMPLETED);
        assertThat(job.getBanca()).isEqualTo(Banca.FGV);
        assertThat(job.getAno()).isEqualTo(2023);
        assertThat(job.getCargo()).isEqualTo("Analista");
        verify(downloader).download("http://prova.pdf");
        verify(downloader).download("http://gabarito.pdf");
    }

    @Test
    @DisplayName("process misto arquivo e URL processa ambos")
    void process_mistoArquivoEUrl_processaAmbos() throws IOException {
        var provaFile = new MockMultipartFile("provaArquivo", "prova.pdf",
                "application/pdf", PDF_BYTES);

        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(downloader.download("http://gabarito.pdf")).willReturn(PDF_BYTES);
        given(extractor.extract(PDF_BYTES)).willReturn("texto");

        var job = service.process("CESGRANRIO", null, "Órgão", null,
                provaFile, null, null, "http://gabarito.pdf");

        assertThat(job.getStatus()).isEqualTo(IngestionStatus.COMPLETED);
        verify(downloader, never()).download("http://prova.pdf");
        verify(downloader).download("http://gabarito.pdf");
    }

    @Test
    @DisplayName("process sem fonte da prova lança IllegalArgumentException")
    void process_semFonteDaProva_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                service.process("CEBRASPE", null, "Órgão", null,
                        null, null, null, "http://gabarito.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prova");
    }

    @Test
    @DisplayName("process sem fonte do gabarito lança IllegalArgumentException")
    void process_semFonteDoGabarito_throwsIllegalArgumentException() {
        var provaFile = new MockMultipartFile("provaArquivo", "prova.pdf",
                "application/pdf", PDF_BYTES);

        assertThatThrownBy(() ->
                service.process("CEBRASPE", null, "Órgão", null,
                        provaFile, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gabarito");
    }

    @Test
    @DisplayName("process com banca inválida lança IllegalArgumentException antes de qualquer I/O")
    void process_bancaInvalida_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                service.process("INVALIDA", null, "Órgão", null,
                        null, "http://prova.pdf", null, "http://gabarito.pdf"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(downloader, never()).download(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("process com erro na extração retorna job FAILED com errorMessage")
    void process_erroPdfExtraction_retornaJobFailed() throws IOException {
        var provaFile = new MockMultipartFile("provaArquivo", "prova.pdf",
                "application/pdf", PDF_BYTES);
        var gabaritoFile = new MockMultipartFile("gabaritoArquivo", "gabarito.pdf",
                "application/pdf", PDF_BYTES);

        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(extractor.extract(PDF_BYTES)).willThrow(new IOException("PDF corrompido"));

        var job = service.process("FGV", null, "Órgão", null,
                provaFile, null, gabaritoFile, null);

        assertThat(job.getStatus()).isEqualTo(IngestionStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("PDF corrompido");
        assertThat(job.getTextProva()).isNull();
    }

    @Test
    @DisplayName("process após extração bem-sucedida chama QuestionParserService com os textos")
    void process_comExtracaoBemSucedida_chamaQuestionParser() throws IOException {
        var provaFile = new MockMultipartFile("provaArquivo", "prova.pdf",
                "application/pdf", PDF_BYTES);
        var gabaritoFile = new MockMultipartFile("gabaritoArquivo", "gabarito.pdf",
                "application/pdf", PDF_BYTES);

        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(extractor.extract(PDF_BYTES)).willReturn("texto prova").willReturn("texto gabarito");
        given(questionParserService.parse(any(), any(), any(), any(), any(), any()))
                .willReturn(new ParseResult(3, 1));

        var job = service.process("CEBRASPE", 2024, "Órgão", "Analista",
                provaFile, null, gabaritoFile, null);

        assertThat(job.getStatus()).isEqualTo(IngestionStatus.COMPLETED);
        assertThat(job.getQuestoesSalvas()).isEqualTo(3);
        assertThat(job.getQuestoesInvalidas()).isEqualTo(1);
        verify(questionParserService).parse(
                eq("texto prova"), eq("texto gabarito"),
                eq(Banca.CEBRASPE), eq(2024), eq("Analista"), isNull());
    }

    @Test
    @DisplayName("process quando QuestionParser lança exceção retorna job FAILED")
    void process_questionParserLancaException_retornaJobFailed() throws IOException {
        var provaFile = new MockMultipartFile("provaArquivo", "prova.pdf",
                "application/pdf", PDF_BYTES);
        var gabaritoFile = new MockMultipartFile("gabaritoArquivo", "gabarito.pdf",
                "application/pdf", PDF_BYTES);

        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(extractor.extract(PDF_BYTES)).willReturn("texto");
        given(questionParserService.parse(any(), any(), any(), any(), any(), any()))
                .willThrow(new RuntimeException("Falha na API Anthropic"));

        var job = service.process("FGV", null, "Órgão", null,
                provaFile, null, gabaritoFile, null);

        assertThat(job.getStatus()).isEqualTo(IngestionStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("Falha na API Anthropic");
    }
}
