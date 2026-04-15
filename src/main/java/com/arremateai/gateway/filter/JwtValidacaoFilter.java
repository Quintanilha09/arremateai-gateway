package com.arremateai.gateway.filter;

import com.arremateai.gateway.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidacaoFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    /**
     * Caminhos públicos que não precisam de autenticação.
     */
    private static final int MAX_TOKEN_LENGTH = 4096;

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/uploads/"
    );

    private static final List<String> PUBLIC_EXACT_PATHS = List.of(
            "/api/health",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/2fa/enviar-codigo",
            "/api/auth/2fa/verificar-codigo",
            "/api/auth/recuperar-senha",
            "/api/auth/redefinir-senha",
            "/api/auth/oauth2/callback/google"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // Remove headers injetados por clientes externos (prevenção de spoofing)
        ServerHttpRequest sanitizedRequest = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                    headers.remove("X-User-Email");
                })
                .build();

        // Caminhos públicos: deixa passar sem validar JWT
        if (isPublicPath(path, method)) {
            return chain.filter(exchange.mutate().request(sanitizedRequest).build());
        }

        // Extrai Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return responderNaoAutorizado(exchange.getResponse(), "Token não fornecido");
        }

        String token = authHeader.substring(7);

        if (token.length() > MAX_TOKEN_LENGTH) {
            return responderNaoAutorizado(exchange.getResponse(), "Token inválido");
        }

        try {
            if (!jwtService.isTokenValid(token)) {
                return responderNaoAutorizado(exchange.getResponse(), "Token inválido ou expirado");
            }

            String userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            String email = jwtService.extractEmail(token);

            // Injeta identificação do usuário nos headers downstream
            ServerHttpRequest autenticadoRequest = sanitizedRequest.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Role", role != null ? role : "")
                    .header("X-User-Email", email != null ? email : "")
                    .build();

            log.debug("Request autenticado: path={}, userId={}, role={}", path, userId, role);

            return chain.filter(exchange.mutate().request(autenticadoRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Token expirado para path: {}", path);
            return responderNaoAutorizado(exchange.getResponse(), "Token expirado");
        } catch (JwtException e) {
            log.warn("Token inválido para path: {}: {}", path, e.getMessage());
            return responderNaoAutorizado(exchange.getResponse(), "Token inválido");
        }
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        // Paths exatos públicos
        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return true;
        }
        // Prefixos públicos
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        // GET em catálogo de imóveis, produtos e localização é público
        if (HttpMethod.GET.equals(method)) {
            if ((path.startsWith("/api/imoveis/") || path.equals("/api/imoveis"))
                    && !path.equals("/api/imoveis/meus")) {
                // /{id}/estatisticas é protegido (requer autenticação do vendedor)
                if (path.length() > "/api/imoveis/".length()) {
                    String subPath = path.substring("/api/imoveis/".length());
                    if (subPath.contains("/estatisticas")) {
                        return false;
                    }
                }
                return true;
            }
            if (path.startsWith("/api/produtos/") || path.equals("/api/produtos")) {
                return true;
            }
            if (path.startsWith("/api/localizacao/")) {
                return true;
            }
            if (path.startsWith("/api/perfil/avatar/arquivo/")) {
                return true;
            }
        }
        // Registro de vendedor é público
        if (path.equals("/api/vendedores/registrar") && HttpMethod.POST.equals(method)) {
            return true;
        }
        if (path.equals("/api/vendedores/verificar-email-corporativo") && HttpMethod.POST.equals(method)) {
            return true;
        }
        if (path.equals("/api/vendedores/reenviar-codigo") && HttpMethod.POST.equals(method)) {
            return true;
        }
        return false;
    }

    private Mono<Void> responderNaoAutorizado(ServerHttpResponse response, String mensagem) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String escapedMsg = mensagem.replace("\\", "\\\\").replace("\"", "\\\"");
        String body = "{\"error\":\"Não autorizado\",\"message\":\"" + escapedMsg + "\"}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Flux.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // Executa antes de outros filtros
    }
}
