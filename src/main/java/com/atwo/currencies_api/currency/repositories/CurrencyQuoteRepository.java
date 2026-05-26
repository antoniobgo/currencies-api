package com.atwo.currencies_api.currency.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.atwo.currencies_api.currency.entities.CurrencyQuote;

@Repository
public interface CurrencyQuoteRepository extends JpaRepository<CurrencyQuote, Long> {

    Optional<CurrencyQuote> findTopByCodeOrderByQuotedAtDesc(String code);

    List<CurrencyQuote> findByCodeAndQuotedAtBetweenOrderByQuotedAtAsc(String code,
            LocalDateTime start, LocalDateTime end);
}
