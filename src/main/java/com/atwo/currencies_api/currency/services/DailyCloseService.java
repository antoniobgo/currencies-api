package com.atwo.currencies_api.currency.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.atwo.currencies_api.currency.client.AwesomeApiClient;
import com.atwo.currencies_api.currency.dtos.AwesomeApiQuoteDTO;
import com.atwo.currencies_api.currency.dtos.SummaryDTO;
import com.atwo.currencies_api.currency.entities.CurrencyDailyClose;
import com.atwo.currencies_api.currency.entities.CurrencyQuote;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;
import com.atwo.currencies_api.currency.repositories.CurrencyDailyCloseRepository;
import com.atwo.currencies_api.currency.repositories.MonitoredCurrencyRepository;

@Service
public class DailyCloseService {

    private final CurrencyDailyCloseRepository dailyCloseRepository;
    private final MonitoredCurrencyRepository monitoredCurrencyRepository;
    private final AwesomeApiClient awesomeApiClient;

    private static final Logger logger = LoggerFactory.getLogger(DailyCloseService.class);

    public DailyCloseService(CurrencyDailyCloseRepository dailyCloseRepository,
            MonitoredCurrencyRepository monitoredCurrencyRepository,
            AwesomeApiClient awesomeApiClient) {
        this.dailyCloseRepository = dailyCloseRepository;
        this.monitoredCurrencyRepository = monitoredCurrencyRepository;
        this.awesomeApiClient = awesomeApiClient;
    }

    public void importFullHistory() {
        List<String> codes = monitoredCurrencyRepository.findAll().stream()
                .map(MonitoredCurrency::getCode).toList();

        LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);
        LocalDate today = LocalDate.now();

        for (String code : codes) {
            LocalDate end = today;
            LocalDate start = end.minusDays(360);

            while (end.isAfter(fiveYearsAgo)) {
                if (start.isBefore(fiveYearsAgo))
                    start = fiveYearsAgo;
                fetchAndSaveHistorical(code, start, end);
                end = start.minusDays(1);
                start = end.minusDays(360);
            }

            logger.info("Histórico completo importado: code={}", code);
        }
    }

    private void fetchAndSaveHistorical(String code, LocalDate start, LocalDate end) {

        try {
            List<AwesomeApiQuoteDTO> quotes =
                    awesomeApiClient.fetchHistoricalQuotes(code, start, end);

            Set<LocalDate> existing =
                    dailyCloseRepository.findByCodeAndDateBetweenOrderByDateAsc(code, start, end)
                            .stream().map(CurrencyDailyClose::getDate).collect(Collectors.toSet());

            List<CurrencyDailyClose> toSave = quotes.stream().filter(this::isValidQuote)
                    .map(this::toEntity).filter(e -> !existing.contains(e.getDate()))
                    .collect(Collectors.toMap(CurrencyDailyClose::getDate, e -> e, (a, b) -> a))
                    .values().stream().toList();

            if (!toSave.isEmpty())
                dailyCloseRepository.saveAll(toSave);

            logger.info("Histórico salvo: moeda={}, inicio={}, fim={}, registros={}", code, start,
                    end, toSave.size());

        } catch (Exception e) {
            logger.error("Erro ao buscar histórico: moeda={}, inicio={}, fim={}: {}", code, start,
                    end, e.getMessage());
        }

    }

    public List<CurrencyDailyClose> findHistory(String code, LocalDate start, LocalDate end) {
        LocalDate effectiveStart = start != null ? start : LocalDate.now().minusYears(1);
        LocalDate effectiveEnd = end != null ? end : LocalDate.now();
        return dailyCloseRepository.findByCodeAndDateBetweenOrderByDateAsc(code, effectiveStart,
                effectiveEnd);
    }

    public SummaryDTO findSummary(String code, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        List<CurrencyDailyClose> closes =
                dailyCloseRepository.findByCodeAndDateBetweenOrderByDateAsc(code, start, end);

        if (closes.isEmpty())
            throw new NoSuchElementException("Sem dados para: " + code);

        BigDecimal max = closes.stream().map(CurrencyDailyClose::getHigh)
                .max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal min = closes.stream().map(CurrencyDailyClose::getLow)
                .min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal avg = closes.stream().map(CurrencyDailyClose::getBid)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(closes.size()), 6, RoundingMode.HALF_UP);

        CurrencyDailyClose first = closes.get(0);
        CurrencyDailyClose last = closes.get(closes.size() - 1);
        double pctChange = ((last.getBid().doubleValue() - first.getBid().doubleValue())
                / first.getBid().doubleValue()) * 100;

        return new SummaryDTO(code, max, min, avg, pctChange, start, end);
    }

    public void saveClose(CurrencyQuote quote) {
        LocalDate date = quote.getQuotedAt().toLocalDate();

        if (dailyCloseRepository.existsByCodeAndDate(quote.getCode(), date))
            return;

        CurrencyDailyClose close = new CurrencyDailyClose();
        close.setCode(quote.getCode());
        close.setCodeIn(quote.getCodeIn());
        close.setName(quote.getName());
        close.setHigh(quote.getHigh());
        close.setLow(quote.getLow());
        close.setBid(quote.getBid());
        close.setAsk(quote.getAsk());
        close.setVarBid(quote.getVarBid());
        close.setPctChange(quote.getPctChange());
        close.setDate(date);

        dailyCloseRepository.save(close);
        logger.info("Fechamento salvo: code={}, date={}, bid={}", quote.getCode(), date,
                quote.getBid());
    }

    private boolean isValidQuote(AwesomeApiQuoteDTO dto) {
        return dto.bid() != null && dto.ask() != null && dto.high() != null && dto.low() != null
                && dto.varBid() != null && dto.pctChange() != null && dto.timestamp() != null;
    }

    private CurrencyDailyClose toEntity(AwesomeApiQuoteDTO dto) {
        CurrencyDailyClose close = new CurrencyDailyClose();
        close.setCode(dto.code());
        close.setCodeIn(dto.codein());
        close.setName(dto.name());
        close.setHigh(new BigDecimal(dto.high()));
        close.setLow(new BigDecimal(dto.low()));
        close.setBid(new BigDecimal(dto.bid()));
        close.setAsk(new BigDecimal(dto.ask()));
        close.setVarBid(Double.parseDouble(dto.varBid()));
        close.setPctChange(Double.parseDouble(dto.pctChange()));
        close.setDate(Instant.ofEpochSecond(Long.parseLong(dto.timestamp()))
                .atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate());
        return close;
    }


}
