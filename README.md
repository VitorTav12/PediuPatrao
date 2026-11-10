# Documentação Técnica - Dashboard Administrativo "Pediu Patrão"

## 1. Requisitos do Sistema

### Requisitos Funcionais (RF)
* **RF01 — Indicadores gerais de vendas:** O sistema deve calcular e exibir o faturamento total, quantidade de pedidos válidos, ticket médio e percentual de cancelamento.
* **RF02 — Análise de vendas (gráficos):** O sistema deve exibir gráficos de linhas para faturamento por dia e gráficos de barras para vendas por horário (picos de movimento).
* **RF03 — Situação dos pedidos:** O sistema deve apresentar um gráfico consolidado (Doughnut/Pizza) demonstrando a distribuição dos pedidos por status.
* **RF04 — Produtos mais vendidos:** O sistema deve exibir uma tabela com o ranking dos produtos mais populares, ordenados por quantidade vendida e faturamento gerado.
* **RF05 — Filtros de consulta:** O dashboard deve permitir a filtragem dinâmica de todos os indicadores e gráficos por `data de início`, `data de fim` e `status` do pedido.
* **RF06 — Integração com a API:** A interface deve consumir os dados dinamicamente via requisições assíncronas (Fetch API) diretamente dos endpoints do backend.
* **RF07 — Atualização e tratamento de dados:** A interface deve gerenciar estados de carregamento (spinner) e exibir mensagens de erro amigáveis caso a API recuse a consulta (ex: períodos inválidos).

### Requisitos Não Funcionais (RNF)
* **RNF01 — Segurança de Acesso:** As rotas visuais (`/admin`) e os endpoints da API (`/api/dashboard/**`) devem ser estritamente protegidas pelo Spring Security, permitindo acesso apenas aos perfis `ADMIN` e `GERENTE`.
* **RNF02 — Performance e Limites:** A API deve rejeitar automaticamente filtros de datas que excedam o intervalo de 366 dias (Status 400), prevenindo sobrecarga de processamento no banco de dados.
* **RNF03 — Formato de Comunicação:** Todos os endpoints RESTful devem consumir e produzir dados no formato `application/json`.
* **RNF04 — Arquitetura de Banco de Dados:** As queries de agregação (Aggregation Pipeline) devem ser executadas nativamente no MongoDB para garantir alta performance no agrupamento de milhares de pedidos.

---

## 2. Documentação da API REST (Endpoints)

A API do Dashboard foi projetada de forma modularizada, com um endpoint específico para cada componente da tela. Isso permite renderização progressiva no frontend.

**URL Base:** `/api/dashboard`  
**Autenticação Obrigatória:** Sim (Cookie/Session, Perfis: `ADMIN`, `GERENTE`).  
**Parâmetros de Query (Aceites em todos os endpoints):**
* `de` (String, opcional): Data de início no formato `YYYY-MM-DD`.
* `ate` (String, opcional): Data final no formato `YYYY-MM-DD`.
* `status` (String, opcional): Filtra por status específico (ex: `FINALIZADO`, `RECEBIDO`).

### 2.1. Resumo de Indicadores
* **Endpoint:** `GET /resumo`
* **Descrição:** Retorna os KPIs globais do período selecionado.
* **Códigos de Resposta:**
  * `200 OK`: Sucesso.
  * `400 Bad Request`: Data mal formatada ou intervalo superior a 366 dias.
* **Exemplo de Resposta (200 OK):**
```json
{
  "faturamento": 2450.75,
  "pedidosValidos": 42,
  "ticketMedio": 58.35,
  "percentualCancelamento": 4.5
}

2.2. Vendas por Dia
Endpoint: GET /vendas-por-dia

Descrição: Agrupa o faturamento por dia, preenchendo datas vazias com valor zero.

Exemplo de Resposta (200 OK):

JSON
[
  { "data": "2026-11-08", "faturamento": 1200.00 },
  { "data": "2026-11-09", "faturamento": 1250.75 }
]
2.3. Pedidos por Status
Endpoint: GET /pedidos-por-status

Descrição: Conta a quantidade de pedidos em cada etapa do funil (Em Preparo, Saiu para Entrega, etc).

Exemplo de Resposta (200 OK):

JSON
[
  { "status": "FINALIZADO", "quantidade": 38 },
  { "status": "CANCELADO", "quantidade": 2 },
  { "status": "EM_PREPARO", "quantidade": 4 }
]
2.4. Vendas por Hora
Endpoint: GET /vendas-por-hora

Descrição: Identifica os picos de faturamento agrupados pela hora do dia (0h às 23h).

Exemplo de Resposta (200 OK):

JSON
[
  { "hora": 19, "faturamento": 800.00 },
  { "hora": 20, "faturamento": 1450.50 }
]
2.5. Produtos Mais Vendidos
Endpoint: GET /produtos-mais-vendidos?limite=5

Descrição: Lista o ranking de produtos com base no volume de itens vendidos (padrão: top 5).

Exemplo de Resposta (200 OK):

JSON
[
  { "nome": "Pizza Calabresa", "quantidade": 15, "faturamento": 750.00 },
  { "nome": "Coca-Cola 2L", "quantidade": 12, "faturamento": 180.00 }
]
3. Evidências de Teste e Validação
3.1. Execução de Testes Automatizados (Unitários e Integração)
O projeto conta com uma suíte de testes robusta que cobre as regras de negócio, controladores de API e as restrições de segurança (Spring Security). Abaixo encontra-se o registo oficial da execução dos testes via Maven, comprovando o sucesso e a estabilidade da aplicação:

Plaintext
Results:

Tests run: 87, Failures: 0, Errors: 0, Skipped: 0

------------------------------------------------------------------------
BUILD SUCCESS
------------------------------------------------------------------------
Total time:  10.811 s
Finished at: 2026-11-09T22:56:51-03:00
------------------------------------------------------------------------