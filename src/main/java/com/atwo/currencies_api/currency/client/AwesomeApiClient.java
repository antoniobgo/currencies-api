package com.atwo.currencies_api.currency.client;

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
import com.atwo.currencies_api.currency.dtos.AwesomeApiQuoteDTO;

@Component
public class AwesomeApiClient {

    private final RestClient restClient;

    private static final Logger logger = LoggerFactory.getLogger(AwesomeApiClient.class);

    public AwesomeApiClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://economia.awesomeapi.com.br/json/last").build();
    }

    // TODO: analisar error handling
    public Map<String, AwesomeApiQuoteDTO> fetchQuotes(List<String> codes) {
        String pairs = codes.stream().map(code -> code + "-BRL").collect(Collectors.joining(","));

        try {
            return restClient.get().uri("/{pairs}", pairs).retrieve()
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
}
