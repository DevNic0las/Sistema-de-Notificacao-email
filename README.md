# Sistema de Notificação

Sistema de notificação por email baseado em microserviços com comunicação assíncrona via RabbitMQ. Desenvolvido com Spring Boot 3 e Java 21, implementa envio de emails transacionais com retry automático e dead-letter queue para garantia de entrega.

---

## Problema que Resolve

Em sistemas com múltiplos perfis de usuário, notificações por email precisam ser confiáveis e não podem bloquear o fluxo principal da aplicação. Uma falha temporária no servidor de email não deve impedir o cadastro de um usuário ou a entrega de uma notificação administrativa.

Este sistema resolve isso desacoplando o disparo do evento do envio do email via mensageria assíncrona, com retry automático com backoff exponencial e dead-letter queue para rastreabilidade de falhas.

---

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.5.x | Framework base |
| Spring AMQP | — | Integração com RabbitMQ |
| Spring Data JPA | — | Persistência |
| Spring Security | 6.x | Encoding de senha (BCrypt) |
| Spring Mail | — | Envio via SMTP |
| MySQL | 8.0 | Banco de dados do `user-service` |
| RabbitMQ | 3.13 | Message broker |
| Docker + Compose | — | Containerização |
| JUnit 5 + Mockito | — | Testes unitários |
| Maven | 3.9.x | Build |

---

## Arquitetura

O sistema é composto por dois microserviços independentes que se comunicam exclusivamente via RabbitMQ — nenhum serviço chama o outro diretamente.

```
┌──────────────┐     HTTP      ┌─────────────────────────┐
│    CLIENT    │ ────────────► │    user-service :8080    │
└──────────────┘               │                         │
                               │  - Gerencia usuários    │
                               │  - Valida e persiste    │
                               │  - Publica eventos      │
                               └───────────┬─────────────┘
                                           │ AMQP
                                           ▼
                               ┌─────────────────────────┐
                               │     RabbitMQ :5672       │
                               │                         │
                               │  user.exchange          │
                               │  staff.exchange         │
                               │  Retry queues (TTL)     │
                               │  Dead-letter queues     │
                               └───────────┬─────────────┘
                                           │ AMQP
                                           ▼
                               ┌─────────────────────────┐
                               │ notification-service     │
                               │         :8081            │
                               │                         │
                               │  - Consome eventos      │
                               │  - Envia emails SMTP    │
                               │  - Gerencia retries     │
                               └─────────────────────────┘
```

### Estratégia de Retry com Backoff Exponencial

Aplicada tanto para emails de boas-vindas (`user.email.queue`) quanto para notificações de staff (`staff.queue`):

```
fila principal
    │ falha → basicNack
    ▼
retry.exchange
    ├── retry.1 (TTL  5s) → expira → fila principal
    ├── retry.2 (TTL 20s) → expira → fila principal
    └── retry.3 (TTL 60s) → expira → fila principal

Após 4 falhas totais → DLX → DLQ (mensagem retida para análise)
```

---

## Estrutura de Pastas

