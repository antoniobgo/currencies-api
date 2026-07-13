package com.atwo.currencies_api.currency.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.atwo.currencies_api.currency.client.AwesomeApiClient;
import com.atwo.currencies_api.currency.dtos.AwesomeApiQuoteDTO;
import com.atwo.currencies_api.currency.dtos.SummaryDTO;
import com.atwo.currencies_api.currency.entities.CurrencyDailyClose;
import com.atwo.currencies_api.currency.entities.CurrencyQuote;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;
import com.atwo.currencies_api.currency.repositories.CurrencyDailyCloseRepository;
import com.atwo.currencies_api.currency.repositories.MonitoredCurrencyRepository;

@ExtendWith(MockitoExtension.class)
class DailyCloseServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Mock
    private CurrencyDailyCloseRepository dailyCloseRepository;

    @Mock
    private MonitoredCurrencyRepository monitoredCurrencyRepository;

    @Mock
    private AwesomeApiClient awesomeApiClient;

    private DailyCloseService service;

    @BeforeEach
    void setUp() {
        service = new DailyCloseService(dailyCloseRepository, monitoredCurrencyRepository,
                awesomeApiClient);
    }

    @Test
    void saveClose_fechamentoJaExistente_naoDeveSalvarNovamente() {
        // given
        CurrencyQuote quote = quote("USD", LocalDate.of(2026, 7, 9));
        when(dailyCloseRepository.existsByCodeAndDate("USD", LocalDate.of(2026, 7, 9)))
                .thenReturn(true);

        // when
        service.saveClose(quote);

        // then
        verify(dailyCloseRepository, never()).save(any());
    }

    @Test
    void saveClose_fechamentoNovo_devePersistirComDadosDaQuote() {
        // given
        CurrencyQuote quote = quote("USD", LocalDate.of(2026, 7, 9));
        when(dailyCloseRepository.existsByCodeAndDate("USD", LocalDate.of(2026, 7, 9)))
                .thenReturn(false);

        // when
        service.saveClose(quote);

        // then
        ArgumentCaptor<CurrencyDailyClose> captor = ArgumentCaptor.forClass(CurrencyDailyClose.class);
        verify(dailyCloseRepository).save(captor.capture());
        CurrencyDailyClose saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("USD");
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 7, 9));
        assertThat(saved.getBid()).isEqualByComparingTo(quote.getBid());
        assertThat(saved.getHigh()).isEqualByComparingTo(quote.getHigh());
        assertThat(saved.getLow()).isEqualByComparingTo(quote.getLow());
        assertThat(saved.getAsk()).isEqualByComparingTo(quote.getAsk());
    }

    @Test
    void findHistory_semDatasInformadas_deveUsarUltimoAnoComoPadrao() {
        // given
        when(dailyCloseRepository.findByCodeAndDateBetweenOrderByDateAsc(eq("USD"), any(), any()))
                .thenReturn(List.of());

        // when
        service.findHistory("USD", null, null);

        // then
        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dailyCloseRepository).findByCodeAndDateBetweenOrderByDateAsc(eq("USD"),
                startCaptor.capture(), endCaptor.capture());
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.now());
        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.now().minusYears(1));
    }

    @Test
    void findHistory_comDatasInformadas_deveUsarDatasFornecidas() {
        // given
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 6, 1);
        when(dailyCloseRepository.findByCodeAndDateBetweenOrderByDateAsc("USD", start, end))
                .thenReturn(List.of());

        // when
        service.findHistory("USD", start, end);

        // then
        verify(dailyCloseRepository).findByCodeAndDateBetweenOrderByDateAsc("USD", start, end);
    }

    @Test
    void findSummary_semFechamentos_deveLancarNoSuchElementException() {
        // given
        when(dailyCloseRepository.findByCodeAndDateBetweenOrderByDateAsc(eq("USD"), any(), any()))
                .thenReturn(List.of());

        // when / then
        assertThatThrownBy(() -> service.findSummary("USD", 30))
                .isInstanceOf(NoSuchElementException.class).hasMessageContaining("USD");
    }

    @Test
    void findSummary_comFechamentos_deveCalcularMaxMinAvgEPctChange() {
        // given
        CurrencyDailyClose first = dailyClose("5.00", "5.40", "5.20", LocalDate.of(2026, 6, 1));
        CurrencyDailyClose last = dailyClose("5.10", "5.60", "5.55", LocalDate.of(2026, 6, 30));
        when(dailyCloseRepository.findByCodeAndDateBetweenOrderByDateAsc(eq("USD"), any(), any()))
                .thenReturn(List.of(first, last));

        // when
        SummaryDTO summary = service.findSummary("USD", 30);

        // then
        assertThat(summary.code()).isEqualTo("USD");
        assertThat(summary.max()).isEqualByComparingTo("5.60");
        assertThat(summary.min()).isEqualByComparingTo("5.20");
        assertThat(summary.avg()).isEqualByComparingTo(new BigDecimal("5.05"));
        assertThat(summary.pctChange()).isCloseTo(2.0, within(0.001));
    }

    @Test
    void importFullHistory_semMoedasMonitoradas_naoDeveChamarAwesomeApi() {
        // given
        when(monitoredCurrencyRepository.findAll()).thenReturn(List.of());

        // when
        service.importFullHistory();

        // then
        verify(awesomeApiClient, never()).fetchHistoricalQuotes(any(), any(), any());
        verify(dailyCloseRepository, never()).saveAll(any());
    }

    @Test
    void importFullHistory_deveFiltrarQuotesInvalidasEDeduplicarPorDataAntesDeSalvar() {
        // given: AwesomeAPI retorna uma data duplicada, uma quote inválida e uma data já existente
        LocalDate today = LocalDate.now();
        LocalDate novaData = today.minusDays(5);
        LocalDate dataJaExistente = today.minusDays(3);
        LocalDate outraDataNova = today.minusDays(1);

        MonitoredCurrency usd = new MonitoredCurrency();
        usd.setCode("USD");
        when(monitoredCurrencyRepository.findAll()).thenReturn(List.of(usd));

        AwesomeApiQuoteDTO quoteDuplicadaOriginal = historicalDto("5.40", novaData);
        AwesomeApiQuoteDTO quoteDuplicadaRepetida = historicalDto("9.99", novaData);
        AwesomeApiQuoteDTO quoteJaExistente = historicalDto("5.30", dataJaExistente);
        AwesomeApiQuoteDTO quoteInvalida = new AwesomeApiQuoteDTO("USD", "BRL", "Dólar", "5.5", "5.3",
                null, "5.36", "0.01", "0.1", String.valueOf(epochSeconds(outraDataNova)));
        AwesomeApiQuoteDTO quoteNova = historicalDto("5.45", outraDataNova);

        when(awesomeApiClient.fetchHistoricalQuotes(eq("USD"), any(), any())).thenReturn(List.of(
                quoteDuplicadaOriginal, quoteDuplicadaRepetida, quoteJaExistente, quoteInvalida,
                quoteNova));

        CurrencyDailyClose existente = dailyClose("5.00", "5.00", "5.00", dataJaExistente);
        when(dailyCloseRepository.findByCodeAndDateBetweenOrderByDateAsc(eq("USD"), any(), any()))
                .thenReturn(List.of(existente));

        // when
        service.importFullHistory();

        // then: só a data nova (deduplicada) e a outra data nova são salvas
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CurrencyDailyClose>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyCloseRepository, times(1)).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(CurrencyDailyClose::getDate)
                .containsExactlyInAnyOrder(novaData, outraDataNova);
    }

    private CurrencyQuote quote(String code, LocalDate quotedAt) {
        CurrencyQuote quote = new CurrencyQuote();
        quote.setCode(code);
        quote.setCodeIn("BRL");
        quote.setName("Dólar Americano/Real Brasileiro");
        quote.setHigh(new BigDecimal("5.50"));
        quote.setLow(new BigDecimal("5.30"));
        quote.setBid(new BigDecimal("5.40"));
        quote.setAsk(new BigDecimal("5.41"));
        quote.setVarBid(0.01);
        quote.setPctChange(0.2);
        quote.setQuotedAt(quotedAt.atTime(18, 0));
        return quote;
    }

    private CurrencyDailyClose dailyClose(String bid, String high, String low, LocalDate date) {
        CurrencyDailyClose close = new CurrencyDailyClose();
        close.setCode("USD");
        close.setCodeIn("BRL");
        close.setName("Dólar Americano/Real Brasileiro");
        close.setBid(new BigDecimal(bid));
        close.setHigh(new BigDecimal(high));
        close.setLow(new BigDecimal(low));
        close.setAsk(new BigDecimal(bid));
        close.setVarBid(0.0);
        close.setPctChange(0.0);
        close.setDate(date);
        return close;
    }

    private AwesomeApiQuoteDTO historicalDto(String bid, LocalDate date) {
        return new AwesomeApiQuoteDTO("USD", "BRL", "Dólar", "5.5", "5.3", bid, "5.36", "0.01",
                "0.1", String.valueOf(epochSeconds(date)));
    }

    private long epochSeconds(LocalDate date) {
        return date.atTime(12, 0).atZone(SP).toEpochSecond();
    }
}
