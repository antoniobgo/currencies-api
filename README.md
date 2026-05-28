# Currencies API

Backend em Spring Boot que monitora cotações de moedas em tempo real, consumindo a [AwesomeAPI](https://economia.awesomeapi.com.br/). Armazena dados intraday durante o pregão e mantém um histórico de fechamentos diários dos últimos 5 anos.

---

## O que o sistema faz

- Busca cotações de moedas monitoradas a cada 5 minutos, de segunda a sexta, entre 9h e 18h (horário de Brasília)
- Armazena os dados intraday do dia atual e os expõe via API REST
- Ao início de cada pregão, salva o fechamento do dia anterior e limpa os dados intraday
- Mantém um histórico de fechamentos diários consultável por moeda e intervalo de datas
- Permite gerenciar quais moedas são monitoradas (adicionar, listar, remover)
- Expõe endpoints de resumo estatístico (máxima, mínima, média, variação percentual)

---

## Pré-requisitos

- Java 21
- Maven (ou use o `mvnw` incluído no projeto)
- PostgreSQL 13+

---

## Configuração do banco de dados

Crie o banco e o usuário no PostgreSQL:

```sql
CREATE DATABASE cotacoes;
CREATE USER cotacoes WITH PASSWORD 'cotacoes';
GRANT ALL PRIVILEGES ON DATABASE cotacoes TO cotacoes;
```

As tabelas são criadas automaticamente pelo Hibernate na primeira execução (`ddl-auto=update`).

---

## Executando o projeto

**1. Clone o repositório:**

```bash
git clone https://github.com/seu-usuario/currencies-api.git
cd currencies-api
```

**2. Configure o banco de dados em `src/main/resources/application.properties`:**

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cotacoes
spring.datasource.username=cotacoes
spring.datasource.password=cotacoes
```

**3. Execute:**

```bash
./mvnw spring-boot:run
```

A aplicação sobe na porta `8080` por padrão.

---

## Primeira execução

Após subir a aplicação, adicione as moedas que deseja monitorar:

```bash
curl -X POST http://localhost:8080/api/moedas \
  -H "Content-Type: application/json" \
  -d '{"code": "USD"}'

curl -X POST http://localhost:8080/api/moedas \
  -H "Content-Type: application/json" \
  -d '{"code": "EUR"}'
```

Em seguida, importe o histórico dos últimos 5 anos:

```bash
curl -X POST http://localhost:8080/api/admin/historico/inicializar
```

> Essa operação pode levar alguns minutos dependendo da quantidade de moedas monitoradas.

---

## Endpoints disponíveis

### Cotações

| Método | Endpoint                                                                     | Descrição                                    |
| ------ | ---------------------------------------------------------------------------- | -------------------------------------------- |
| GET    | `/api/cotacoes/atual`                                                        | Cotação atual de todas as moedas             |
| GET    | `/api/cotacoes/atual/{moeda}`                                                | Cotação atual de uma moeda específica        |
| GET    | `/api/cotacoes/hoje`                                                         | Registros intraday do dia de todas as moedas |
| GET    | `/api/cotacoes/hoje/{moeda}`                                                 | Registros intraday do dia de uma moeda       |
| GET    | `/api/cotacoes/historico?moeda=USD&dataInicio=2024-01-01&dataFim=2024-12-31` | Histórico de fechamentos diários             |
| GET    | `/api/cotacoes/resumo?moeda=USD&periodo=30`                                  | Resumo estatístico do período                |

### Moedas monitoradas

| Método | Endpoint              | Descrição                           |
| ------ | --------------------- | ----------------------------------- |
| GET    | `/api/moedas`         | Lista todas as moedas monitoradas   |
| POST   | `/api/moedas`         | Adiciona uma moeda ao monitoramento |
| DELETE | `/api/moedas/{sigla}` | Remove uma moeda do monitoramento   |

### Sincronização

| Método | Endpoint           | Descrição                                     |
| ------ | ------------------ | --------------------------------------------- |
| POST   | `/api/sync`        | Força sincronização imediata com a AwesomeAPI |
| GET    | `/api/sync/status` | Status da última sincronização                |

### Admin

| Método | Endpoint                                           | Descrição                            |
| ------ | -------------------------------------------------- | ------------------------------------ |
| POST   | `/api/admin/historico/inicializar`                 | Importa histórico dos últimos 5 anos |
| POST   | `/api/admin/historico/inicializar?reimportar=true` | Apaga e reimporta todo o histórico   |

---

## Moedas suportadas

Qualquer moeda suportada pela [AwesomeAPI](https://economia.awesomeapi.com.br/json/available). Exemplos comuns:

| Sigla | Moeda           |
| ----- | --------------- |
| USD   | Dólar Americano |
| EUR   | Euro            |
| BTC   | Bitcoin         |
| GBP   | Libra Esterlina |
| ARS   | Peso Argentino  |
| JPY   | Iene Japonês    |

---

## Tecnologias utilizadas

- Java 21
- Spring Boot 4
- Spring Data JPA
- Spring Web
- PostgreSQL
- AwesomeAPI
