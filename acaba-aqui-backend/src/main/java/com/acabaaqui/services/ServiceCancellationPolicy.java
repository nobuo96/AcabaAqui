package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;

public final class ServiceCancellationPolicy {

    private ServiceCancellationPolicy() {
    }

    public static String validarMotivo(String reason) {
        if (reason == null || reason.trim().length() < 5 || reason.trim().length() > 1000) {
            throw new IllegalArgumentException("Informe um motivo com 5 a 1000 caracteres.");
        }
        return reason.trim();
    }

    public static void validarCancelamento(PaymentStatus paymentStatus, boolean executionStarted) {
        if (executionStarted) {
            throw new IllegalStateException("O serviço não pode ser cancelado após o início da execução.");
        }
        if (paymentStatus != PaymentStatus.PAGO && paymentStatus != PaymentStatus.REEMBOLSO_PENDENTE) {
            throw new IllegalStateException("Somente um serviço pago pode ser cancelado.");
        }
    }
}
