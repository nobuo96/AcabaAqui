package com.acabaaqui.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ViaCepServiceTest {

    @Test
    void normalizesFormattedCep() {
        assertEquals("01001000", ViaCepService.normalizarCep("01001-000"));
    }

    @Test
    void rejectsInvalidCepBeforeCallingProvider() {
        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> ViaCepService.normalizarCep("123"));
        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    void rejectsNonDigitCharactersInCep() {
        assertThrows(WebApplicationException.class, () -> ViaCepService.normalizarCep("abc01001000"));
    }

    @Test
    void mapsViaCepAddressFields() throws Exception {
        ViaCepService service = new ViaCepService();
        service.objectMapper = new ObjectMapper();

        ViaCepService.CepResponse response = service.mapearResposta(service.objectMapper.readTree("""
                {
                  "cep": "01001-000",
                  "logradouro": "Praça da Sé",
                  "complemento": "lado ímpar",
                  "bairro": "Sé",
                  "localidade": "São Paulo",
                  "uf": "SP"
                }
                """));

        assertEquals("01001-000", response.cep());
        assertEquals("Praça da Sé", response.logradouro());
        assertEquals("lado ímpar", response.complemento());
        assertEquals("Sé", response.bairro());
        assertEquals("São Paulo", response.cidade());
        assertEquals("SP", response.uf());
    }

    @Test
    void returnsNotFoundWhenViaCepMarksCepAsMissing() throws Exception {
        ViaCepService service = new ViaCepService();
        service.objectMapper = new ObjectMapper();

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> service.mapearResposta(service.objectMapper.readTree("{\"erro\": true}")));

        assertEquals(404, exception.getResponse().getStatus());
    }
}
