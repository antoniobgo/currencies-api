package com.atwo.currencies_api.currency.client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import com.atwo.currencies_api.currency.config.AwesomeApiProperties;
import com.atwo.currencies_api.currency.dtos.AwesomeApiQuoteDTO;

@Component
public class AwesomeApiClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;

    private static final Logger logger = LoggerFactory.getLogger(AwesomeApiClient.class);

    public AwesomeApiClient(RestClient.Builder builder, AwesomeApiProperties properties) {
        this.baseUrl = properties.baseUrl();
        this.apiKey = properties.apiKey();
        this.restClient = builder.baseUrl(this.baseUrl).defaultHeader("x-api-key", this.apiKey)
                .requestInterceptor((request, body, execution) -> {
                    logger.debug("→ {} {}", request.getMethod(), request.getURI());
                    logger.debug("Headers: {}", request.getHeaders());
                    return execution.execute(request, body);
                }).build();
    }

    // TODO: analisar error handling
    public Map<String, AwesomeApiQuoteDTO> fetchQuotes(List<String> codes) {
        String pairs = codes.stream().map(code -> code + "-BRL").collect(Collectors.joining(","));

        try {

            return restClient.get().uri("/last/{pairs}", pairs).retrieve()
                    .body(new ParameterizedTypeReference<Map<String, AwesomeApiQuoteDTO>>() {});
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Erro ao buscar cotações na AwesomeAPI: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return Map.of();
        } catch (ResourceAccessException e) {
            logger.error("AwesomeAPI indisponível: {}", e.getMessage());
            return Map.of();
        }
    }

    public List<AwesomeApiQuoteDTO> fetchHistoricalQuotes(String code, LocalDate start,
            LocalDate end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        logger.info("Fazendo chamada: start: {}, end: {}, code: {}", start, end, code);

        List<AwesomeApiQuoteDTO> quotes = restClient.get()
                .uri("/daily/{pair}/360?start_date={start}&end_date={end}", code + "-BRL",
                        start.format(fmt), end.format(fmt))
                .retrieve().body(new ParameterizedTypeReference<List<AwesomeApiQuoteDTO>>() {});


        return quotes.stream()
                .map(dto -> dto.code() != null ? dto
                        : new AwesomeApiQuoteDTO(quotes.get(0).code(), quotes.get(0).codein(),
                                quotes.get(0).name(), dto.high(), dto.low(), dto.bid(), dto.ask(),
                                dto.varBid(), dto.pctChange(), dto.timestamp()))
                .toList();
    }
}
