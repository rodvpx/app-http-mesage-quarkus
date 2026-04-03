# Relatório Técnico — Sistema de Troca de Mensagens com Quarkus

> **Disciplina:** Sistemas Distribuídos  
> **Framework:** Quarkus (JAX-RS / RESTEasy)  
> **Linguagem:** Java 21

---

## Sumário

1. [Objetivo](#1-objetivo)
2. [Contexto Teórico](#2-contexto-teórico)
3. [Arquitetura da Solução](#3-arquitetura-da-solução)
4. [Modelo de Dados](#4-modelo-de-dados)
5. [Implementação dos Endpoints](#5-implementação-dos-endpoints)
6. [Evidências de Funcionamento](#6-evidências-de-funcionamento)
7. [Justificativa dos Status Codes](#7-justificativa-dos-status-codes)

---

## 1. Objetivo

Implementar uma aplicação distribuída simples utilizando o framework **Quarkus**, explorando o protocolo **HTTP** como mecanismo de comunicação direta entre processos. A prática relaciona-se ao modelo teórico de comunicação **send/receive** estudado em Sistemas Distribuídos, onde um processo envia uma mensagem e outro a recebe e processa.

---

## 2. Contexto Teórico

Em sistemas distribuídos, a comunicação entre processos pode ocorrer de forma direta — o modelo **send/receive** — onde um processo envia uma mensagem e espera que outro a receba e processe. O protocolo **HTTP**, construído sobre o TCP, é uma das implementações mais amplamente utilizadas desse modelo na web moderna.

Frameworks como o **Quarkus** abstraem os detalhes de baixo nível do TCP e oferecem uma forma estruturada de expor e consumir serviços via HTTP. Em comparação ao Spring Boot, o Quarkus foi projetado especificamente para ambientes de **nuvem e microsserviços**, com:

- Tempo de inicialização reduzido (fast startup);
- Menor consumo de memória (low memory footprint);
- Suporte nativo a compilação AOT (GraalVM Native Image).

Essas características tornam o Quarkus especialmente adequado para sistemas distribuídos modernos que exigem elasticidade e eficiência de recursos.

> **JAX-RS** é uma *especificação* (não uma implementação). Ela define o contrato — anotações e interfaces — que outros frameworks implementam. No Quarkus, a implementação padrão de JAX-RS é o **RESTEasy**; no Spring Boot, é comumente utilizado o **Jersey**.

---

## 3. Arquitetura da Solução

### 3.1 Visão Geral

```
┌─────────────────────┐         HTTP/TCP          ┌──────────────────────────┐
│                     │  ──── POST /message ────►  │                          │
│   Cliente (Postman) │                            │   Servidor (Quarkus)     │
│     [SENDER]        │  ◄─── 201 Created ──────   │   [RECEIVER]             │
│                     │                            │                          │
└─────────────────────┘                            └──────────────────────────┘
                                                           │
                                                   ┌───────┴────────┐
                                                   │ MessageService │
                                                   │ (List<> em     │
                                                   │  memória)      │
                                                   └────────────────┘
```

### 3.2 Fluxo de uma Requisição `POST /message`

1. **Cliente (Postman / Sender)** envia uma requisição HTTP com método `POST` para `http://localhost:8080/message`, incluindo no corpo os campos `sender` e `content`.

2. **Protocolo HTTP encapsula** a mensagem em um envelope com cabeçalhos (`Content-Type: application/json`) e corpo (payload JSON), transmitindo sobre a camada TCP.

3. **Servidor Quarkus (Receiver)** recebe a requisição no `MessageController`, que está mapeado via `@Path("/message")` e `@POST`. O RESTEasy desserializa o JSON para um objeto `MessageEntity`.

4. **`MessageService`** é acionado: gera um `id` único via `AtomicLong`, adiciona o timestamp atual e armazena o objeto na lista em memória.

5. O controlador retorna `Response.status(201 Created).entity(createdMessage)`, que é serializado de volta a JSON e enviado ao cliente como resposta HTTP.

### 3.3 Mapeamento Teórico: HTTP ↔ Send/Receive

| Método HTTP | Operação Distribuída | Descrição |
|-------------|----------------------|-----------|
| `POST`      | **Send**             | O cliente *envia* uma nova mensagem ao processo servidor, que a armazena. Corresponde diretamente à primitiva `send(message)`. |
| `GET`       | **Receive / Read**   | O cliente *recebe* mensagens armazenadas pelo servidor. Equivale à primitiva `receive()` — o processo recupera dados de uma caixa de mensagens compartilhada. |
| `DELETE`    | **Remove**           | O cliente solicita a remoção de uma mensagem do estado do servidor, operação análoga a descartar uma mensagem já processada do buffer de recebimento. |

---

## 4. Modelo de Dados

A classe `MessageEntity` representa a mensagem trafegada entre os processos:

```java
package org.rodvpx.entity;

import java.time.LocalDateTime;

public class MessageEntity {
    public Long id;
    public String sender;
    public String content;
    public LocalDateTime timestamp;
}
```

| Campo       | Tipo              | Descrição                                      |
|-------------|-------------------|------------------------------------------------|
| `id`        | `Long`            | Identificador único gerado pelo servidor       |
| `sender`    | `String`          | Identificador do processo remetente            |
| `content`   | `String`          | Conteúdo da mensagem                           |
| `timestamp` | `LocalDateTime`   | Momento em que a mensagem foi registrada       |

---

## 5. Implementação dos Endpoints

### `MessageController.java`

```java
@Path("/message")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageController {

    @Inject
    MessageService messageService;

    @GET
    public Response findAll() { ... }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) { ... }

    @POST
    public Response sendMessage(MessageEntity message) { ... }

    @DELETE
    @Path("/{id}")
    public Response deleteMessage(@PathParam("id") Long id) { ... }
}
```

### `MessageService.java`

Utiliza uma **lista em memória** (`ArrayList`) e um contador atômico (`AtomicLong`) para simular persistência sem banco de dados:

```java
@ApplicationScoped
public class MessageService {
    private List<MessageEntity> messages = new ArrayList<>();
    private AtomicLong counter = new AtomicLong();
    // findAllMessages, findbyId, sendMessage, deleteMessage
}
```

### Tabela de Endpoints

| Método   | Rota            | Descrição                                  | Status de Sucesso |
|----------|-----------------|--------------------------------------------|-------------------|
| `GET`    | `/message`      | Retorna todas as mensagens armazenadas     | `200 OK` / `204 No Content` |
| `GET`    | `/message/{id}` | Retorna uma mensagem pelo ID               | `200 OK`          |
| `POST`   | `/message`      | Envia/cria uma nova mensagem               | `201 Created`     |
| `DELETE` | `/message/{id}` | Remove uma mensagem pelo ID                | `204 No Content`  |

---

## 6. Evidências de Funcionamento

### 6.1 `POST /message` — 201 Created

Criação de uma nova mensagem com `sender` e `content`. O servidor retorna o objeto completo com `id` e `timestamp` gerados.

![POST 201 Created](img/status_201_mensagem_criada_enviada.png)

**Request Body:**
```json
{
  "sender": "Rodrigo",
  "content": "Tudo bem?"
}
```

**Response Body:**
```json
{
  "id": 2,
  "sender": "Rodrigo",
  "content": "Tudo bem?",
  "timestamp": "2026-04-03T18:50:07.589734341"
}
```

---

### 6.2 `GET /message` — 200 OK (com mensagens)

Retorna a lista completa de mensagens armazenadas em memória.

![GET 200 OK todas as mensagens](img/status_200_todas_as_mensagens.png)

**Response Body:**
```json
[
  {
    "id": 2,
    "sender": "Rodrigo",
    "content": "Tudo bem?",
    "timestamp": "2026-04-03T18:50:07.589734341"
  },
  {
    "id": 3,
    "sender": "Tiago",
    "content": "Bão de mais e ocê??",
    "timestamp": "2026-04-03T18:51:53.988768276"
  }
]
```

---

### 6.3 `GET /message` — 204 No Content (lista vazia)

Quando não há mensagens armazenadas, o servidor retorna `204 No Content` sem corpo.

![GET 204 No Content lista vazia](img/Status_204_lista_vazia_de_mensagens.png)

---

### 6.4 `GET /message/{id}` — 200 OK

Busca de uma mensagem específica pelo seu `id`.

![GET 200 OK mensagem por id](img/status_200_mensagem_encontrada_por_id.png)

**Requisição:** `GET http://localhost:8080/message/2`

**Response Body:**
```json
{
  "id": 2,
  "sender": "Rodrigo",
  "content": "Tudo bem?",
  "timestamp": "2026-04-03T18:50:07.589734341"
}
```

---

### 6.5 `GET /message/{id}` — 404 Not Found

Quando o `id` informado não existe na lista em memória.

![GET 404 Not Found](img/status_404_message_não_encontrada.png)

**Requisição:** `GET http://localhost:8080/message/1`  
**Resposta:** `404 Not Found` — sem corpo.

---

### 6.6 `DELETE /message/{id}` — 204 No Content

Remoção bem-sucedida de uma mensagem existente.

![DELETE 204 No Content](img/status_204_mensagem_deletada.png)

**Requisição:** `DELETE http://localhost:8080/message/2`  
**Resposta:** `204 No Content` — sem corpo.

---

### 6.7 `DELETE /message/{id}` — 404 Not Found

Tentativa de deletar uma mensagem com `id` inexistente (ou já removida).

![DELETE 404 Not Found](img/status_404_mensagem_não_encontrada_para_deletar.png)

**Requisição:** `DELETE http://localhost:8080/message/1`  
**Resposta:** `404 Not Found` — sem corpo.

---

## 7. Justificativa dos Status Codes

| Status Code        | Método   | Cenário                                                                                                    |
|--------------------|----------|------------------------------------------------------------------------------------------------------------|
| `200 OK`           | `GET`    | A requisição foi processada com sucesso e há conteúdo a retornar (mensagem ou lista não vazia).            |
| `201 Created`      | `POST`   | Um novo recurso (mensagem) foi criado com sucesso no servidor. Semanticamente mais preciso que `200 OK`.   |
| `204 No Content`   | `GET`    | A requisição foi bem-sucedida, mas a lista está vazia — não há corpo para retornar.                        |
| `204 No Content`   | `DELETE` | A remoção foi bem-sucedida. Não há corpo a retornar, pois o recurso foi deletado.                          |
| `404 Not Found`    | `GET`    | O `id` informado não corresponde a nenhuma mensagem na lista em memória.                                   |
| `404 Not Found`    | `DELETE` | O `id` informado não foi encontrado, portanto nenhuma remoção ocorreu.                                     |
| `400 Bad Request`  | `POST`   | O corpo da requisição está incompleto (`sender` ou `content` nulos/em branco). Validado no controller.     |

---