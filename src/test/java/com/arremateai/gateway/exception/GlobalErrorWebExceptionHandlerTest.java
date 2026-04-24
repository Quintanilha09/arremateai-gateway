package com.arremateai.gateway.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

@DisplayName("GlobalErrorWebExceptionHandler — respostas RFC 7807 no gateway")
class GlobalErrorWebExceptionHandlerTest {

    private static final String PATH_TESTE = "/api/identity/login";
    private static final String TIPO_PREFIXO = "urn:arremateai:error:";

    private GlobalErrorWebExceptionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        this.handler = new GlobalErrorWebExceptionHandler(objectMapper);
    }

    private MockServerWebExchange novaExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get(PATH_TESTE).build());
    }

    private JsonNode lerCorpo(MockServerWebExchange exchange) throws Exception {
        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).isNotNull();
        return objectMapper.readTree(body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("handle → 401 para ResponseStatusException UNAUTHORIZED")
    void handleDeveRetornar401ParaUnauthorized() throws Exception {
        MockServerWebExchange exchange = novaExchange();
        ResponseStatusException erro = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token ausente");

        StepVerifier.create(handler.handle(exchange, erro)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        JsonNode json = lerCorpo(exchange);
        assertThat(json.get("status").asInt()).isEqualTo(401);
        assertThat(json.get("type").asText()).isEqualTo(TIPO_PREFIXO + "unauthenticated");
        assertThat(json.get("title").asText()).isEqualTo("Autenticação necessária");
        assertThat(json.get("instance").asText()).isEqualTo(PATH_TESTE);
        assertThat(json.get("path").asText()).isEqualTo(PATH_TESTE);
        assertThat(json.has("timestamp")).isTrue();
    }

    @Test
    @DisplayName("handle → 404 para ResponseStatusException NOT_FOUND (rota inexistente)")
    void handleDeveRetornar404ParaNotFound() throws Exception {
        MockServerWebExchange exchange = novaExchange();
        ResponseStatusException erro = new ResponseStatusException(HttpStatus.NOT_FOUND, null);

        StepVerifier.create(handler.handle(exchange, erro)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode json = lerCorpo(exchange);
        assertThat(json.get("type").asText()).isEqualTo(TIPO_PREFIXO + "not-found");
        assertThat(json.get("title").asText()).isEqualTo("Rota não encontrada");
    }

    @Test
    @DisplayName("handle → WebClient 5xx vira 502 bad-gateway sem vazar body")
    void handleDeveMascarar5xxComo502() throws Exception {
        MockServerWebExchange exchange = novaExchange();
        WebClientResponseException erro = WebClientResponseException.create(
                500, "Internal Server Error", HttpHeaders.EMPTY,
                "stack trace do downstream".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        StepVerifier.create(handler.handle(exchange, erro)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        JsonNode json = lerCorpo(exchange);
        assertThat(json.get("status").asInt()).isEqualTo(502);
        assertThat(json.get("type").asText()).isEqualTo(TIPO_PREFIXO + "bad-gateway");
        assertThat(json.get("detail").asText()).doesNotContain("stack trace");
    }

    @Test
    @DisplayName("handle → WebClient 4xx propaga status do downstream")
    void handleDevePropagar4xxDoDownstream() throws Exception {
        MockServerWebExchange exchange = novaExchange();
        WebClientResponseException erro = WebClientResponseException.create(
                404, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);

        StepVerifier.create(handler.handle(exchange, erro)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode json = lerCorpo(exchange);
        assertThat(json.get("type").asText()).isEqualTo(TIPO_PREFIXO + "not-found");
    }

    @Test
    @DisplayName("handle → exceção genérica retorna 500 internal sem stack trace")
    void handleDeveRetornar500ParaExcecaoGenerica() throws Exception {
        MockServerWebExchange exchange = novaExchange();
        RuntimeException erro = new RuntimeException("NullPointerException interno");

        StepVerifier.create(handler.handle(exchange, erro)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        JsonNode json = lerCorpo(exchange);
        assertThat(json.get("status").asInt()).isEqualTo(500);
        assertThat(json.get("type").asText()).isEqualTo(TIPO_PREFIXO + "internal");
        assertThat(json.get("title").asText()).isEqualTo("Erro interno do servidor");
        assertThat(json.get("detail").asText()).doesNotContain("NullPointerException");
    }

    @Test
    @DisplayName("handle → 503 para ResponseStatusException SERVICE_UNAVAILABLE")
    void handleDeveRetornar503ParaServiceUnavailable() throws Exception {
        MockServerWebExchange exchange = novaExchange();
        ResponseStatusException erro = new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "circuito aberto");

        StepVerifier.create(handler.handle(exchange, erro)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        JsonNode json = lerCorpo(exchange);
        assertThat(json.get("type").asText()).isEqualTo(TIPO_PREFIXO + "service-unavailable");
        assertThat(json.get("title").asText()).isEqualTo("Serviço indisponível");
    }

    @Test
    @DisplayName("handle → 504 para ResponseStatusException GATEWAY_TIMEOUT")
    void handleDeveRetornar504ParaGatewayTimeout() throws Exception {
        MockServerWebExchange exchange = novaExchange();
        ResponseStatusException erro = new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "timeout");

        StepVerifier.create(handler.handle(exchange, erro)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        JsonNode json = lerCorpo(exchange);
        assertThat(json.get("type").asText()).isEqualTo(TIPO_PREFIXO + "gateway-timeout");
    }

    @Test
    @DisplayName("resolverStatus → 500 quando código HTTP é desconhecido")
    void resolverStatusDeveCairParaInternalQuandoCodigoDesconhecido() {
        ResponseStatusException erro = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(handler.resolverStatus(erro)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("tipoPara → mapeia corretamente status conhecidos")
    void tipoParaDeveMapearStatusConhecidos() {
        assertThat(handler.tipoPara(HttpStatus.UNAUTHORIZED)).isEqualTo("unauthenticated");
        assertThat(handler.tipoPara(HttpStatus.FORBIDDEN)).isEqualTo("forbidden");
        assertThat(handler.tipoPara(HttpStatus.BAD_REQUEST)).isEqualTo("illegal-argument");
        assertThat(handler.tipoPara(HttpStatus.CONFLICT)).isEqualTo("conflict");
        assertThat(handler.tipoPara(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo("internal");
    }
}
