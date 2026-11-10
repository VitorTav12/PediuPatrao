# Pediu Patrão — Dashboard Administrativo

Dashboard administrativo integrado ao sistema de pedidos **Pediu Patrão**. O painel consome uma API REST própria (`/api/dashboard`) para que o responsável pelo estabelecimento acompanhe vendas, situação dos pedidos e produtos mais vendidos.

**Tecnologias:** Java 21, Spring Boot, Spring Security, Spring Data MongoDB, Thymeleaf, Chart.js.

- Vídeo de demonstração: https://youtu.be/tmzNT3SA_TI
- Evidências de teste: [Evidencias.pdf](Evidencias.pdf)

---

## 1. Como executar

**Pré-requisitos:** Java 21 e MongoDB em execução.

**Variáveis de ambiente (opcionais):**

| Variável | Descrição | Padrão |
|---|---|---|
| `MONGO_URI` | URI de conexão com o MongoDB | `mongodb://<usuario>:<senha>@localhost:27017/pizzaria?authSource=admin` (ajuste para o seu Mongo) |
| `MONGO_DB` | Nome do banco | `pizzaria` |

**Subir a aplicação:**

```bash
./mvnw spring-boot:run
```

A aplicação fica em `http://localhost:8080`.

**Rodar os testes:**

```bash
./mvnw test
```

**Acesso inicial:** na primeira execução, se não houver nenhum ADMIN, é criado o usuário `admin` com a senha `teste123` (apenas para ambiente de teste). Pela tela de usuários, o ADMIN cria contas `GERENTE` e `ATENDENTE`.

**Acesso ao dashboard:** menu lateral ou `http://localhost:8080/admin`, com perfil `ADMIN` ou `GERENTE`.

---

## 2. Requisitos

### 2.1 Requisitos funcionais

| Código | Requisito | Como foi atendido |
|---|---|---|
| RF01 | Indicadores gerais de vendas | Cards com faturamento, pedidos válidos, ticket médio e percentual de cancelamento. A API também devolve total de pedidos, cancelados e total de descontos. |
| RF02 | Análise de vendas (gráficos) | Gráfico de linha com o faturamento por dia e gráfico de barras com as vendas por hora do dia. |
| RF03 | Situação dos pedidos | Gráfico de rosca com a quantidade de pedidos por status. |
| RF04 | Produtos mais vendidos | Tabela com o ranking por quantidade vendida (desempate por faturamento). |
| RF05 | Filtros de consulta | Filtros por data inicial, data final e status, aplicados a todos os indicadores, gráficos e ao ranking. |
| RF06 | Integração com a API | A tela consome os 5 endpoints com `fetch`, de forma assíncrona. |
| RF07 | Atualização e tratamento de dados | Indicador de carregamento, mensagem de erro amigável quando a API recusa a consulta (ex.: período inválido) e botão para limpar os filtros. |

### 2.2 Requisitos não funcionais

| Código | Requisito |
|---|---|
| RNF01 | **Segurança de acesso:** a tela (`/admin`) e a API (`/api/dashboard/**`) só aceitam os perfis `ADMIN` e `GERENTE`, com a regra aplicada no backend pelo Spring Security. Só o método `GET` é permitido na API do dashboard. |
| RNF02 | **Limites de consulta:** períodos acima de 366 dias e `limite` fora de 1 a 20 são rejeitados com status 400. Sem filtro de data, o padrão é o período dos últimos 30 dias. |
| RNF03 | **Formato de comunicação:** os endpoints do dashboard respondem em `application/json`. As mensagens de erro de validação são devolvidas em texto simples (`text/plain`). |
| RNF04 | **Persistência:** os pedidos ficam no MongoDB. O filtro por período e status é feito na consulta ao banco (`@Query`), e os indicadores são calculados na camada de serviço (`DashboardService`). |
| RNF05 | **Qualidade:** regras de negócio na camada de serviço, tratamento centralizado de erros da API e testes automatizados (87 testes). |

---

## 3. Documentação da API

**URL base:** `/api/dashboard`
**Autenticação:** obrigatória (sessão), perfis `ADMIN` ou `GERENTE`.

**Parâmetros de query aceitos em todos os endpoints:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `de` | data `AAAA-MM-DD` | não | Data inicial. Padrão: 29 dias antes da data final. |
| `ate` | data `AAAA-MM-DD` | não | Data final. Padrão: hoje. |
| `status` | texto | não | Um de `RECEBIDO`, `EM_PREPARO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO`, `CANCELADO`. Sem o parâmetro, considera todos. |

