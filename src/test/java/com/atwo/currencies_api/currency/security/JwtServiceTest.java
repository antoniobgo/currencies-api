package com.atwo.currencies_api.currency.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET =
            "test-only-secret-not-used-in-any-real-environment-32bytes";

    @Test
    void generateToken_tokenGeradoDeveSerValido() {
        JwtService service = new JwtService(SECRET, 7_200_000);

        String token = service.generateToken();

        assertThat(service.isValid(token)).isTrue();
    }

    @Test
    void isValid_tokenMalformado_deveRetornarFalse() {
        JwtService service = new JwtService(SECRET, 7_200_000);

        assertThat(service.isValid("token-invalido")).isFalse();
    }

    @Test
    void isValid_tokenAssinadoComOutraChave_deveRetornarFalse() {
        JwtService service = new JwtService(SECRET, 7_200_000);
        JwtService outroServico =
                new JwtService("outra-chave-completamente-diferente-tambem-com-32-bytes", 7_200_000);

        String token = outroServico.generateToken();

        assertThat(service.isValid(token)).isFalse();
    }

    @Test
    void isValid_tokenExpirado_deveRetornarFalse() {
        JwtService service = new JwtService(SECRET, -1000);

        String token = service.generateToken();

        assertThat(service.isValid(token)).isFalse();
    }
}
