package com.acabaaqui.services;

import com.acabaaqui.model.PixKeyType;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PixKeyValidationTest {

    @Test
    void normalizesSupportedPixKeyTypes() {
        assertEquals("52998224725", PixKeyValidation.normalize(PixKeyType.CPF, "529.982.247-25"));
        assertEquals("11222333000181", PixKeyValidation.normalize(PixKeyType.CNPJ, "11.222.333/0001-81"));
        assertEquals("copilot@example.com", PixKeyValidation.normalize(PixKeyType.EMAIL, "copilot@example.com"));
        assertEquals("+5511999999999", PixKeyValidation.normalize(PixKeyType.TELEFONE, "+55 (11) 99999-9999"));
        assertEquals("123e4567-e89b-12d3-a456-426614174000",
                PixKeyValidation.normalize(PixKeyType.ALEATORIA, "123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    void rejectsInvalidKeysAndMasksSavedValues() {
        assertThrows(IllegalArgumentException.class,
                () -> PixKeyValidation.normalize(PixKeyType.CPF, "111.111.111-11"));
        assertThrows(IllegalArgumentException.class,
                () -> PixKeyValidation.normalize(PixKeyType.EMAIL, "not-an-email"));
        assertEquals("c***@example.com",
                PixKeyValidation.mascarar(PixKeyType.EMAIL, "copilot@example.com"));
        assertEquals("... 25",
                PixKeyValidation.mascarar(PixKeyType.CPF, "52998224725"));
    }

    @Test
    void encryptsPixKeysWithAUniqueAuthenticatedCiphertext() {
        PixKeyCryptoService cryptoService = new PixKeyCryptoService();
        cryptoService.encryptionSecret = Base64.getEncoder().encodeToString(new byte[32]);
        String first = cryptoService.encrypt("52998224725");
        String second = cryptoService.encrypt("52998224725");

        assertNotEquals("52998224725", first);
        assertNotEquals(first, second);
        assertEquals("52998224725", cryptoService.decrypt(first));
    }
}
