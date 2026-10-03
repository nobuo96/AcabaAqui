package com.acabaaqui.services;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationCodeServiceTest {

    private final VerificationCodeService codeService = new VerificationCodeService(
            Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void generatesSixDigitCodesAndBindsHashToEmail() {
        String email = "cliente@example.com";
        String code = codeService.gerarCodigo();
        String hmac = codeService.calcularHmac(email, code);

        assertTrue(code.matches("\\d{6}"));
        assertTrue(codeService.verificar(email, code, hmac));
        assertFalse(codeService.verificar("outra@example.com", code, hmac));
        assertFalse(codeService.verificar(email, "000000", hmac));
    }
}