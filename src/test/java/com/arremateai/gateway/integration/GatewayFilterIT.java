package com.arremateai.gateway.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Testes de integração do Gateway (E16-H10).
 *
 * <p>Exercitam o {@link com.arremateai.gateway.filter.JwtValidacaoFilter}
 * verificando que requisições a rotas protegidas sem autenticação válida
 * são rejeitadas com HTTP 401 antes de qualquer tentativa de roteamento
 * para serviços downstream.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "jwt.secret=chave-secreta-para-testes-integracao-com-pelo-menos-256-bits-segura-ok-1234567890"
})
@DisplayName("Gateway - Filtro JWT (IT)")
class GatewayFilterIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("GET /api/perfil sem Authorization deve retornar 401")
    void deveRetornar401ParaApiPerfilSemAuthorization() {
        webTestClient.get().uri("/api/perfil")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/vendedores/meus sem Authorization deve retornar 401")
    void deveRetornar401ParaApiVendedoresMeusSemAuthorization() {
        webTestClient.get().uri("/api/vendedores/meus")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/favoritos sem Authorization deve retornar 401")
    void deveRetornar401ParaApiFavoritosSemAuthorization() {
        webTestClient.get().uri("/api/favoritos")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/configuracoes sem Authorization deve retornar 401")
    void deveRetornar401ParaApiConfiguracoesSemAuthorization() {
        webTestClient.get().uri("/api/configuracoes")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/orchestrator/dashboard sem Authorization deve retornar 401")
    void deveRetornar401ParaApiOrchestratorSemAuthorization() {
        webTestClient.get().uri("/api/orchestrator/dashboard")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/notificacoes sem Authorization deve retornar 401")
    void deveRetornar401ParaApiNotificacoesSemAuthorization() {
        webTestClient.get().uri("/api/notificacoes")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Authorization sem prefixo Bearer deve retornar 401")
    void deveRetornar401ParaAuthorizationSemBearer() {
        webTestClient.get().uri("/api/perfil")
                .header("Authorization", "Token abc.def.ghi")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Bearer com token aleatório inválido deve retornar 401")
    void deveRetornar401ParaTokenInvalido() {
        webTestClient.get().uri("/api/perfil")
                .header("Authorization", "Bearer token.invalido.lixo")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Bearer com token maior que 4096 caracteres deve retornar 401")
    void deveRetornar401ParaTokenMuitoGrande() {
        String tokenGrande = "a".repeat(5000);
        webTestClient.get().uri("/api/perfil")
                .header("Authorization", "Bearer " + tokenGrande)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/imoveis/meus sem Authorization deve retornar 401 (rota protegida)")
    void deveRetornar401ParaImoveisMeusSemAuthorization() {
        webTestClient.get().uri("/api/imoveis/meus")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/imoveis/{id}/estatisticas sem Authorization deve retornar 401 (rota protegida)")
    void deveRetornar401ParaImoveisEstatisticasSemAuthorization() {
        webTestClient.get().uri("/api/imoveis/123/estatisticas")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