```
sistema-de-notificacao/
├── docker-compose.yml              # Orquestra todos os serviços
├── env.example                     # Template de variáveis — copie para .env
│
├── user-service/                   # Gerencia usuários e publica eventos
│   ├── Dockerfile
│   └── src/main/java/br/com/nicolas/user_service/
│       ├── config/
│       │   ├── rabbitmq/
│       │   │   └── RabbitmqConfig.java     # RabbitTemplate com JSON converter
│       │   └── security/
│       │       └── SecurityConfig.java     # BCrypt + CSRF desabilitado
│       ├── exceptions/
│       │   ├── EmailAlreadyExistsException.java
│       │   ├── ErroResponse.java           # Record de resposta de erro
│       │   └── HandlerException.java       # @RestControllerAdvice global
│       └── user/
│           ├── User.java                   # Entidade JPA
│           ├── UserRepository.java         # findByRole, existsByEmail
│           ├── UserRoles.java              # Enum: ADMIN, STAFF, CLIENT
│           ├── controller/
│           │   ├── UserController.java     # POST /api/user
│           │   └── AdminController.java    # POST /api/admin/notifications/staff
│           ├── dtos/
│           │   ├── UserRequestDTO.java     # Input com Bean Validation
│           │   ├── UserResponseDTO.java    # Output público (sem senha)
│           │   ├── EmailDTO.java           # Payload publicado na fila
│           │   └── NotificationDTO.java    # Input de notificação admin
│           ├── producer/
│           │   ├── UserProducer.java       # Publica em user.exchange
│           │   └── AdminProducer.java      # Publica em staff.exchange
│           └── service/
│               ├── UserService.java        # Criação de usuário
│               └── AdminService.java       # Notificação para STAFF
│
└── notification-service/           # Consome eventos e envia emails
    ├── Dockerfile
    └── src/main/java/br/com/nicolas/notification_service/
        ├── config/
        │   └── RabbitmqConfig.java     # Filas, exchanges, retry e DLQ
        ├── consumer/
        │   ├── UserConsumer.java       # Consome user.email.queue
        │   └── StaffConsumer.java      # Consome staff.queue
        ├── dtos/
        │   ├── EmailDTO.java
        │   └── UserResponseDTO.java
        └── service/
            └── EmailService.java       # Envia email via JavaMailSender
```

---

## Fluxo da Aplicação

### Fluxo 1 — Criação de Usuário

```
POST /api/user
    ↓
UserController → UserService.createUser() [@Transactional]
    ├── Verifica duplicidade de email → lança EmailAlreadyExistsException se duplicado
    ├── Aplica BCrypt na senha
    ├── Persiste no MySQL
    ├── Publica UserResponseDTO em user.exchange (routing: user.created)
    └── Retorna 201 Created com Location header

RabbitMQ: user.email.queue
    ↓
UserConsumer.listenUserCreated() [notification-service]
    ├── Constrói email de boas-vindas
    ├── EmailService.send() via SMTP
    ├── Sucesso → basicAck()
    └── Falha → retry (5s / 20s / 60s) → após 4 falhas → user.email.dlq
```

### Fluxo 2 — Notificação Administrativa para Staff

```
POST /api/admin/notifications/staff
    ↓
AdminController → AdminService.sendEmailToStaff()
    ├── Busca todos os Users com role = STAFF
    └── Para cada staff: publica EmailDTO em staff.exchange

RabbitMQ: staff.queue
    ↓
StaffConsumer.listenStaff() [notification-service]
    ├── EmailService.send() via SMTP
    ├── Sucesso → basicAck()
    └── Falha → retry (5s / 20s / 60s) → após 4 falhas → staff.email.dlq
```

---

## Regras de Negócio

- Email deve ser único — cadastro duplicado retorna `400 Bad Request`
- Senha deve ter mínimo 8 caracteres com maiúscula, minúscula, número e caractere especial (`@#$%^&+=!`)
- Senha sempre armazenada com hash BCrypt — nunca em plain text
- Ao criar um usuário, email de boas-vindas é disparado de forma assíncrona
- Falhas no envio são retentadas automaticamente com backoff: 5s, 20s e 60s
- Após 4 falhas consecutivas, mensagem vai para DLQ para análise manual
- Notificações administrativas são enviadas para todos os usuários com role `STAFF`
- O endpoint de notificação retorna `202 Accepted` imediatamente — processamento é assíncrono

---

## Como Rodar

### Pré-requisitos

