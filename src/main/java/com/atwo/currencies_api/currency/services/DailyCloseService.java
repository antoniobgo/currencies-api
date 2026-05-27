package com.atwo.currencies_api.currency.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.atwo.currencies_api.currency.client.AwesomeApiClient;
import com.atwo.currencies_api.currency.dtos.AwesomeApiQuoteDTO;
import com.atwo.currencies_api.currency.entities.CurrencyDailyClose;
import com.atwo.currencies_api.currency.entities.CurrencyQuote;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;
import com.atwo.currencies_api.currency.repositories.CurrencyDailyCloseRepository;
import com.atwo.currencies_api.currency.repositories.MonitoredCurrencyRepository;

@Service
public class DailyCloseService {

    private final AwesomeApiClient awesomeApiClient;
    private final CurrencyDailyCloseRepository dailyCloseRepository;
    private final MonitoredCurrencyRepository monitoredCurrencyRepository;

    private static final Logger logger = LoggerFactory.getLogger(DailyCloseService.class);

    public DailyCloseService(AwesomeApiClient awesomeApiClient,
            CurrencyDailyCloseRepository dailyCloseRepository,
            MonitoredCurrencyRepository monitoredCurrencyRepository) {
        this.awesomeApiClient = awesomeApiClient;
        this.dailyCloseRepository = dailyCloseRepository;
        this.monitoredCurrencyRepository = monitoredCurrencyRepository;
    }

    public void initializeHistoryIfNeeded() {
        LocalDate oldest = dailyCloseRepository.findTopByOrderByDateAsc()
                .map(CurrencyDailyClose::getDate).orElse(null);
        LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);

        if (oldest != null && !oldest.isAfter(fiveYearsAgo))
            return;

        List<String> codes = monitoredCurrencyRepository.findAll().stream()
                .map(MonitoredCurrency::getCode).toList();

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(360);

        while (end.isAfter(fiveYearsAgo)) {
            if (start.isBefore(fiveYearsAgo))
                start = fiveYearsAgo;
            fetchAndSaveHistorical(codes, start, end);
            end = start.minusDays(1);
            start = end.minusDays(360);
        }
    }

    public void saveClose(CurrencyQuote quote) {
        if (dailyCloseRepository.existsByCodeAndDate(quote.getCode(),
                quote.getQuotedAt().toLocalDate()))
            return;

        CurrencyDailyClose close = toEntity(quote);
        dailyCloseRepository.save(close);
    }

    private void fetchAndSaveHistorical(List<String> codes, LocalDate start, LocalDate end) {
        for (String code : codes) {
            try {
                List<AwesomeApiQuoteDTO> quotes =
                        awesomeApiClient.fetchHistoricalQuotes(code, start, end);

                List<CurrencyDailyClose> entities =
                        quotes.stream().filter(this::isValidQuote).map(this::toEntity).toList();

                dailyCloseRepository.saveAll(entities);
                logger.info("Histórico salvo: moeda={}, inicio={}, fim={}, registros={}", code,
                        start, end, entities.size());

            } catch (Exception e) {
                logger.error("Erro ao buscar histórico: moeda={}, inicio={}, fim={}: {}", code,
                        start, end, e.getMessage());
            }
        }
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

    private CurrencyDailyClose toEntity(CurrencyQuote quote) {
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
        close.setDate(quote.getQuotedAt().toLocalDate());
        return close;
    }

    private boolean isValidQuote(AwesomeApiQuoteDTO dto) {
        return dto.bid() != null && dto.ask() != null && dto.high() != null && dto.low() != null
                && dto.varBid() != null && dto.pctChange() != null && dto.timestamp() != null;
    }
}
