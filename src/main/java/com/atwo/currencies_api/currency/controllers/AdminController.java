package com.atwo.currencies_api.currency.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.atwo.currencies_api.currency.services.DailyCloseService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    // Comentario para testar pipeline de deploy automatico
    private final DailyCloseService dailyCloseService;

    public AdminController(DailyCloseService dailyCloseService) {
        this.dailyCloseService = dailyCloseService;
    }

    @PostMapping("/historico/inicializar")
    public ResponseEntity<Void> importFullHistory() {
        dailyCloseService.importFullHistory();
        return ResponseEntity.ok().build();
    }
}