- Docker e Docker Compose
- Java 21 e Maven (para rodar pelo IntelliJ)
- Conta no [Mailtrap](https://mailtrap.io) para receber emails em desenvolvimento

### Opção A — Tudo via Docker

**1.** Configure as variáveis de ambiente:

```bash
cp env.example .env
```

Edite o `.env` com seus valores:

```env
DB_ROOT_PASSWORD=suasenha

RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USER=seu_usuario_mailtrap
MAIL_PASSWORD=sua_senha_mailtrap
```

**2.** Suba todos os serviços:

```bash
docker-compose up --build
```

### Opção B — Infraestrutura no Docker, Serviços no IntelliJ

**1.** Suba MySQL e RabbitMQ:

```bash
docker-compose up -d mysql rabbitmq
```

**2.** Crie `src/main/resources/application-local.yaml` em cada serviço com credenciais locais. Adicione ao `.gitignore`.

Para o `notification-service`:
```yaml
spring:
  mail:
    username: seu_usuario_mailtrap
    password: sua_senha_mailtrap
```

**3.** Configure o profile `local` no IntelliJ: **Run → Edit Configurations → Active profiles: `local`**

**4.** Rode `UserServiceApplication` e `NotificationServiceApplication` pelo IntelliJ.

### Serviços disponíveis

| Serviço | URL |
|---|---|
| user-service | http://localhost:8080 |
| notification-service | http://localhost:8081 |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |
| MySQL | localhost:3306 |

---

## Exemplos de Uso

### Criar usuário

```bash
curl -X POST http://localhost:8080/api/user \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@example.com",
    "password": "Senha@123",
    "role": "CLIENT"
  }'
```

**201 Created:**
```json
{
  "id": 1,
  "email": "joao@example.com",
  "name": "João Silva"
}
```

**400 — Email duplicado:**
```json
{
  "status": 400,
  "message": "Email já cadastrado",
  "timestamp": 1719000000000
}
```

**400 — Senha inválida:**
```json
{
  "errors": [
    "A senha deve ter pelo menos 8 caracteres, incluindo uma letra maiúscula, uma minúscula, um número e um caractere especial"
  ]
}
```

### Enviar notificação para staff

```bash
curl -X POST http://localhost:8080/api/admin/notifications/staff \
  -H "Content-Type: application/json" \
  -d '{
    "senderName": "Sistema",
    "topic": "Manutenção programada",
    "body": "O sistema entrará em manutenção às 22h de hoje."
  }'
```

**202 Accepted** — sem body, processamento assíncrono.

Um email será enviado para cada usuário cadastrado com role `STAFF`.

---

## Variáveis de Ambiente

| Variável | Serviço | Descrição | Padrão |
|---|---|---|---|
| `DB_ROOT_PASSWORD` | mysql, user-service | Senha root MySQL | `root` |
| `RABBITMQ_USER` | ambos | Usuário RabbitMQ | `guest` |
| `RABBITMQ_PASSWORD` | ambos | Senha RabbitMQ | `guest` |
| `MAIL_HOST` | notification-service | Host SMTP | `sandbox.smtp.mailtrap.io` |
| `MAIL_PORT` | notification-service | Porta SMTP | `2525` |
| `MAIL_USER` | notification-service | Usuário SMTP | — |
| `MAIL_PASSWORD` | notification-service | Senha SMTP | — |

---

## Testes

O projeto possui testes unitários para o `UserService` cobrindo os principais cenários de negócio:

```bash
# Rodar os testes
cd user-service
./mvnw test
```

Cenários cobertos:
- Lança `EmailAlreadyExistsException` quando email já existe
- Salva usuário e publica evento quando email é novo
- Retorna `UserResponseDTO` com dados corretos após criação

---

## Melhorias Futuras

- [ ] Testes unitários para `AdminService`
- [ ] Testes de integração com Testcontainers (MySQL + RabbitMQ reais)
- [ ] Flyway para versionamento do schema em vez de `ddl-auto: update`
- [ ] Autenticação JWT na rota `/api/admin/notifications/staff`
- [ ] Endpoint para reprocessar mensagens da DLQ manualmente
- [ ] Observabilidade com Micrometer + Prometheus + Grafana
- [ ] `@Value` para externalizar o remetente de email (`emailSender`)
- [ ] Variáveis de ambiente no `user-service/application.yaml` com `${VAR:default}`
