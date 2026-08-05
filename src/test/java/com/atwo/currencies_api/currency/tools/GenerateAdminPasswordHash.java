package com.atwo.currencies_api.currency.tools;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilitário manual, não roda em `mvnw test` normal (nome não bate com o
 * padrão do Surefire). Rodar sob demanda com:
 * ./mvnw test -Dtest=GenerateAdminPasswordHash -DSENHA=suaSenhaAqui
 */
class GenerateAdminPasswordHash {

    @Test
    void generate() {
        String senha = System.getProperty("SENHA");
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException(
                    "Passe a senha via -DSENHA=suaSenhaAqui");
        }
        System.out.println(new BCryptPasswordEncoder().encode(senha));
    }
}
