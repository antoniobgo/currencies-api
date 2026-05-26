package com.atwo.currencies_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CurrenciesApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrenciesApiApplication.class, args);
    }

}
