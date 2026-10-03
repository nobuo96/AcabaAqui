package com.acabaaqui.services;

import com.acabaaqui.model.AlgoritmoCredential;
import com.acabaaqui.model.UserCredential;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashesAndVerifiesPasswordUsingCredentialFields() {
        UserCredential credential = new UserCredential();

        passwordHasher.definirSenha(credential, "SenhaDeTeste123");

        assertEquals(AlgoritmoCredential.pbkdf2.toString(), credential.getAlgoritmo().toString());
        assertNotNull(credential.getSalt());
        assertNotEquals("SenhaDeTeste123", credential.getSenhaHash());
        assertTrue(passwordHasher.verificarSenha("SenhaDeTeste123", credential));
        assertFalse(passwordHasher.verificarSenha("senha-incorreta", credential));
    }
}