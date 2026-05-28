package com.atwo.currencies_api.currency.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SummaryDTO(String code, BigDecimal max, BigDecimal min, BigDecimal avg,
        double pctChange, LocalDate start, LocalDate end) {
}
