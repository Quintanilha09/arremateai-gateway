package com.arremateai.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Tratador global reativo de exceções do API Gateway em formato RFC 7807
 * ({@code application/problem+json}).
 *
 * <p>Como o gateway é baseado em WebFlux (Spring Cloud Gateway), implementa
 * {@link ErrorWebExceptionHandler} com {@link Order} {@code -2} para ter
 * precedência sobre o {@code DefaultErrorWebExceptionHandler} do Spring Boot.
 * Produz respostas com tipo URN {@code urn:arremateai:error:*}, título,
 * detalhe, instância, timestamp e path, sem vazar stack traces em 5xx.</p>
 */
@Slf4j
@Order(-2)
@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final String TYPE_PREFIX = "urn:arremateai:error:";
    private static final String DETALHE_INTERNO = "Erro interno no gateway";

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable erro) {
        String path = exchange.getRequest().getPath().value();
        HttpStatus status = resolverStatus(erro);
        String tipoSufixo = tipoPara(status);
        String titulo = tituloPara(status);
        String detalhe = detalhePara(erro, status);

        if (status.is5xxServerError()) {
            log.error("Erro {} no gateway em {}", status.value(), path, erro);
        } else {
            log.warn("Erro {} no gateway em {}: {}", status.value(), path, erro.getMessage());
        }

        Map<String, Object> problema = new LinkedHashMap<>();
        problema.put("type", TYPE_PREFIX + tipoSufixo);
        problema.put("title", titulo);
        problema.put("status", status.value());
        problema.put("detail", detalhe);
        problema.put("instance", path);
        problema.put("timestamp", OffsetDateTime.now().toString());
        problema.put("path", path);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        DataBuffer buffer;
        try {
            buffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(problema));
        } catch (Exception falhaSerializacao) {
            log.error("Falha ao serializar ProblemDetail no gateway", falhaSerializacao);
            byte[] fallback = ("{\"type\":\"" + TYPE_PREFIX + "internal\",\"title\":\"Erro interno\","
                    + "\"status\":500,\"detail\":\"" + DETALHE_INTERNO + "\",\"instance\":\""
                    + path + "\"}").getBytes();
            buffer = bufferFactory.wrap(fallback);
        }
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    HttpStatus resolverStatus(Throwable erro) {
        if (erro instanceof ResponseStatusException rse) {
            HttpStatusCode code = rse.getStatusCode();
            HttpStatus status = HttpStatus.resolve(code.value());
            return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (erro instanceof WebClientResponseException wcre) {
            HttpStatus status = HttpStatus.resolve(wcre.getStatusCode().value());
            if (status != null && status.is5xxServerError()) {
                return HttpStatus.BAD_GATEWAY;
            }
            return status != null ? status : HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    String tipoPara(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "unauthenticated";
            case FORBIDDEN -> "forbidden";
            case NOT_FOUND -> "not-found";
            case BAD_REQUEST -> "illegal-argument";
            case CONFLICT -> "conflict";
            case BAD_GATEWAY -> "bad-gateway";
            case SERVICE_UNAVAILABLE -> "service-unavailable";
            case GATEWAY_TIMEOUT -> "gateway-timeout";
            default -> status.is5xxServerError() ? "internal" : "error";
        };
    }

    String tituloPara(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "Autenticação necessária";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Rota não encontrada";
            case BAD_REQUEST -> "Requisição inválida";
            case CONFLICT -> "Operação em conflito com o estado atual";
            case BAD_GATEWAY -> "Erro em serviço downstream";
            case SERVICE_UNAVAILABLE -> "Serviço indisponível";
            case GATEWAY_TIMEOUT -> "Tempo esgotado em serviço downstream";
            default -> status.is5xxServerError() ? "Erro interno do servidor" : status.getReasonPhrase();
        };
    }

    String detalhePara(Throwable erro, HttpStatus status) {
        if (status.is5xxServerError()) {
            return DETALHE_INTERNO;
        }
        if (erro instanceof ResponseStatusException rse) {
            return Optional.ofNullable(rse.getReason()).orElse(status.getReasonPhrase());
        }
        return Optional.ofNullable(erro.getMessage()).orElse(status.getReasonPhrase());
    }
}
