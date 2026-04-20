package com.arremateai.gateway.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Externalização de Configurações do Gateway (E1-H1)")
class ExternalizacaoConfiguracoesTest {

    private static String conteudoApplicationYml;
    private static String conteudoEnvExample;

    private static final Path CAMINHO_APPLICATION_YML = Path.of("src/main/resources/application.yml");
    private static final Path CAMINHO_ENV_EXAMPLE = Path.of(".env.example");

    private static final Pattern PADRAO_VARIAVEL_AMBIENTE = Pattern.compile("\\$\\{([A-Z_]+)(?::([^}]*))?}");

    @BeforeAll
    static void setUp() throws IOException {
        conteudoApplicationYml = Files.readString(CAMINHO_APPLICATION_YML);
        conteudoEnvExample = Files.readString(CAMINHO_ENV_EXAMPLE);
    }

    // ==================== APPLICATION.YML — EXISTÊNCIA ====================

    @Test
    @DisplayName("Deve existir o arquivo application.yml")
    void deveExistirArquivoApplicationYml() {
        assertThat(CAMINHO_APPLICATION_YML).exists();
    }

    // ==================== APPLICATION.YML — PORTA ====================

    @Test
    @DisplayName("Deve externalizar a porta do servidor com default 8080")
    void deveExternalizarPortaDoServidorComDefault() {
        assertThat(conteudoApplicationYml).contains("${SERVER_PORT:8080}");
    }

    // ==================== APPLICATION.YML — JWT ====================

    @Test
    @DisplayName("Deve externalizar JWT_SECRET sem valor default (obrigatório)")
    void deveExternalizarJwtSecretSemDefault() {
        assertThat(conteudoApplicationYml).contains("${JWT_SECRET}");
        assertThat(conteudoApplicationYml).doesNotContain("${JWT_SECRET:");
    }

    @Test
    @DisplayName("Não deve conter segredos em texto plano no application.yml")
    void naoDeveConterSegredosEmTextoPlano() {
        assertThat(conteudoApplicationYml)
                .doesNotContainPattern("secret:\\s+[a-zA-Z0-9+/=]{20,}");
    }

    // ==================== APPLICATION.YML — URLs DOS MICROSSERVIÇOS ====================

    @ParameterizedTest
    @ValueSource(strings = {
            "IDENTITY_URL",
            "MEDIA_URL",
            "PROPERTY_CATALOG_URL",
            "VENDOR_URL",
            "USERPROFILE_URL",
            "ORCHESTRATOR_URL",
            "NOTIFICATION_URL",
            "MONOLITH_URL"
    })
    @DisplayName("Deve externalizar URL do microsserviço com default localhost")
    void deveExternalizarUrlDoMicrosservicoComDefault(String variavel) {
        Pattern padrao = Pattern.compile("\\$\\{" + variavel + ":http://localhost:\\d+}");
        Matcher matcher = padrao.matcher(conteudoApplicationYml);
        assertThat(matcher.find())
                .as("Variável %s deve estar externalizada com default localhost", variavel)
                .isTrue();
    }

    @Test
    @DisplayName("Deve usar padrão de externalização ${VAR:default} para todas as URIs de roteamento")
    void deveUsarPadraoDeExternalizacaoParaTodasAsUrisDeRoteamento() {
        List<String> linhasComUri = conteudoApplicationYml.lines()
                .filter(linha -> linha.stripLeading().startsWith("uri:"))
                .collect(Collectors.toList());

        assertThat(linhasComUri)
                .as("Deve haver pelo menos uma linha de URI configurada")
                .isNotEmpty();

        linhasComUri.forEach(linha ->
                assertThat(linha)
                        .as("Linha de URI deve usar variável de ambiente: %s", linha.strip())
                        .containsPattern("\\$\\{[A-Z_]+:")
        );
    }

    // ==================== .ENV.EXAMPLE — EXISTÊNCIA ====================

    @Test
    @DisplayName("Deve existir o arquivo .env.example")
    void deveExistirArquivoEnvExample() {
        assertThat(CAMINHO_ENV_EXAMPLE).exists();
    }

    // ==================== .ENV.EXAMPLE — DOCUMENTAÇÃO DAS VARIÁVEIS ====================

    @ParameterizedTest
    @ValueSource(strings = {
            "SERVER_PORT",
            "JWT_SECRET",
            "IDENTITY_URL",
            "MEDIA_URL",
            "PROPERTY_CATALOG_URL",
            "VENDOR_URL",
            "USERPROFILE_URL",
            "ORCHESTRATOR_URL",
            "NOTIFICATION_URL",
            "MONOLITH_URL"
    })
    @DisplayName("Deve documentar variável de ambiente no .env.example")
    void deveDocumentarVariavelNoEnvExample(String variavel) {
        assertThat(conteudoEnvExample)
                .as("Variável %s deve estar documentada no .env.example", variavel)
                .contains(variavel);
    }

    @Test
    @DisplayName("Todas as variáveis do application.yml devem estar documentadas no .env.example")
    void todasAsVariaveisDoYmlDevemEstarDocumentadasNoEnvExample() {
        Matcher matcher = PADRAO_VARIAVEL_AMBIENTE.matcher(conteudoApplicationYml);

        int quantidadeVariaveis = 0;
        while (matcher.find()) {
            String variavel = matcher.group(1);
            quantidadeVariaveis++;
            assertThat(conteudoEnvExample)
                    .as("Variável %s usada no application.yml deve estar no .env.example", variavel)
                    .contains(variavel);
        }

        assertThat(quantidadeVariaveis)
                .as("Deve haver pelo menos uma variável externalizada no application.yml")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("Deve marcar JWT_SECRET como obrigatório no .env.example")
    void deveMarcarJwtSecretComoObrigatorioNoEnvExample() {
        assertThat(conteudoEnvExample).containsIgnoringCase("OBRIGATÓRIO");
        assertThat(conteudoEnvExample).contains("JWT_SECRET");
    }

    // ==================== CONSISTÊNCIA ENTRE ARQUIVOS ====================

    @Test
    @DisplayName("Deve ter 9 rotas configuradas no gateway")
    void deveTerNoveRotasConfiguradas() {
        long quantidadeRotas = conteudoApplicationYml.lines()
                .filter(linha -> linha.stripLeading().startsWith("- id:"))
                .count();

        assertThat(quantidadeRotas)
                .as("Gateway deve ter 9 rotas configuradas")
                .isEqualTo(9);
    }

    @Test
    @DisplayName("Deve ter actuator expondo apenas endpoint de health")
    void deveTerActuatorExpondoApenasHealth() {
        assertThat(conteudoApplicationYml).contains("include: health");
    }
}
