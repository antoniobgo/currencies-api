package com.atwo.currencies_api.currency.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.atwo.currencies_api.currency.dtos.LoginRequest;
import com.atwo.currencies_api.currency.dtos.LoginResponse;
import com.atwo.currencies_api.currency.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String HASH_CONFIGURADO = "$2a$10$hashFalsoParaTeste";

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(passwordEncoder, jwtService, HASH_CONFIGURADO);
    }

    @Test
    void login_senhaCorreta_deveRetornarTokenComStatus200() {
        // given
        when(passwordEncoder.matches("senhaCorreta", HASH_CONFIGURADO)).thenReturn(true);
        when(jwtService.generateToken()).thenReturn("token-gerado");

        // when
        ResponseEntity<LoginResponse> response = controller.login(new LoginRequest("senhaCorreta"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().token()).isEqualTo("token-gerado");
    }

    @Test
    void login_senhaIncorreta_deveRetornar401() {
        // given
        when(passwordEncoder.matches("senhaErrada", HASH_CONFIGURADO)).thenReturn(false);

        // when
        ResponseEntity<LoginResponse> response = controller.login(new LoginRequest("senhaErrada"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_hashConfiguradoInvalido_deveRetornar401SemLancarExcecao() {
        // given
        when(passwordEncoder.matches("qualquerSenha", HASH_CONFIGURADO))
                .thenThrow(new IllegalArgumentException("Encoded password does not look like BCrypt"));

        // when
        ResponseEntity<LoginResponse> response = controller.login(new LoginRequest("qualquerSenha"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
