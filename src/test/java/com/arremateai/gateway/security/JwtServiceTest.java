package com.arremateai.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String CHAVE_SECRETA = "minha-chave-secreta-para-testes-com-pelo-menos-256-bits-ok";
    private static final String EMAIL_PADRAO = "usuario@teste.com";
    private static final String USER_ID_PADRAO = "123e4567-e89b-12d3-a456-426614174000";
    private static final String ROLE_PADRAO = "COMPRADOR";

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        Field secretField = JwtService.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(jwtService, CHAVE_SECRETA);
    }

    private String criarTokenValido() {
        SecretKey chave = Keys.hmacShaKeyFor(CHAVE_SECRETA.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(EMAIL_PADRAO)
                .claim("userId", USER_ID_PADRAO)
                .claim("role", ROLE_PADRAO)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(chave)
                .compact();
    }

    private String criarTokenExpirado() {
        SecretKey chave = Keys.hmacShaKeyFor(CHAVE_SECRETA.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(EMAIL_PADRAO)
                .claim("userId", USER_ID_PADRAO)
                .claim("role", ROLE_PADRAO)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(chave)
                .compact();
    }

    private String criarTokenSemClaims() {
        SecretKey chave = Keys.hmacShaKeyFor(CHAVE_SECRETA.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(EMAIL_PADRAO)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(chave)
                .compact();
    }

    @Test
    @DisplayName("Deve extrair email do token válido")
    void deveExtrairEmailDoTokenValido() {
        var token = criarTokenValido();

        var resultado = jwtService.extractEmail(token);

        assertThat(resultado).isEqualTo(EMAIL_PADRAO);
    }

    @Test
    @DisplayName("Deve extrair userId do token válido")
    void deveExtrairUserIdDoTokenValido() {
        var token = criarTokenValido();

        var resultado = jwtService.extractUserId(token);

        assertThat(resultado).isEqualTo(USER_ID_PADRAO);
    }

    @Test
    @DisplayName("Deve extrair role do token válido")
    void deveExtrairRoleDoTokenValido() {
        var token = criarTokenValido();

        var resultado = jwtService.extractRole(token);

        assertThat(resultado).isEqualTo(ROLE_PADRAO);
    }

    @Test
    @DisplayName("Deve retornar null quando userId não existe no token")
    void deveRetornarNullQuandoUserIdNaoExisteNoToken() {
        var token = criarTokenSemClaims();

        var resultado = jwtService.extractUserId(token);

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("Deve retornar null quando role não existe no token")
    void deveRetornarNullQuandoRoleNaoExisteNoToken() {
        var token = criarTokenSemClaims();

        var resultado = jwtService.extractRole(token);

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("Deve retornar false para token expirado via isTokenExpired")
    void deveRetornarTrueParaTokenExpiradoViaIsTokenExpired() {
        var token = criarTokenExpirado();

        assertThatThrownBy(() -> jwtService.isTokenExpired(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Deve retornar false para token não expirado via isTokenExpired")
    void deveRetornarFalseParaTokenNaoExpirado() {
        var token = criarTokenValido();

        var resultado = jwtService.isTokenExpired(token);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Deve retornar true quando token é válido")
    void deveRetornarTrueQuandoTokenEValido() {
        var token = criarTokenValido();

        var resultado = jwtService.isTokenValid(token);

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false quando token é expirado")
    void deveRetornarFalseQuandoTokenEExpirado() {
        var token = criarTokenExpirado();

        var resultado = jwtService.isTokenValid(token);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando token é inválido")
    void deveRetornarFalseQuandoTokenEInvalido() {
        var resultado = jwtService.isTokenValid("token-completamente-invalido");

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando token tem assinatura inválida")
    void deveRetornarFalseQuandoTokenTemAssinaturaInvalida() {
        SecretKey outraChave = Keys.hmacShaKeyFor(
                "outra-chave-secreta-completamente-diferente-256-bits-minimo".getBytes(StandardCharsets.UTF_8));
        var tokenComOutraChave = Jwts.builder()
                .subject(EMAIL_PADRAO)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(outraChave)
                .compact();

        var resultado = jwtService.isTokenValid(tokenComOutraChave);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Deve extrair claim genérico do token")
    void deveExtrairClaimGenericoDoToken() {
        var token = criarTokenValido();

        var resultado = jwtService.extractClaim(token, Claims::getSubject);

        assertThat(resultado).isEqualTo(EMAIL_PADRAO);
    }

    @Test
    @DisplayName("Deve lançar exceção ao extrair claim de token inválido")
    void deveLancarExcecaoAoExtrairClaimDeTokenInvalido() {
        assertThatThrownBy(() -> jwtService.extractEmail("token-invalido"))
                .isInstanceOf(Exception.class);
    }
}
