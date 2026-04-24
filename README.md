# 🚪 ArremateAI - API Gateway

![CI](https://github.com/Quintanilha09/arremateai-gateway/actions/workflows/ci.yml/badge.svg)

Gateway centralizado para gerenciamento de rotas, autenticação, rate limiting e circuit breaker da arquitetura de microsserviços ArremateAI.

## 📋 Descrição

O API Gateway é o ponto de entrada único para todos os clientes (web, mobile, APIs externas). Implementa:

- **Roteamento inteligente** para microsserviços downstream
- **Validação JWT** centralizada
- **Rate Limiting** para proteção contra abuso
- **Circuit Breaker** para resiliência
- **Logging e monitoramento** de todas as requisições

## 🛠️ Tecnologias

- **Java 17** (LTS)
- **Spring Boot 3.2.2**
- **Spring Cloud Gateway 2023.0.0** - Reactive routing
- **Resilience4j** - Circuit breaker e rate limiting
- **Redis** - Cache de tokens e rate limiting distribuído
- **JJWT 0.12.3** - Validação de tokens JWT

## 🏗️ Arquitetura

```
Cliente → API Gateway (8080)
              ↓ Valida JWT
              ↓ Rate Limiting
              ↓ Circuit Breaker
              ├─→ Identity Service (8081)
              ├─→ UserProfile Service (8082)
              ├─→ Vendor Service (8083)
              ├─→ Property Catalog (8084)
              ├─→ Media Service (8085)
              └─→ Notification Service (8086)
```

## 📦 Estrutura do Projeto

```
src/main/java/com/arremateai/gateway/
├── GatewayApplication.java          # Main application
├── filter/
│   └── JwtValidacaoFilter.java      # Filtro de validação JWT
└── security/
    └── JwtService.java              # Serviço de processamento JWT
```

## 🔐 Segurança

### Validação JWT
Todas as rotas (exceto `/auth/**`, `/public/**`) exigem token JWT válido no header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Rate Limiting
- **10 requisições/segundo** por IP
- **Burst de 20 requisições**
- Cache distribuído via Redis

### Circuit Breaker (Resilience4j)
- **Sliding window**: 10 requisições
- **Failure threshold**: 50%
- **Wait duration**: 10 segundos
- **Half-open calls**: 5

## 🚀 Endpoints Roteados

| Rota | Destino | Porta | Descrição |
|------|---------|-------|-----------|
| `/auth/**` | Identity | 8081 | Autenticação e registro |
| `/api/perfil/**` | UserProfile | 8082 | Perfil de usuário |
| `/api/favoritos/**` | UserProfile | 8082 | Favoritos |
| `/api/vendedores/**` | Vendor | 8083 | Cadastro de vendedores PJ |
| `/api/documentos/**` | Vendor | 8083 | Documentos vendedor |
| `/api/imoveis/**` | Property Catalog | 8084 | CRUD de imóveis |
| `/api/leiloes/**` | Property Catalog | 8084 | Leilões |
| `/api/upload/**` | Media | 8085 | Upload de arquivos |
| `/api/notificacoes/**` | Notification | 8086 | Notificações |

## ⚙️ Variáveis de Ambiente

```bash
# Server
SERVER_PORT=8080

# JWT Configuration
JWT_SECRET=sua_chave_secreta_256_bits_minimo
JWT_EXPIRATION=3600000

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Service Discovery
IDENTITY_SERVICE_URL=http://localhost:8081
USERPROFILE_SERVICE_URL=http://localhost:8082
VENDOR_SERVICE_URL=http://localhost:8083
PROPERTY_CATALOG_SERVICE_URL=http://localhost:8084
MEDIA_SERVICE_URL=http://localhost:8085
NOTIFICATION_SERVICE_URL=http://localhost:8086
```

## 🏃 Como Executar

### Desenvolvimento Local

```bash
# Clone o repositório
git clone https://github.com/Quintanilha09/arremateai-gateway.git
cd arremateai-gateway

# Configure as variáveis de ambiente
cp .env.example .env
# Edite o .env com suas configurações

# Execute com Maven
./mvnw spring-boot:run

# Ou compile e execute o JAR
./mvnw clean package
java -jar target/arremateai-gateway-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
# Build da imagem
docker build -t arremateai-gateway:latest .

# Execute o container
docker run -d \
  --name gateway \
  -p 8080:8080 \
  -e JWT_SECRET=sua_chave_secreta \
  -e REDIS_HOST=redis \
  --network arremateai-network \
  arremateai-gateway:latest
```

### Docker Compose

```bash
docker-compose up -d gateway
```

## 📊 Monitoramento

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics (Prometheus)
```bash
curl http://localhost:8080/actuator/prometheus
```

### Circuit Breaker Status
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

## 🧪 Testes

```bash
# Executar todos os testes
./mvnw test

# Testes de integração
./mvnw verify

# Coverage report
./mvnw jacoco:report
```

## 📝 Logs

Logs estruturados em formato JSON:
```json
{
  "timestamp": "2026-03-27T10:30:00.000Z",
  "level": "INFO",
  "service": "gateway",
  "traceId": "abc123",
  "spanId": "def456",
  "method": "GET",
  "path": "/api/imoveis",
  "status": 200,
  "duration": 45
}
```

## 🔧 Troubleshooting

### Gateway não inicia
```bash
# Verifique se a porta 8080 está livre
netstat -ano | findstr :8080

# Verifique conexão com Redis
redis-cli -h localhost -p 6379 ping
```

### Token JWT inválido
- Verifique se `JWT_SECRET` é o mesmo em Identity e Gateway
- Confirme que o token não expirou (TTL padrão: 1 hora)

### Rate Limit atingido
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 60 seconds",
  "status": 429
}
```
Aguarde o tempo especificado ou use outro IP.

## 📚 Documentação Adicional

- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)
- [Resilience4j Guide](https://resilience4j.readme.io/)
- [Architecture Decision Records](../arremateai-docs/ADRs/)

## 📄 Licença

Proprietary - © 2026 ArremateAI

## 👥 Contribuidores

- **Gabriel Quintanilha** - Desenvolvedor Principal
