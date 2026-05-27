package com.atwo.currencies_api.currency.initializers;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.atwo.currencies_api.currency.services.DailyCloseService;

@Component
public class HistoryInitializer implements ApplicationRunner {

    private final DailyCloseService dailyCloseService;

    public HistoryInitializer(DailyCloseService dailyCloseService) {
        this.dailyCloseService = dailyCloseService;
    }

    @Override
    public void run(ApplicationArguments args) {
        dailyCloseService.initializeHistoryIfNeeded();
    }
}
