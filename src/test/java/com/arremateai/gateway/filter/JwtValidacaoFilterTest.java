package com.arremateai.gateway.filter;

import com.arremateai.gateway.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtValidacaoFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private JwtValidacaoFilter filtro;

    private static final String TOKEN_VALIDO = "eyJhbGciOiJIUzI1NiJ9.valido";
    private static final String USER_ID_PADRAO = "123e4567-e89b-12d3-a456-426614174000";
    private static final String ROLE_PADRAO = "COMPRADOR";
    private static final String EMAIL_PADRAO = "usuario@teste.com";

    @BeforeEach
    void setUp() {
        lenient().when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    // ==================== CAMINHOS PÚBLICOS ====================

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/health",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/2fa/enviar-codigo",
            "/api/auth/2fa/verificar-codigo",
            "/api/auth/recuperar-senha",
            "/api/auth/redefinir-senha",
            "/api/auth/oauth2/callback/google"
    })
    @DisplayName("Deve permitir acesso sem token em caminhos públicos exatos")
    void devePermitirAcessoSemTokenEmCaminhosPublicosExatos(String caminho) {
        var request = MockServerHttpRequest.get(caminho).build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Deve permitir acesso sem token em caminhos com prefixo /uploads/")
    void devePermitirAcessoSemTokenEmUploads() {
        var request = MockServerHttpRequest.get("/uploads/imagens/foto.jpg").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Deve permitir GET público em /api/imoveis")
    void devePermitirGetPublicoEmImoveis() {
        var request = MockServerHttpRequest.get("/api/imoveis").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Deve permitir GET público em /api/imoveis/{id}")
    void devePermitirGetPublicoEmImoveisComId() {
        var request = MockServerHttpRequest.get("/api/imoveis/123").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Deve exigir autenticação para GET em /api/imoveis/meus")
    void deveExigirAutenticacaoParaGetEmImoveisMeus() {
        var request = MockServerHttpRequest.get("/api/imoveis/meus").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Deve permitir GET público em /api/produtos")
    void devePermitirGetPublicoEmProdutos() {
        var request = MockServerHttpRequest.get("/api/produtos").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Deve permitir GET público em /api/localizacao/")
    void devePermitirGetPublicoEmLocalizacao() {
        var request = MockServerHttpRequest.get("/api/localizacao/estados").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Deve permitir GET público em /api/perfil/avatar/arquivo/")
    void devePermitirGetPublicoEmAvatar() {
        var request = MockServerHttpRequest.get("/api/perfil/avatar/arquivo/123").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Deve permitir POST público em /api/vendedores/registrar")
    void devePermitirPostPublicoEmVendedoresRegistrar() {
        var request = MockServerHttpRequest.post("/api/vendedores/registrar").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Deve permitir POST público em /api/vendedores/verificar-email-corporativo")
    void devePermitirPostPublicoEmVerificarEmailCorporativo() {
        var request = MockServerHttpRequest.post("/api/vendedores/verificar-email-corporativo").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Deve permitir POST público em /api/vendedores/reenviar-codigo")
    void devePermitirPostPublicoEmReenviarCodigo() {
        var request = MockServerHttpRequest.post("/api/vendedores/reenviar-codigo").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }

    // ==================== AUTENTICAÇÃO ====================

    @Test
    @DisplayName("Deve retornar 401 quando Authorization header ausente")
    void deveRetornar401QuandoAuthorizationAusente() {
        var request = MockServerHttpRequest.get("/api/perfil").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Deve retornar 401 quando Authorization não começa com Bearer")
    void deveRetornar401QuandoAuthorizationNaoComecaComBearer() {
        var request = MockServerHttpRequest.get("/api/perfil")
                .header("Authorization", "Basic dXN1YXJpbzpzZW5oYQ==")
                .build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Deve retornar 401 quando token excede tamanho máximo")
    void deveRetornar401QuandoTokenExcedeTamanhoMaximo() {
        var tokenGrande = "a".repeat(4097);
        var request = MockServerHttpRequest.get("/api/perfil")
                .header("Authorization", "Bearer " + tokenGrande)
                .build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Deve retornar 401 quando token é inválido")
    void deveRetornar401QuandoTokenEInvalido() {
        when(jwtService.isTokenValid(TOKEN_VALIDO)).thenReturn(false);

        var request = MockServerHttpRequest.get("/api/perfil")
                .header("Authorization", "Bearer " + TOKEN_VALIDO)
                .build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Deve propagar request autenticado quando token é válido")
    void devePropagarRequestAutenticadoQuandoTokenEValido() {
        when(jwtService.isTokenValid(TOKEN_VALIDO)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN_VALIDO)).thenReturn(USER_ID_PADRAO);
        when(jwtService.extractRole(TOKEN_VALIDO)).thenReturn(ROLE_PADRAO);
        when(jwtService.extractEmail(TOKEN_VALIDO)).thenReturn(EMAIL_PADRAO);

        var request = MockServerHttpRequest.get("/api/perfil")
                .header("Authorization", "Bearer " + TOKEN_VALIDO)
                .build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        var exchangeCapturado = captor.getValue();
        var headers = exchangeCapturado.getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo(USER_ID_PADRAO);
        assertThat(headers.getFirst("X-User-Role")).isEqualTo(ROLE_PADRAO);
        assertThat(headers.getFirst("X-User-Email")).isEqualTo(EMAIL_PADRAO);
    }

    @Test
    @DisplayName("Deve injetar headers vazios quando claims são nulos")
    void deveInjetarHeadersVaziosQuandoClaimsSaoNulos() {
        when(jwtService.isTokenValid(TOKEN_VALIDO)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN_VALIDO)).thenReturn(null);
        when(jwtService.extractRole(TOKEN_VALIDO)).thenReturn(null);
        when(jwtService.extractEmail(TOKEN_VALIDO)).thenReturn(null);

        var request = MockServerHttpRequest.get("/api/perfil")
                .header("Authorization", "Bearer " + TOKEN_VALIDO)
                .build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        var headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEmpty();
        assertThat(headers.getFirst("X-User-Role")).isEmpty();
        assertThat(headers.getFirst("X-User-Email")).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar 401 quando ExpiredJwtException é lançada")
    void deveRetornar401QuandoExpiredJwtExceptionLancada() {
        when(jwtService.isTokenValid(TOKEN_VALIDO)).thenThrow(
                new ExpiredJwtException(null, null, "Token expirado"));

        var request = MockServerHttpRequest.get("/api/perfil")
                .header("Authorization", "Bearer " + TOKEN_VALIDO)
                .build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Deve retornar 401 quando JwtException genérica é lançada")
    void deveRetornar401QuandoJwtExceptionGenericaLancada() {
        when(jwtService.isTokenValid(TOKEN_VALIDO)).thenThrow(
                new JwtException("Token malformado"));

        var request = MockServerHttpRequest.get("/api/perfil")
                .header("Authorization", "Bearer " + TOKEN_VALIDO)
                .build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ==================== SPOOFING PREVENTION ====================

    @Test
    @DisplayName("Deve remover headers X-User injetados por clientes externos em caminhos públicos")
    void deveRemoverHeadersSpoofingEmCaminhosPublicos() {
        var request = MockServerHttpRequest.get("/api/imoveis")
                .header("X-User-Id", "hacker-id")
                .header("X-User-Role", "ADMIN")
                .header("X-User-Email", "hacker@evil.com")
                .build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        var headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.containsKey("X-User-Id")).isFalse();
        assertThat(headers.containsKey("X-User-Role")).isFalse();
        assertThat(headers.containsKey("X-User-Email")).isFalse();
    }

    @Test
    @DisplayName("Deve substituir headers X-User spoofados pelos extraídos do token")
    void deveSubstituirHeadersSpoofadosPelosDoToken() {
        when(jwtService.isTokenValid(TOKEN_VALIDO)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN_VALIDO)).thenReturn(USER_ID_PADRAO);
        when(jwtService.extractRole(TOKEN_VALIDO)).thenReturn(ROLE_PADRAO);
        when(jwtService.extractEmail(TOKEN_VALIDO)).thenReturn(EMAIL_PADRAO);

        var request = MockServerHttpRequest.get("/api/perfil")
                .header("Authorization", "Bearer " + TOKEN_VALIDO)
                .header("X-User-Id", "hacker-id")
                .header("X-User-Role", "ADMIN")
                .header("X-User-Email", "hacker@evil.com")
                .build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        var headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo(USER_ID_PADRAO);
        assertThat(headers.getFirst("X-User-Role")).isEqualTo(ROLE_PADRAO);
        assertThat(headers.getFirst("X-User-Email")).isEqualTo(EMAIL_PADRAO);
    }

    // ==================== ORDEM DO FILTRO ====================

    @Test
    @DisplayName("Deve retornar ordem -100 para executar antes de outros filtros")
    void deveRetornarOrdemMenosCem() {
        assertThat(filtro.getOrder()).isEqualTo(-100);
    }

    // ==================== PATHS NÃO PÚBLICOS ====================

    @Test
    @DisplayName("Deve exigir autenticação para POST em /api/imoveis")
    void deveExigirAutenticacaoParaPostEmImoveis() {
        var request = MockServerHttpRequest.post("/api/imoveis").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Deve exigir autenticação para GET em /api/favoritos")
    void deveExigirAutenticacaoParaGetEmFavoritos() {
        var request = MockServerHttpRequest.get("/api/favoritos").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Deve exigir autenticação para GET em /api/notificacoes")
    void deveExigirAutenticacaoParaGetEmNotificacoes() {
        var request = MockServerHttpRequest.get("/api/notificacoes").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Deve exigir autenticação para GET em /api/configuracoes")
    void deveExigirAutenticacaoParaGetEmConfiguracoes() {
        var request = MockServerHttpRequest.get("/api/configuracoes").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Deve permitir GET em /api/produtos/{id}")
    void devePermitirGetEmProdutosComId() {
        var request = MockServerHttpRequest.get("/api/produtos/123").build();
        var exchange = MockServerWebExchange.from(request);

        filtro.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
    }
}
