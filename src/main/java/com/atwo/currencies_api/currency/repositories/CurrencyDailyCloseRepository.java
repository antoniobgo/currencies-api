package com.atwo.currencies_api.currency.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.atwo.currencies_api.currency.entities.CurrencyDailyClose;

@Repository
public interface CurrencyDailyCloseRepository extends JpaRepository<CurrencyDailyClose, Long> {

    List<CurrencyDailyClose> findByCodeAndDateBetweenOrderByDateAsc(String code, LocalDate start,
            LocalDate end);

    Optional<CurrencyDailyClose> findTopByCodeOrderByDateAsc(String code);

    boolean existsByCodeAndDate(String code, LocalDate date);
}
