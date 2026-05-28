package com.atwo.currencies_api.currency.dtos;

import java.time.LocalDateTime;

public record SyncStatusDTO(LocalDateTime lastSyncAt, int lastSyncCount) {
}
