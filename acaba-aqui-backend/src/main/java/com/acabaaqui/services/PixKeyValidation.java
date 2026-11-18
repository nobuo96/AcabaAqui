package com.acabaaqui.services;

import com.acabaaqui.model.PixKeyType;

import java.util.UUID;
import java.util.regex.Pattern;

public final class PixKeyValidation {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private PixKeyValidation() {
    }

    public static String normalize(PixKeyType type, String value) {
        if (type == null || value == null || value.isBlank() || value.length() > 255) {
            throw new IllegalArgumentException("Informe um tipo e uma chave Pix válida.");
        }
        String trimmed = value.trim();
        return switch (type) {
            case CPF -> {
                String digits = digitsOnly(trimmed);
                if (!validCpf(digits)) {
                    throw new IllegalArgumentException("A chave Pix CPF é inválida.");
                }
                yield digits;
            }
            case CNPJ -> {
                String digits = digitsOnly(trimmed);
                if (!validCnpj(digits)) {
                    throw new IllegalArgumentException("A chave Pix CNPJ é inválida.");
                }
                yield digits;
            }
            case EMAIL -> {
                if (!EMAIL.matcher(trimmed).matches()) {
                    throw new IllegalArgumentException("A chave Pix de e-mail é inválida.");
                }
                yield trimmed;
            }
            case TELEFONE -> {
                String digits = digitsOnly(trimmed);
                if (digits.length() < 10 || digits.length() > 13) {
                    throw new IllegalArgumentException("A chave Pix de telefone deve conter de 10 a 13 dígitos.");
                }
                yield trimmed.startsWith("+") ? "+" + digits : digits;
            }
            case ALEATORIA -> {
                try {
                    yield UUID.fromString(trimmed).toString();
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("A chave Pix aleatória deve ser um UUID válido.");
                }
            }
        };
    }

    public static String mascarar(PixKeyType type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (type) {
            case EMAIL -> {
                int separator = value.indexOf('@');
                yield separator <= 0 ? "***" : value.charAt(0) + "***" + value.substring(separator);
            }
            case TELEFONE -> "..." + lastCharacters(digitsOnly(value), 4);
            case CPF, CNPJ -> "... " + lastCharacters(digitsOnly(value), 2);
            case ALEATORIA -> value.length() <= 8
                    ? "********"
                    : value.substring(0, 4) + "..." + value.substring(value.length() - 4);
        };
    }

    private static String digitsOnly(String value) {
        return value.replaceAll("\\D", "");
    }

    private static String lastCharacters(String value, int count) {
        return value.length() <= count ? value : value.substring(value.length() - count);
    }

    private static boolean validCpf(String digits) {
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        int first = checkDigit(digits, 9, 10);
        int second = checkDigit(digits, 10, 11);
        return digits.charAt(9) - '0' == first && digits.charAt(10) - '0' == second;
    }

    private static int checkDigit(String digits, int length, int initialWeight) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (digits.charAt(index) - '0') * (initialWeight - index);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private static boolean validCnpj(String digits) {
        if (digits.length() != 14 || digits.chars().distinct().count() == 1) {
            return false;
        }
        int first = cnpjCheckDigit(digits, 12);
        int second = cnpjCheckDigit(digits, 13);
        return digits.charAt(12) - '0' == first && digits.charAt(13) - '0' == second;
    }

    private static int cnpjCheckDigit(String digits, int length) {
        int[] weights = length == 12
                ? new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (digits.charAt(index) - '0') * weights[index];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
