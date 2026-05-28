package com.atwo.currencies_api.currency.initializers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;
import com.atwo.currencies_api.currency.repositories.CurrencyQuoteRepository;
import com.atwo.currencies_api.currency.repositories.MonitoredCurrencyRepository;
import com.atwo.currencies_api.currency.services.DailyCloseService;

@Component
public class DailyCloseInitializer implements ApplicationRunner {

    private final DailyCloseService dailyCloseService;
    private final CurrencyQuoteRepository quoteRepository;
    private final MonitoredCurrencyRepository monitoredCurrencyRepository;

    private static final Logger logger = LoggerFactory.getLogger(DailyCloseInitializer.class);

    public DailyCloseInitializer(DailyCloseService dailyCloseService,
            CurrencyQuoteRepository quoteRepository,
            MonitoredCurrencyRepository monitoredCurrencyRepository) {
        this.dailyCloseService = dailyCloseService;
        this.quoteRepository = quoteRepository;
        this.monitoredCurrencyRepository = monitoredCurrencyRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Reconciliando fechamentos recentes...");

        monitoredCurrencyRepository.findAll().stream().map(MonitoredCurrency::getCode)
                .forEach(code -> quoteRepository.findTopByCodeOrderByQuotedAtDesc(code)
                        .ifPresent(dailyCloseService::saveClose));

        logger.info("Reconciliação concluída.");
    }
}
