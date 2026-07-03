package com.atwo.currencies_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.atwo.currencies_api.currency.config.AwesomeApiProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AwesomeApiProperties.class)
public class CurrenciesApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrenciesApiApplication.class, args);
    }

}
