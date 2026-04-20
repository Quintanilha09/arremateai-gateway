# [E2-H1] Dockerfiles Multi-Stage — Serviços Core (Gateway, Identity, Userprofile, Property-Catalog)

## Resumo
**Como** engenheiro DevOps / desenvolvedor do time ArremateAI,
**Quero** que cada microsserviço core tenha um Dockerfile multi-stage otimizado e um .dockerignore configurado,
**Para** que possamos construir imagens Docker leves, seguras e prontas para orquestração com Docker Compose e futura implantação em produção.

## Contexto
O ArremateAI opera com arquitetura de microsserviços Java/Spring Boot. Atualmente, todos os serviços são executados diretamente na máquina do desenvolvedor via `mvn spring-boot:run` ou JAR local. **Nenhum repositório possui Dockerfile ou .dockerignore**. Esta história é o primeiro passo do Épico E2 (Containerização com Docker) e é pré-requisito para a criação do `docker-compose.yml` (E2-H4).

## Inventário dos Serviços

| Serviço | Porta | Artifact ID | Config | Banco |
|---|---|---|---|---|
| **Gateway** | 8080 | `arremateai-gateway-0.0.1-SNAPSHOT.jar` | `application.yml` | Nenhum (proxy reverso) |
| **Identity** | 8081 | `arremateai-identity-0.0.1-SNAPSHOT.jar` | `application.properties` | `identity_db` |
| **Userprofile** | 8085 | `arremateai-userprofile-0.0.1-SNAPSHOT.jar` | `application.properties` | `arremateai` |
| **Property-Catalog** | 8082 | `arremateai-property-catalog-0.0.1-SNAPSHOT.jar` | `application.properties` | `property_catalog_db` |

## Regras de Negócio

| # | Regra | Descrição |
|---|---|---|
| RN01 | Multi-stage obrigatório | Toda imagem DEVE usar build multi-stage: Stage 1 (build) com Maven+JDK, Stage 2 (runtime) com JRE slim |
| RN02 | Imagem base padronizada | Build: `maven:3.9-eclipse-temurin-17-alpine`. Runtime: `eclipse-temurin:17-jre-alpine` |
| RN03 | Usuário não-root | O container DEVE rodar com um usuário não-root (ex: `appuser`, UID 1001) |
| RN04 | Porta correta | Cada Dockerfile DEVE expor (EXPOSE) apenas a porta do seu serviço |
| RN05 | HEALTHCHECK embutido | Cada Dockerfile DEVE conter instrução `HEALTHCHECK` usando `curl` contra `/actuator/health` |
| RN06 | .dockerignore obrigatório | Cada repositório DEVE ter um `.dockerignore` |
| RN07 | Sem secrets hardcoded | Nenhum valor de credencial pode estar no Dockerfile |
| RN08 | Cache de dependências Maven | Copiar `pom.xml` antes do código-fonte para cachear dependências |
| RN09 | JAR mínimo | Apenas o JAR fat deve ser copiado para o stage de runtime |
| RN10 | Tamanho de imagem | Imagem final < 300MB |

## Cenários BDD

### Cenário 1: Build com sucesso
```gherkin
Dado que o repositório do serviço "<servico>" possui um Dockerfile na raiz
E possui um .dockerignore na raiz
Quando eu executo "docker build -t arremateai/<servico>:latest ." na raiz do repositório
Então o build completa sem erros
E a imagem é criada com sucesso

Exemplos:
  | servico           |
  | gateway           |
  | identity          |
  | userprofile       |
  | property-catalog  |
```

### Cenário 2: Usuário não-root
```gherkin
Dado que a imagem "arremateai/<servico>:latest" foi construída
Quando eu executo "docker run --rm arremateai/<servico>:latest whoami"
Então a saída deve ser "appuser"
```

### Cenário 3: Porta correta exposta
```gherkin
Dado que a imagem foi construída
Quando eu inspeciono com "docker inspect"
Então a porta exposta deve ser "<porta>/tcp"

Exemplos:
  | servico           | porta |
  | gateway           | 8080  |
  | identity          | 8081  |
  | userprofile       | 8085  |
  | property-catalog  | 8082  |
```

### Cenário 4: HEALTHCHECK configurado
```gherkin
Dado que a imagem foi construída
Quando eu inspeciono o HEALTHCHECK
Então deve conter chamada a "/actuator/health"
E intervalo=30s, timeout=10s, retries=3, start-period=40s
```

### Cenário 5: Gateway sobe sem banco de dados
```gherkin
Dado que o Gateway NÃO usa banco de dados
Quando eu inicio o container apenas com JWT_SECRET definido
Então /actuator/health retorna 200
```

### Cenário 6: Serviço falha sem banco disponível
```gherkin
Dado que o container foi iniciado com DB_HOST apontando para host inexistente
Quando o Spring Boot tenta inicializar
Então o container encerra com exit code diferente de 0
```

### Cenário 7: Tamanho da imagem
```gherkin
Dado que a imagem foi construída
Quando verifico o tamanho
Então deve ser inferior a 300MB
```

## Particularidades por Serviço

### Gateway (porta 8080)
- NÃO usa banco de dados. Sem variáveis DB_*
- Usa `application.yml`
- Precisa de `JWT_SECRET` para validar tokens
- Spring Cloud Gateway (reativo/WebFlux)

### Identity (porta 8081)
- Flyway ativo — precisa de banco para iniciar
- `JWT_SECRET` é obrigatório e sem default
- Variáveis opcionais: `TWO_FA_ENABLED`, `MAIL_HOST`, etc.

### Userprofile (porta 8085)
- Diretório `uploads/` NÃO deve ser copiado para a imagem
- `AVATAR_STORAGE_PATH` deve apontar para volume Docker em produção

### Property-Catalog (porta 8082)
- Flyway ativo — precisa de banco para iniciar
- Serviço mais simples em variáveis de ambiente

## Padrão do .dockerignore
```
target/
.git/
.gitignore
.idea/
*.iml
.vscode/
.mvn/
mvnw
mvnw.cmd
*.md
.env
.env.*
docker-compose*.yml
Dockerfile
.dockerignore
uploads/
.DS_Store
Thumbs.db
```

## Definição de Pronto
- [ ] Dockerfile criado na raiz dos 4 repositórios
- [ ] .dockerignore criado na raiz dos 4 repositórios
- [ ] `docker build` passa sem erros nos 4 serviços
- [ ] Containers rodam com usuário não-root
- [ ] HEALTHCHECK retorna `healthy` quando serviço UP
- [ ] Gateway inicia sem banco de dados
- [ ] Imagens finais < 300MB cada
- [ ] Nenhum secret hardcoded
