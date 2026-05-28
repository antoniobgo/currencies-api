package com.atwo.currencies_api.currency.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.atwo.currencies_api.currency.entities.CurrencyQuote;

public record CurrencyQuoteResponse(String code, String codeIn, String name, BigDecimal high,
        BigDecimal low, BigDecimal bid, BigDecimal ask, Double varBid, Double pctChange,
        LocalDateTime quotedAt) {
    public static CurrencyQuoteResponse from(CurrencyQuote entity) {
        return new CurrencyQuoteResponse(entity.getCode(), entity.getCodeIn(), entity.getName(),
                entity.getHigh(), entity.getLow(), entity.getBid(), entity.getAsk(),
                entity.getVarBid(), entity.getPctChange(), entity.getQuotedAt());
    }
}
