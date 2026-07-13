package com.atwo.currencies_api.currency.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.atwo.currencies_api.currency.client.AwesomeApiClient;
import com.atwo.currencies_api.currency.dtos.AwesomeApiQuoteDTO;
import com.atwo.currencies_api.currency.entities.CurrencyQuote;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;
import com.atwo.currencies_api.currency.repositories.CurrencyQuoteRepository;
import com.atwo.currencies_api.currency.repositories.MonitoredCurrencyRepository;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Mock
    private AwesomeApiClient awesomeApiClient;

    @Mock
    private CurrencyQuoteRepository quoteRepository;

    @Mock
    private MonitoredCurrencyRepository monitoredCurrencyRepository;

    @Mock
    private DailyCloseService dailyCloseService;

    private QuoteService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(LocalDateTime.of(2026, 7, 10, 14, 0));
    }

    @Test
    void sync_semMoedasMonitoradas_naoDeveChamarAwesomeApi() {
        // given
        when(monitoredCurrencyRepository.findAll()).thenReturn(List.of());

        // when
        service.sync();

        // then
        verify(awesomeApiClient, never()).fetchQuotes(any());
        verify(quoteRepository, never()).saveAll(any());
    }

    @Test
    void sync_forDaJanelaDeAberturaEFechamento_deveBuscarESalvarCotacoesIntraday() {
        // given
        stubMonitoredCurrencies("USD");
        when(awesomeApiClient.fetchQuotes(List.of("USD")))
                .thenReturn(Map.of("USDBRL", dto("USD", "5.40")));
        QuoteService service = serviceAt(LocalDateTime.of(2026, 7, 10, 14, 0));

        // when
        service.sync();

        // then
        ArgumentCaptor<List<CurrencyQuote>> captor = captorForQuoteList();
        verify(quoteRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(CurrencyQuote::getCode).containsExactly("USD");
        assertThat(service.getLastSyncAt()).isNotNull();
        assertThat(service.getLastSyncCount()).isEqualTo(1);
        verify(quoteRepository, never()).deleteByQuotedAtBetween(any(), any());
        verify(dailyCloseService, never()).saveClose(any());
    }

    @Test
    void sync_cicloDeAberturaAs9h_deveSalvarFechamentoAnteriorELimparIntradayDoDiaAnterior() {
        // given
        stubMonitoredCurrencies("USD");
        CurrencyQuote ultimaCotacaoDeOntem = new CurrencyQuote();
        ultimaCotacaoDeOntem.setCode("USD");
        when(quoteRepository.findTopByCodeOrderByQuotedAtDesc("USD"))
                .thenReturn(Optional.of(ultimaCotacaoDeOntem));
        when(awesomeApiClient.fetchQuotes(List.of("USD")))
                .thenReturn(Map.of("USDBRL", dto("USD", "5.40")));

        LocalDate hoje = LocalDate.of(2026, 7, 10);
        QuoteService service = serviceAt(hoje.atTime(9, 2));

        // when
        service.sync();

        // then: fechamento do dia anterior salvo e intraday antigo limpo
        verify(dailyCloseService).saveClose(ultimaCotacaoDeOntem);

        LocalDate ontem = hoje.minusDays(1);
        verify(quoteRepository).deleteByQuotedAtBetween(ontem.atStartOfDay(),
                ontem.atTime(LocalTime.MAX));

        // e, por estar dentro da janela do cron, também busca as cotações do novo ciclo
        verify(awesomeApiClient).fetchQuotes(List.of("USD"));
    }

    @Test
    void sync_cicloDeFechamentoAs18h_naoDeveBuscarNovasCotacoes() {
        // given
        stubMonitoredCurrencies("USD");
        QuoteService service = serviceAt(LocalDateTime.of(2026, 7, 10, 18, 0));

        // when
        service.sync();

        // then
        verify(awesomeApiClient, never()).fetchQuotes(any());
        verify(quoteRepository, never()).saveAll(any());
        assertThat(service.getLastSyncAt()).isNull();
    }

    @Test
    void findAllLatest_deveRetornarApenasMoedasComCotacaoRegistrada() {
        // given
        stubMonitoredCurrencies("USD", "EUR");
        CurrencyQuote usdQuote = new CurrencyQuote();
        usdQuote.setCode("USD");
        when(quoteRepository.findTopByCodeOrderByQuotedAtDesc("USD"))
                .thenReturn(Optional.of(usdQuote));
        when(quoteRepository.findTopByCodeOrderByQuotedAtDesc("EUR")).thenReturn(Optional.empty());

        // when
        List<CurrencyQuote> result = service.findAllLatest();

        // then
        assertThat(result).containsExactly(usdQuote);
    }

    @Test
    void findLatestByCode_semCotacaoRegistrada_deveLancarNoSuchElementException() {
        // given
        when(quoteRepository.findTopByCodeOrderByQuotedAtDesc("XYZ")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.findLatestByCode("XYZ"))
                .isInstanceOf(NoSuchElementException.class).hasMessageContaining("XYZ");
    }

    @Test
    void findLatestByCode_comCotacaoRegistrada_deveRetornarCotacao() {
        // given
        CurrencyQuote quote = new CurrencyQuote();
        quote.setCode("USD");
        when(quoteRepository.findTopByCodeOrderByQuotedAtDesc("USD"))
                .thenReturn(Optional.of(quote));

        // when
        CurrencyQuote result = service.findLatestByCode("USD");

        // then
        assertThat(result).isSameAs(quote);
    }

    private QuoteService serviceAt(LocalDateTime dateTime) {
        Clock clock = Clock.fixed(dateTime.atZone(SP).toInstant(), SP);
        return new QuoteService(awesomeApiClient, quoteRepository, monitoredCurrencyRepository,
                dailyCloseService, clock);
    }

    private void stubMonitoredCurrencies(String... codes) {
        List<MonitoredCurrency> currencies = List.of(codes).stream().map(code -> {
            MonitoredCurrency currency = new MonitoredCurrency();
            currency.setCode(code);
            return currency;
        }).toList();
        when(monitoredCurrencyRepository.findAll()).thenReturn(currencies);
    }

    private AwesomeApiQuoteDTO dto(String code, String bid) {
        return new AwesomeApiQuoteDTO(code, "BRL", "Dólar", "5.50", "5.30", bid, "5.41", "0.01",
                "0.1", "1700000000");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<CurrencyQuote>> captorForQuoteList() {
        return ArgumentCaptor.forClass(List.class);
    }
}
