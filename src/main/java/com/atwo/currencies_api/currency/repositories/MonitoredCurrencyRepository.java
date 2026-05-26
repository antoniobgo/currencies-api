package com.atwo.currencies_api.currency.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;

@Repository
public interface MonitoredCurrencyRepository extends JpaRepository<MonitoredCurrency, String> {
}
