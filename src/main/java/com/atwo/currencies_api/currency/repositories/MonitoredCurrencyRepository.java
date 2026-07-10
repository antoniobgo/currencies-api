package com.atwo.currencies_api.currency.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;

public interface MonitoredCurrencyRepository extends JpaRepository<MonitoredCurrency, String> {
}
