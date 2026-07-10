package com.atwo.currencies_api.currency.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.atwo.currencies_api.currency.client.AwesomeApiClient;
import com.atwo.currencies_api.currency.dtos.AwesomeApiQuoteDTO;
import com.atwo.currencies_api.currency.entities.CurrencyQuote;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;
import com.atwo.currencies_api.currency.repositories.CurrencyQuoteRepository;
import com.atwo.currencies_api.currency.repositories.MonitoredCurrencyRepository;
import jakarta.transaction.Transactional;

@Service
public class QuoteService {

    private final AwesomeApiClient awesomeApiClient;
    private final CurrencyQuoteRepository quoteRepository;
    private final MonitoredCurrencyRepository monitoredCurrencyRepository;
    private final DailyCloseService dailyCloseService;

    private LocalDateTime lastSyncAt;
    private int lastSyncCount;

    private static final Logger logger = LoggerFactory.getLogger(AwesomeApiClient.class);

    public QuoteService(AwesomeApiClient awesomeApiClient, CurrencyQuoteRepository quoteRepository,
            MonitoredCurrencyRepository monitoredCurrencyRepository,
            DailyCloseService dailyCloseService) {
        this.awesomeApiClient = awesomeApiClient;
        this.quoteRepository = quoteRepository;
        this.monitoredCurrencyRepository = monitoredCurrencyRepository;
        this.dailyCloseService = dailyCloseService;
    }

    @Scheduled(cron = "0 0/5 9-18 * * MON-FRI", zone = "America/Sao_Paulo")
    @Transactional
    public void sync() {
        logger.info("Iniciando scheduled task");
        List<String> codes = monitoredCurrencyRepository.findAll().stream()
                .map(MonitoredCurrency::getCode).toList();

        if (codes.isEmpty())
            return;

        LocalTime now = LocalTime.now(ZoneId.of("America/Sao_Paulo"));
        boolean isOpeningCycle = now.getHour() == 9 && now.getMinute() < 5;
        boolean isClosingCycle = now.getHour() == 18;

        if (isOpeningCycle) {
            logger.info("iniciando rotina de inicio de ciclo diario");
            LocalDate yesterday = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1);
            LocalDateTime start = yesterday.atStartOfDay();
            LocalDateTime end = yesterday.atTime(LocalTime.MAX);

            // salva fechamento antes de deletar
            codes.forEach(code -> quoteRepository.findTopByCodeOrderByQuotedAtDesc(code)
                    .ifPresent(dailyCloseService::saveClose));

            // TODO: pensar se faz sentido deletar sempre os registros de intraday
            // e se faz, se há necessidade de setar o between visto que teoricamente poderia deletar
            // tudo no opening cycle
            logger.info("deletando registros de intraday do dia anterior");
            quoteRepository.deleteByQuotedAtBetween(start, end);

        }
        if (isClosingCycle) {
            // TODO: retornar algo dizendo que o dia está fechado (faz sentido?)
        } else {
            Map<String, AwesomeApiQuoteDTO> quotes = awesomeApiClient.fetchQuotes(codes);

            List<CurrencyQuote> entities = quotes.values().stream().map(this::toEntity).toList();

            quoteRepository.saveAll(entities);

            lastSyncAt = LocalDateTime.now();
            lastSyncCount = entities.size();
        }


    }

    public List<CurrencyQuote> findAllLatest() {
        List<String> codes = monitoredCurrencyRepository.findAll().stream()
                .map(MonitoredCurrency::getCode).toList();

        return codes.stream().map(quoteRepository::findTopByCodeOrderByQuotedAtDesc)
                .filter(Optional::isPresent).map(Optional::get).toList();
    }

    public CurrencyQuote findLatestByCode(String code) {
        return quoteRepository.findTopByCodeOrderByQuotedAtDesc(code)
                .orElseThrow(() -> new NoSuchElementException("Cotação não encontrada: " + code));
    }

    public List<CurrencyQuote> findTodayAll() {
        List<String> codes = monitoredCurrencyRepository.findAll().stream()
                .map(MonitoredCurrency::getCode).toList();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        return codes.stream()
                .flatMap(code -> quoteRepository
                        .findByCodeAndQuotedAtBetweenOrderByQuotedAtAsc(code, startOfDay, now)
                        .stream())
                .toList();
    }

    public List<CurrencyQuote> findTodayByCode(String code) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        return quoteRepository.findByCodeAndQuotedAtBetweenOrderByQuotedAtAsc(code, startOfDay,
                now);
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public int getLastSyncCount() {
        return lastSyncCount;
    }

    private CurrencyQuote toEntity(AwesomeApiQuoteDTO dto) {
        CurrencyQuote quote = new CurrencyQuote();
        quote.setCode(dto.code());
        quote.setCodeIn(dto.codein());
        quote.setName(dto.name());
        quote.setHigh(new BigDecimal(dto.high()));
        quote.setLow(new BigDecimal(dto.low()));
        quote.setBid(new BigDecimal(dto.bid()));
        quote.setAsk(new BigDecimal(dto.ask()));
        quote.setVarBid(Double.parseDouble(dto.varBid()));
        quote.setPctChange(Double.parseDouble(dto.pctChange()));
        quote.setQuotedAt(parseTimestamp(dto.timestamp()));
        return quote;
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        return Instant.ofEpochSecond(Long.parseLong(timestamp))
                .atZone(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
    }
}
