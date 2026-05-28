package com.atwo.currencies_api.currency.dtos;

import java.time.LocalDateTime;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;

public record MonitoredCurrencyResponse(String code, boolean active, LocalDateTime createdAt) {
    public static MonitoredCurrencyResponse from(MonitoredCurrency entity) {
        return new MonitoredCurrencyResponse(entity.getCode(), entity.isActive(),
                entity.getCreatedAt());
    }
}
