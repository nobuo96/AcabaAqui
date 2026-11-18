package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.ServiceExecutionStatus;

import java.time.LocalDateTime;

public final class ServiceExecutionPolicy {

    private ServiceExecutionPolicy() {
    }

    public static void validarInicio(
            ServiceExecutionStatus status,
            PaymentStatus paymentStatus,
            boolean cancellationExists) {
        if (status != ServiceExecutionStatus.AGUARDANDO_INICIO) {
            throw new IllegalStateException("Este serviço não está aguardando início.");
        }
        if (paymentStatus != PaymentStatus.PAGO || cancellationExists) {
            throw new IllegalStateException(
                    "O serviço não pode ser iniciado enquanto o pagamento estiver pendente de reembolso ou cancelado.");
        }
    }

    public static void validarConclusao(ServiceExecutionStatus status) {
        if (status != ServiceExecutionStatus.EM_ANDAMENTO) {
            throw new IllegalStateException("Somente um serviço em andamento pode ser concluído.");
        }
    }

    public static void validarAprovacao(
            ServiceExecutionStatus status,
            LocalDateTime approvalDeadline,
            LocalDateTime now) {
        if (status != ServiceExecutionStatus.CONCLUIDA_PELO_PRESTADOR) {
            throw new IllegalStateException("Este serviço não está aguardando aprovação.");
        }
        if (approvalDeadline == null || now.isAfter(approvalDeadline)) {
            throw new IllegalStateException("O prazo para aprovação deste serviço terminou.");
        }
    }
}
