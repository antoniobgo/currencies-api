package com.atwo.currencies_api.currency.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AwesomeApiQuoteDTO(String code, String codein, String name, String high, String low,
        String bid, String ask, String varBid, String pctChange, String timestamp,
        @JsonProperty("create_date") String createDate) {
}
