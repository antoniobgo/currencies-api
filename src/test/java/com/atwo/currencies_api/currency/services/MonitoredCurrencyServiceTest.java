package com.atwo.currencies_api.currency.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.atwo.currencies_api.currency.client.AwesomeApiClient;
import com.atwo.currencies_api.currency.dtos.AwesomeApiQuoteDTO;
import com.atwo.currencies_api.currency.entities.MonitoredCurrency;
import com.atwo.currencies_api.currency.repositories.MonitoredCurrencyRepository;

@ExtendWith(MockitoExtension.class)
class MonitoredCurrencyServiceTest {

    @Mock
    private MonitoredCurrencyRepository repository;

    @Mock
    private AwesomeApiClient awesomeApiClient;

    private MonitoredCurrencyService service;

    @BeforeEach
    void setUp() {
        service = new MonitoredCurrencyService(repository, awesomeApiClient);
    }

    @Test
    void findAll_deveRetornarTodasAsMoedasMonitoradas() {
        // given
        MonitoredCurrency usd = new MonitoredCurrency();
        usd.setCode("USD");
        when(repository.findAll()).thenReturn(List.of(usd));

        // when
        List<MonitoredCurrency> result = service.findAll();

        // then
        assertThat(result).containsExactly(usd);
    }

    @Test
    void add_moedaJaMonitorada_deveLancarIllegalStateException() {
        // given
        when(repository.existsById("USD")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> service.add("USD")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USD");

        verify(awesomeApiClient, never()).fetchQuotes(any());
        verify(repository, never()).save(any());
    }

    @Test
    void add_moedaNaoSuportadaPelaAwesomeApi_deveLancarIllegalArgumentException() {
        // given
        when(repository.existsById("XYZ")).thenReturn(false);
        when(awesomeApiClient.fetchQuotes(List.of("XYZ"))).thenReturn(Map.of());

        // when / then
        assertThatThrownBy(() -> service.add("XYZ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XYZ");

        verify(repository, never()).save(any());
    }

    @Test
    void add_moedaValida_devePersistirCodigoEmMaiusculo() {
        // given
        when(repository.existsById("usd")).thenReturn(false);
        AwesomeApiQuoteDTO dto = new AwesomeApiQuoteDTO("USD", "BRL", "Dólar", "5.4", "5.3", "5.35",
                "5.36", "0.01", "0.1", "1700000000");
        when(awesomeApiClient.fetchQuotes(List.of("usd"))).thenReturn(Map.of("USDBRL", dto));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        MonitoredCurrency result = service.add("usd");

        // then
        ArgumentCaptor<MonitoredCurrency> captor = ArgumentCaptor.forClass(MonitoredCurrency.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("USD");
        assertThat(result.getCode()).isEqualTo("USD");
    }

    @Test
    void remove_moedaNaoEncontrada_deveLancarNoSuchElementException() {
        // given
        when(repository.existsById("USD")).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> service.remove("USD")).isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("USD");

        verify(repository, never()).deleteById(any());
    }

    @Test
    void remove_moedaExistente_deveApenasDeletarMonitoramentoPreservandoHistorico() {
        // given
        when(repository.existsById("USD")).thenReturn(true);

        // when
        service.remove("USD");

        // then
        verify(repository).deleteById(eq("USD"));
    }
}
