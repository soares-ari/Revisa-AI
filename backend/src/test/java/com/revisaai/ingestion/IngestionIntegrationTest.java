package com.revisaai.ingestion;

import com.revisaai.auth.dto.LoginRequest;
import com.revisaai.auth.dto.RegisterRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class IngestionIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    private static MockWebServer mockWebServer;

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri",
                () -> mongoDBContainer.getConnectionString() + "/revisaai_test");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IngestionJobRepository ingestionJobRepository;

    private String jwtToken;

    // Bytes mínimos de um PDF válido para PDFBox
    private static final byte[] MINIMAL_PDF = (
            "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
            "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
            "3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\n" +
            "xref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n" +
            "0000000058 00000 n \n0000000115 00000 n \n" +
            "trailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF"
    ).getBytes();

    @BeforeAll
    static void startMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopMockServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        ingestionJobRepository.deleteAll();

        var register = new RegisterRequest("Admin", "admin@test.com", "senha123");
        restTemplate.postForEntity("/auth/register", register, Map.class);

        var login = new LoginRequest("admin@test.com", "senha123");
        var loginResp = restTemplate.postForEntity("/auth/login", login, Map.class);
        jwtToken = (String) loginResp.getBody().get("accessToken");
    }

    @Test
    @DisplayName("POST /ingestion/jobs sem JWT retorna 403")
    void post_semJwt_retorna403() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var body = new LinkedMultiValueMap<String, Object>();
        body.add("banca", "CEBRASPE");
        body.add("provaArquivo", new ByteArrayResource(MINIMAL_PDF) {
            @Override public String getFilename() { return "prova.pdf"; }
        });
        body.add("gabaritoArquivo", new ByteArrayResource(MINIMAL_PDF) {
            @Override public String getFilename() { return "gabarito.pdf"; }
        });

        var response = restTemplate.exchange("/ingestion/jobs",
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /ingestion/jobs com arquivos e JWT retorna 201 COMPLETED")
    void post_comArquivos_e_jwt_retorna201Completed() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(jwtToken);

        var body = new LinkedMultiValueMap<String, Object>();
        body.add("banca", "CEBRASPE");
        body.add("provaArquivo", new ByteArrayResource(MINIMAL_PDF) {
            @Override public String getFilename() { return "prova.pdf"; }
        });
        body.add("gabaritoArquivo", new ByteArrayResource(MINIMAL_PDF) {
            @Override public String getFilename() { return "gabarito.pdf"; }
        });

        var response = restTemplate.exchange("/ingestion/jobs",
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody().get("status")).isIn("COMPLETED", "FAILED");
        assertThat(ingestionJobRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /ingestion/jobs com URL e JWT retorna 201")
    void post_comUrl_e_jwt_retorna201Completed() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(new String(MINIMAL_PDF))
                .addHeader("Content-Type", "application/pdf"));
        mockWebServer.enqueue(new MockResponse()
                .setBody(new String(MINIMAL_PDF))
                .addHeader("Content-Type", "application/pdf"));

        var provaUrl = mockWebServer.url("/prova.pdf").toString();
        var gabaritoUrl = mockWebServer.url("/gabarito.pdf").toString();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(jwtToken);

        var body = new LinkedMultiValueMap<String, Object>();
        body.add("banca", "FGV");
        body.add("provaUrl", provaUrl);
        body.add("gabaritoUrl", gabaritoUrl);

        var response = restTemplate.exchange("/ingestion/jobs",
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
    }

    @Test
    @DisplayName("POST /ingestion/jobs com banca inválida retorna 400")
    void post_bancaInvalida_retorna400() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(jwtToken);

        var body = new LinkedMultiValueMap<String, Object>();
        body.add("banca", "INVALIDA");
        body.add("provaArquivo", new ByteArrayResource(MINIMAL_PDF) {
            @Override public String getFilename() { return "prova.pdf"; }
        });
        body.add("gabaritoArquivo", new ByteArrayResource(MINIMAL_PDF) {
            @Override public String getFilename() { return "gabarito.pdf"; }
        });

        var response = restTemplate.exchange("/ingestion/jobs",
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
