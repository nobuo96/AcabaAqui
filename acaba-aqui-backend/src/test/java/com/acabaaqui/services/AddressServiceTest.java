package com.acabaaqui.services;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddressServiceTest {

    @Test
    void validatesAddressDataAndAcceptsFormattedCep() {
        AddressService.validar(new AddressService.AddressRequest(
                "01001-000", "Praça da Sé", "10", "", "Sé", "São Paulo", "sp", "Casa", false));
    }

    @Test
    void rejectsInvalidCepAndMissingNumber() {
        IllegalArgumentException invalidCep = assertThrows(IllegalArgumentException.class,
                () -> AddressService.validar(new AddressService.AddressRequest(
                        "123", "Rua", "10", null, null, "São Paulo", "SP", null, false)));
        assertEquals("Informe um CEP válido com 8 dígitos.", invalidCep.getMessage());

        IllegalArgumentException missingNumber = assertThrows(IllegalArgumentException.class,
                () -> AddressService.validar(new AddressService.AddressRequest(
                        "01001000", "Rua", " ", null, null, "São Paulo", "SP", null, false)));
        assertEquals("Informe o número do endereço ou s/n.", missingNumber.getMessage());
    }

    @Test
    void rejectsNonDigitCharactersInCep() {
        assertThrows(IllegalArgumentException.class,
                () -> AddressService.validar(new AddressService.AddressRequest(
                        "abc01001000", "Rua", "10", null, null, "São Paulo", "SP", null, false)));
    }

    @Test
    void rejectsInvalidUf() {
        assertThrows(IllegalArgumentException.class,
                () -> AddressService.validar(new AddressService.AddressRequest(
                        "01001000", "Rua", "10", null, null, "São Paulo", "São Paulo", null, false)));
    }
}
