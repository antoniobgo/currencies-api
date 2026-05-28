package com.atwo.currencies_api.currency.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.atwo.currencies_api.currency.entities.CurrencyDailyClose;

public record CurrencyDailyCloseResponse(String code, String codeIn, String name, BigDecimal high,
        BigDecimal low, BigDecimal bid, BigDecimal ask, Double varBid, Double pctChange,
        LocalDate date) {
    public static CurrencyDailyCloseResponse from(CurrencyDailyClose entity) {
        return new CurrencyDailyCloseResponse(entity.getCode(), entity.getCodeIn(),
                entity.getName(), entity.getHigh(), entity.getLow(), entity.getBid(),
                entity.getAsk(), entity.getVarBid(), entity.getPctChange(), entity.getDate());
    }
}
