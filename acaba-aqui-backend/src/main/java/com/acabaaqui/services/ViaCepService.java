package com.acabaaqui.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class ViaCepService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Inject
    ObjectMapper objectMapper;

    public CepResponse consultar(String cep) {
        String cepNormalizado = normalizarCep(cep);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://viacep.com.br/ws/" + cepNormalizado + "/json/"))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new WebApplicationException("O serviço de CEP está indisponível.",
                        Response.Status.BAD_GATEWAY);
            }
            return mapearResposta(objectMapper.readTree(response.body()));
        } catch (java.net.http.HttpTimeoutException exception) {
            throw new WebApplicationException("A consulta de CEP excedeu o tempo limite.",
                    exception, Response.Status.GATEWAY_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WebApplicationException("A consulta de CEP foi interrompida.",
                    exception, Response.Status.SERVICE_UNAVAILABLE);
        } catch (IOException exception) {
            throw new WebApplicationException("Não foi possível consultar o serviço de CEP.",
                    exception, Response.Status.BAD_GATEWAY);
        }
    }

    static String normalizarCep(String cep) {
        if (cep == null || !cep.matches("\\d{5}-?\\d{3}")) {
            throw new WebApplicationException("Informe um CEP válido com 8 dígitos.",
                    Response.Status.BAD_REQUEST);
        }
        return cep.replace("-", "");
    }

    CepResponse mapearResposta(JsonNode json) {
        if (json == null || !json.isObject()) {
            throw new WebApplicationException("O serviço de CEP retornou uma resposta inválida.",
                    Response.Status.BAD_GATEWAY);
        }
        if (json.path("erro").asBoolean(false)) {
            throw new WebApplicationException("CEP não encontrado.", Response.Status.NOT_FOUND);
        }

        String cep = texto(json, "cep");
        String cidade = texto(json, "localidade");
        String uf = texto(json, "uf");
        if (cep.isBlank() || cidade.isBlank() || uf.isBlank()) {
            throw new WebApplicationException("O serviço de CEP retornou uma resposta incompleta.",
                    Response.Status.BAD_GATEWAY);
        }
        return new CepResponse(
                cep,
                texto(json, "logradouro"),
                texto(json, "complemento"),
                texto(json, "bairro"),
                cidade,
                uf);
    }

    private String texto(JsonNode json, String field) {
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    public record CepResponse(String cep, String logradouro, String complemento,
                              String bairro, String cidade, String uf) { }
}
