package com.atwo.currencies_api.currency.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.atwo.currencies_api.currency.dtos.MonitoredCurrencyRequest;
import com.atwo.currencies_api.currency.dtos.MonitoredCurrencyResponse;
import com.atwo.currencies_api.currency.services.MonitoredCurrencyService;

@RestController
@RequestMapping("/api/moedas")
public class MonitoredCurrencyController {

    private final MonitoredCurrencyService monitoredCurrencyService;

    public MonitoredCurrencyController(MonitoredCurrencyService monitoredCurrencyService) {
        this.monitoredCurrencyService = monitoredCurrencyService;
    }

    @GetMapping
    public ResponseEntity<List<MonitoredCurrencyResponse>> getAll() {
        return ResponseEntity.ok(monitoredCurrencyService.findAll().stream()
                .map(MonitoredCurrencyResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<MonitoredCurrencyResponse> add(
            @RequestBody MonitoredCurrencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MonitoredCurrencyResponse.from(monitoredCurrencyService.add(request.code())));
    }

    @DeleteMapping("/{sigla}")
    public ResponseEntity<Void> remove(@PathVariable String sigla) {
        monitoredCurrencyService.remove(sigla.toUpperCase());
        return ResponseEntity.noContent().build();
    }
}
