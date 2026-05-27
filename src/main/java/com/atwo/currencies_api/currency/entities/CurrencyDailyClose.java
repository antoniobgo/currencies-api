package com.atwo.currencies_api.currency.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "currency_daily_closes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"code", "date"}),
        indexes = @Index(name = "idx_daily_closes_code_date", columnList = "code, date"))
public class CurrencyDailyClose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 10)
    private String codeIn;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal bid;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal ask;

    @Column(nullable = false)
    private Double pctChange;

    @Column(nullable = false)
    private Double varBid;

    @Column(nullable = false)
    private LocalDate date;

    public CurrencyDailyClose() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeIn() {
        return codeIn;
    }

    public void setCodeIn(String codeIn) {
        this.codeIn = codeIn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getBid() {
        return bid;
    }

    public void setBid(BigDecimal bid) {
        this.bid = bid;
    }

    public BigDecimal getAsk() {
        return ask;
    }

    public void setAsk(BigDecimal ask) {
        this.ask = ask;
    }

    public Double getPctChange() {
        return pctChange;
    }

    public void setPctChange(Double pctChange) {
        this.pctChange = pctChange;
    }

    public Double getVarBid() {
        return varBid;
    }

    public void setVarBid(Double varBid) {
        this.varBid = varBid;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }


}