**Regras de cálculo:** pedidos `CANCELADO` não entram no faturamento, no ticket médio nem nos gráficos de venda. Eles são contados em `pedidosCancelados` e no percentual de cancelamento.

### 3.1 Resumo de indicadores — `GET /api/dashboard/resumo`

```http
GET /api/dashboard/resumo?de=2026-11-01&ate=2026-11-09
```

Resposta `200 OK`:

```json
{
  "de": "2026-11-01",
  "ate": "2026-11-09",
  "totalPedidos": 5,
  "pedidosValidos": 5,
  "pedidosCancelados": 0,
  "faturamento": 345.0,
  "ticketMedio": 69.0,
  "totalDescontos": 5.0,
  "percentualCancelamento": 0.0
}
```

### 3.2 Vendas por dia — `GET /api/dashboard/vendas-por-dia`

Devolve um item para cada dia do período, com zero nos dias sem venda.

```json
[
  { "data": "2026-11-08", "pedidos": 3, "faturamento": 200.0 },
  { "data": "2026-11-09", "pedidos": 2, "faturamento": 145.0 }
]
```

### 3.3 Vendas por hora — `GET /api/dashboard/vendas-por-hora`

Devolve sempre 24 itens (hora 0 a 23).

```json
[
  { "hora": 19, "pedidos": 2, "faturamento": 120.0 },
  { "hora": 20, "pedidos": 3, "faturamento": 225.0 }
]
```

### 3.4 Pedidos por status — `GET /api/dashboard/pedidos-por-status`

Devolve um item para cada status, com zero quando não há pedidos.

```json
[
  { "status": "RECEBIDO", "quantidade": 2 },
  { "status": "FINALIZADO", "quantidade": 3 },
  { "status": "CANCELADO", "quantidade": 0 }
]
```

### 3.5 Produtos mais vendidos — `GET /api/dashboard/produtos-mais-vendidos`

Parâmetro extra: `limite` (inteiro de 1 a 20, padrão 5).

```http
GET /api/dashboard/produtos-mais-vendidos?limite=3&de=2026-11-01
```

```json
[
  { "produtoId": "6730a1...", "nome": "Pizza Calabresa", "quantidade": 15, "faturamento": 750.0 },
  { "produtoId": "6730a2...", "nome": "Coca-Cola 2L", "quantidade": 12, "faturamento": 180.0 }
]
```

### 3.6 Erros possíveis

| Status | Quando acontece | Exemplo de mensagem |
|---|---|---|
| `400 Bad Request` | Data inicial posterior à final | `A data inicial não pode ser posterior à data final.` |
| `400 Bad Request` | Período maior que 366 dias | `O período máximo permitido é de 366 dias.` |
| `400 Bad Request` | `limite` fora de 1 a 20 (só em produtos) | `O limite deve estar entre 1 e 20.` |
| `400 Bad Request` | `status` ou data em formato inválido | `Valor inválido para o parâmetro status. Valores aceitos: [...]` |
| `401 / redirecionamento ao login` | Usuário não autenticado | — |
| `403 Forbidden` | Perfil sem permissão (ex.: `ATENDENTE`) ou método diferente de `GET` | Mensagem de acesso negado |

---

## 4. Evidências de teste

- **Testes automatizados:** 87 testes cobrindo regras de negócio, controllers e restrições de segurança. Resultado da última execução (`./mvnw test`):

```text
Tests run: 87, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- **Evidências visuais:** o arquivo [Evidencias.pdf](Evidencias.pdf) traz o erro tratado na tela (RF07), o dashboard funcionando e a resposta JSON de `/api/dashboard/resumo`.

---

## 5. Organização do repositório (gitflow)

| Branch | Uso |
|---|---|
| `main` | Versão final entregue. |
| `develop` | Integração das funcionalidades. |
| `feature/dashboard-api` | Implementação dos endpoints do dashboard. |
| `feature/dashboard-tela` | Tela do dashboard e integração com a API. |

---

## 6. Estrutura principal do código

```text
src/main/java/com/umc/pediupatrao/
├── controller/DashboardApiController.java   # endpoints /api/dashboard
├── service/DashboardService.java            # cálculo dos indicadores
├── dto/DashboardDTO.java                    # formatos de resposta
├── repository/PedidoRepository.java         # consultas por período e status
└── config/SecurityConfig.java               # regras de acesso
src/main/resources/templates/admin.html      # tela do dashboard
src/test/java/                               # testes automatizados
```
