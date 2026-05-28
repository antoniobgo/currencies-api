package com.atwo.currencies_api.currency.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.atwo.currencies_api.currency.dtos.SyncStatusDTO;
import com.atwo.currencies_api.currency.services.QuoteService;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final QuoteService quoteService;

    public SyncController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    public ResponseEntity<SyncStatusDTO> syncNow() {
        quoteService.sync();
        return ResponseEntity.ok(
                new SyncStatusDTO(quoteService.getLastSyncAt(), quoteService.getLastSyncCount()));
    }

    @GetMapping("/status")
    public ResponseEntity<SyncStatusDTO> getStatus() {
        return ResponseEntity.ok(
                new SyncStatusDTO(quoteService.getLastSyncAt(), quoteService.getLastSyncCount()));
    }
}
