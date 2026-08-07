package com.atwo.currencies_api.currency.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.atwo.currencies_api.currency.dtos.LoginRequest;
import com.atwo.currencies_api.currency.dtos.LoginResponse;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AdminSecurityIntegrationTest {

    private static final String SENHA_CORRETA = "senhaTeste123";
    private static final String HASH_DA_SENHA_CORRETA =
            "$2a$10$XNSkOwkVQ/umYDjL1RKMYex0oYCqVmX7HbYnmqbAp4lM0r6ifi7wC";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("admin.password-hash", () -> HASH_DA_SENHA_CORRETA);
        registry.add("jwt.secret",
                () -> "test-only-secret-not-used-in-any-real-environment-32bytes");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void rotaProtegida_semToken_deveRetornar401() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/sync", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rotaProtegida_comTokenInvalido_deveRetornar401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("token-invalido");

        ResponseEntity<String> response = restTemplate.exchange("/api/sync", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rotaPublica_semToken_deveFuncionarNormalmente() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/cotacoes/atual", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_senhaCorreta_deveRetornarTokenComStatus200() {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/api/admin/login",
                new LoginRequest(SENHA_CORRETA), LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void login_senhaIncorreta_deveRetornar401() {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/api/admin/login",
                new LoginRequest("senhaErrada"), LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rotaProtegida_comTokenValido_passaDaSegurancaMesmoFalhandoDepoisPorRegraDeNegocio() {
        String token = login();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange("/api/moedas/NAOEXISTE",
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);

        // não é barrado pela segurança (401); falha depois, na regra de negócio
        // (moeda inexistente), que é um problema completamente diferente
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void preflightCors_semToken_naoDeveSerBloqueadoPelaSeguranca() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "https://cotacoins.online");
        headers.set("Access-Control-Request-Method", "POST");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/historico/inicializar", HttpMethod.OPTIONS, new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rotaProtegida_semTokenValido_deveIncluirHeaderDeCorsMesmoNoErro() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "https://cotacoins.online");
        headers.setBearerAuth("token-expirado-ou-invalido");

        ResponseEntity<String> response = restTemplate.exchange("/api/sync", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("https://cotacoins.online");
    }

    private String login() {
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity("/api/admin/login",
                new LoginRequest(SENHA_CORRETA), LoginResponse.class);
        return response.getBody().token();
    }
}
