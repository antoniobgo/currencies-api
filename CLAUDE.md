# Regras de negócio não-óbvias

## Separação de responsabilidades
- `/api/cotacoes/atual` e `/api/cotacoes/hoje` nunca chamam a AwesomeAPI — leem exclusivamente o banco.
- `/api/cotacoes/historico` serve apenas `currency_daily_closes` (fechamentos diários).
- `/api/cotacoes/hoje` serve apenas `currency_quotes` (intraday do dia atual).
- A chamada à AwesomeAPI é responsabilidade exclusiva do `@Scheduled`. `POST /api/sync` reutiliza o mesmo método do scheduler — sem duplicar lógica.

## Comportamento dos endpoints
- `DELETE /api/moedas/{sigla}` remove a moeda do monitoramento mas preserva o histórico de cotações já salvo.
- `POST /api/moedas` deve validar se a sigla é suportada pela AwesomeAPI antes de persistir.

## Deduplicação
- A AwesomeAPI repete o fechamento do último dia útil para fins de semana e feriados. A deduplicação é feita por data antes de qualquer `saveAll`.
- A importação de histórico é sempre incremental por padrão — datas já existentes são ignoradas.

## DailyCloseInitializer
- Executa a cada inicialização da aplicação como reconciliação leve: se o servidor caiu durante o pregão, persiste o fechamento que ficou pendente em `currency_quotes`.
- Para gaps maiores (servidor fora por vários dias), usar `POST /api/admin/historico/inicializar`.

## Ciclo do scheduler às 9h
- No primeiro ciclo do dia: salva o fechamento do dia anterior em `currency_daily_closes` **e** limpa os registros intraday anteriores de `currency_quotes`.
