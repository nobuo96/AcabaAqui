package com.acabaaqui.services;

import com.acabaaqui.model.ServiceWarrantyStatus;

import java.time.LocalDateTime;

public final class ServiceWarrantyPolicy {

    public static final long CORRECTION_WINDOW_DAYS = 7;

    private ServiceWarrantyPolicy() {
    }

    public static void validarAbertura(
            ServiceWarrantyStatus status,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            LocalDateTime now) {
        if (status != ServiceWarrantyStatus.ATIVA
                || startsAt == null
                || endsAt == null
                || now.isBefore(startsAt)
                || now.isAfter(endsAt)) {
            throw new IllegalStateException("A garantia não está disponível para abertura de chamado.");
        }
    }

    public static LocalDateTime prazoCorrecao(LocalDateTime openedAt) {
        return openedAt.plusDays(CORRECTION_WINDOW_DAYS);
    }

    public static void validarCorrecao(
            ServiceWarrantyStatus status,
            LocalDateTime correctionDeadline,
            LocalDateTime now) {
        if (status != ServiceWarrantyStatus.ACIONADA
                || correctionDeadline == null
                || !now.isBefore(correctionDeadline)) {
            throw new IllegalStateException("O chamado não está mais no prazo para correção.");
        }
    }

    public static void validarResolucao(
            ServiceWarrantyStatus status,
            LocalDateTime correctionDeadline,
            LocalDateTime now) {
        boolean activeClaim = status == ServiceWarrantyStatus.ACIONADA
                && correctionDeadline != null
                && now.isBefore(correctionDeadline);
        if (!activeClaim && status != ServiceWarrantyStatus.CORRIGIDA) {
            throw new IllegalStateException("Este chamado não pode ser encerrado como resolvido.");
        }
    }

    public static String validarMotivo(String reason) {
        if (reason == null) {
            throw new IllegalArgumentException("Descreva o problema encontrado.");
        }
        String normalized = reason.trim();
        if (normalized.length() < 5 || normalized.length() > 1000) {
            throw new IllegalArgumentException("O motivo deve ter entre 5 e 1000 caracteres.");
        }
        return normalized;
    }
}
