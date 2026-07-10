# Regras para testes unitários

## Pirâmide de testes
- Maioria: testes de unidade puros com `MockitoExtension`, sem subir contexto Spring. Rápidos, cobrem regras de negócio isoladas em services.
- `@WebMvcTest` para controllers — sobe só a camada web, mocka a camada de service com `@MockitoBean`, valida serialização JSON, status HTTP e validação de request/DTO.
- `@DataJpaTest` para repositories — só quando a query tem lógica não trivial (`@Query`, múltiplos parâmetros, `BETWEEN`, `deleteBy...`). Não escrever teste de repository para métodos `findById`/`save` puros do Spring Data.
- `@SpringBootTest` + Testcontainers (Postgres real, não H2) — poucos testes, só para os fluxos críticos ponta a ponta (ex: scheduler → sync → persistência). Lentos, usar com moderação.

## O que testar
- Priorizar as regras descritas em `CLAUDE.md`, pois são as que geram bugs silenciosos:
  - Dedup por data antes de `saveAll`.
  - Ciclo de abertura (9h): salvar fechamento em `currency_daily_closes` **e** limpar intraday de `currency_quotes` do dia anterior.
  - `/api/cotacoes/atual` e `/api/cotacoes/hoje` nunca chamando a AwesomeAPI.
  - `DELETE /api/moedas/{sigla}` preservando histórico de cotações.
  - `POST /api/moedas` validando sigla suportada pela AwesomeAPI antes de persistir.
  - `POST /api/sync` reutilizando o mesmo método do scheduler (sem lógica duplicada).
- Testar comportamento (o que o método retorna/persiste/lança), nunca detalhes de implementação interna.
- Cobertura não é a meta — a meta é cobrir regras de negócio e casos de borda. Não escrever teste para getters/setters ou para código gerado pelo framework.

## Mocking
- Mockar apenas dependências externas reais da classe sob teste (repositories, clients HTTP, outros services injetados).
- Nunca mockar a própria classe sob teste.
- `AwesomeApiClient` é sempre mockado nos testes de `QuoteService`/`DailyCloseService` — nunca bater na API real em teste de unidade.
- Em `@WebMvcTest`/`@SpringBootTest`, usar `@MockitoBean` (não `@MockBean`, deprecated a partir do Spring Boot 3.4 / removido em versões mais novas).

## Estilo
- Nomenclatura: `metodo_cenario_resultadoEsperado` (ex: `sync_cicloAbertura_deveLimparIntradayDoDiaAnterior`).
- Estruturar o corpo do teste com comentários `// given`, `// when`, `// then`.
- Assertions com AssertJ (`assertThat`), não `assertEquals`/`assertTrue` puros do JUnit.
- Um cenário de negócio por teste — evitar testes "guarda-chuva" que verificam múltiplos comportamentos não relacionados.
- Dados de teste construídos via builder/factory de teste quando o objeto tiver muitos campos (ex: `CurrencyQuote`, `AwesomeApiQuoteDTO`), para não repetir construção verbosa em cada teste.

## Localização e estrutura
- Testes espelham o pacote da classe sob teste em `src/test/java/...` (ex: `QuoteServiceTest` em `currency/services/`).
- Um arquivo de teste por classe de produção, nomeado `NomeDaClasseTest`.
