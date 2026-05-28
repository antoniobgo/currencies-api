package com.atwo.currencies_api.currency.controllers;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.atwo.currencies_api.currency.dtos.CurrencyDailyCloseResponse;
import com.atwo.currencies_api.currency.dtos.CurrencyQuoteResponse;
import com.atwo.currencies_api.currency.dtos.SummaryDTO;
import com.atwo.currencies_api.currency.services.DailyCloseService;
import com.atwo.currencies_api.currency.services.QuoteService;

@RestController
@RequestMapping("/api/cotacoes")
public class CurrencyQuoteController {

    private final QuoteService quoteService;
    private final DailyCloseService dailyCloseService;

    public CurrencyQuoteController(QuoteService quoteService, DailyCloseService dailyCloseService) {
        this.quoteService = quoteService;
        this.dailyCloseService = dailyCloseService;
    }

    @GetMapping("/atual")
    public ResponseEntity<List<CurrencyQuoteResponse>> getLatestAll() {
        return ResponseEntity.ok(
                quoteService.findAllLatest().stream().map(CurrencyQuoteResponse::from).toList());
    }

    @GetMapping("/atual/{moeda}")
    public ResponseEntity<CurrencyQuoteResponse> getLatestByCode(@PathVariable String moeda) {
        return ResponseEntity
                .ok(CurrencyQuoteResponse.from(quoteService.findLatestByCode(moeda.toUpperCase())));
    }

    @GetMapping("/hoje")
    public ResponseEntity<List<CurrencyQuoteResponse>> getTodayAll() {
        return ResponseEntity
                .ok(quoteService.findTodayAll().stream().map(CurrencyQuoteResponse::from).toList());
    }

    @GetMapping("/hoje/{moeda}")
    public ResponseEntity<List<CurrencyQuoteResponse>> getTodayByCode(@PathVariable String moeda) {
        return ResponseEntity.ok(quoteService.findTodayByCode(moeda.toUpperCase()).stream()
                .map(CurrencyQuoteResponse::from).toList());
    }

    @GetMapping("/historico")
    public ResponseEntity<List<CurrencyDailyCloseResponse>> getHistory(@RequestParam String moeda,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim) {
        return ResponseEntity
                .ok(dailyCloseService.findHistory(moeda.toUpperCase(), dataInicio, dataFim).stream()
                        .map(CurrencyDailyCloseResponse::from).toList());
    }

    @GetMapping("/resumo")
    public ResponseEntity<SummaryDTO> getSummary(@RequestParam String moeda,
            @RequestParam(defaultValue = "30") int periodo) {
        return ResponseEntity.ok(dailyCloseService.findSummary(moeda.toUpperCase(), periodo));
    }
}
