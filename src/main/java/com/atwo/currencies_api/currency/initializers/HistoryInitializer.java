package com.atwo.currencies_api.currency.initializers;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.atwo.currencies_api.currency.services.QuoteService;

@Component
public class HistoryInitializer implements ApplicationRunner {

    private final QuoteService quoteService;

    public HistoryInitializer(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Override
    public void run(ApplicationArguments args) {
        quoteService.initializeHistoryIfNeeded();
    }
}
