package com.atwo.currencies_api.currency.services;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import com.atwo.currencies_api.currency.client.AwesomeApiClient;
import com.atwo.currencies_api.currency.dtos.AwesomeApiQuoteDTO;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;
import com.atwo.currencies_api.currency.repositories.MonitoredCurrencyRepository;

@Service
public class MonitoredCurrencyService {

    private final MonitoredCurrencyRepository repository;
    private final AwesomeApiClient awesomeApiClient;

    // private static final Logger logger = LoggerFactory.getLogger(MonitoredCurrencyService.class);

    public MonitoredCurrencyService(MonitoredCurrencyRepository repository,
            AwesomeApiClient awesomeApiClient) {
        this.repository = repository;
        this.awesomeApiClient = awesomeApiClient;
    }

    public List<MonitoredCurrency> findAll() {
        return repository.findAll();
    }

    public MonitoredCurrency add(String code) {
        if (repository.existsById(code))
            throw new IllegalStateException("Moeda já monitorada: " + code);

        // valida se a moeda existe na AwesomeAPI
        Map<String, AwesomeApiQuoteDTO> result = awesomeApiClient.fetchQuotes(List.of(code));
        if (result.isEmpty())
            throw new IllegalArgumentException("Moeda não suportada pela AwesomeAPI: " + code);

        MonitoredCurrency currency = new MonitoredCurrency();
        currency.setCode(code.toUpperCase());
        return repository.save(currency);
    }

    public void remove(String code) {
        if (!repository.existsById(code))
            throw new NoSuchElementException("Moeda não encontrada: " + code);
        repository.deleteById(code);
    }
}
