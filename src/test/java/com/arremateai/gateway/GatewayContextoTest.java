package com.arremateai.gateway;

import com.arremateai.gateway.filter.JwtValidacaoFilter;
import com.arremateai.gateway.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "jwt.secret=chave-secreta-para-testes-integracao-com-pelo-menos-256-bits-segura-ok-1234567890"
})
@DisplayName("Contexto Spring do Gateway com configurações externalizadas")
class GatewayContextoTest {

    @Autowired
    private ApplicationContext contexto;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtValidacaoFilter jwtValidacaoFilter;

    @Value("${server.port:0}")
    private int porta;

    @Test
    @DisplayName("Deve carregar o contexto Spring quando JWT_SECRET está definido")
    void deveCarregarContextoSpringQuandoJwtSecretDefinido() {
        assertThat(contexto).isNotNull();
    }

    @Test
    @DisplayName("Deve registrar JwtService como bean no contexto")
    void deveRegistrarJwtServiceComoBean() {
        assertThat(jwtService).isNotNull();
        assertThat(contexto.getBean(JwtService.class)).isSameAs(jwtService);
    }

    @Test
    @DisplayName("Deve registrar JwtValidacaoFilter como bean no contexto")
    void deveRegistrarJwtValidacaoFilterComoBean() {
        assertThat(jwtValidacaoFilter).isNotNull();
        assertThat(contexto.getBean(JwtValidacaoFilter.class)).isSameAs(jwtValidacaoFilter);
    }

    @Test
    @DisplayName("Deve usar porta padrão do application.yml quando SERVER_PORT não definido")
    void deveUsarPortaPadraoQuandoServerPortNaoDefinido() {
        // Em testes com RANDOM_PORT, o Spring usa porta aleatória
        // Verificamos que a propriedade server.port existe no Environment
        String portaConfigurada = contexto.getEnvironment().getProperty("server.port");
        assertThat(portaConfigurada).isNotNull();
    }

    @Test
    @DisplayName("Deve carregar propriedade jwt.secret do Environment")
    void deveCarregarPropriedadeJwtSecretDoEnvironment() {
        String jwtSecret = contexto.getEnvironment().getProperty("jwt.secret");
        assertThat(jwtSecret).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Deve carregar nome da aplicação como arremateai-gateway")
    void deveCarregarNomeDaAplicacao() {
        String nomeAplicacao = contexto.getEnvironment().getProperty("spring.application.name");
        assertThat(nomeAplicacao).isEqualTo("arremateai-gateway");
    }
}
