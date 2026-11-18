package com.acabaaqui.services;

import com.acabaaqui.model.PaymentStatus;
import com.acabaaqui.model.ServiceExecution;
import com.acabaaqui.model.ServiceExecutionStatus;
import com.acabaaqui.model.ServicePayoutReason;
import com.acabaaqui.model.ServicePayoutStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ServicePayoutPolicy {

    private ServicePayoutPolicy() {
    }

    public static ServicePayoutReason motivoElegibilidade(ServiceExecution execution, LocalDateTime now) {
        if (execution.getStatus() == ServiceExecutionStatus.APROVADA) {
            return ServicePayoutReason.APROVACAO_CLIENTE;
        }
        if (execution.getStatus() == ServiceExecutionStatus.CONCLUIDA_PELO_PRESTADOR
                && execution.getAprovarAte() != null
                && !execution.getAprovarAte().isAfter(now)) {
            return ServicePayoutReason.PRAZO_EXPIRADO;
        }
        return null;
    }

    public static void validarCriacao(
            PaymentStatus paymentStatus,
            boolean disputeOpen,
            ServicePayoutReason reason) {
        if (reason == null || paymentStatus != PaymentStatus.PAGO || disputeOpen) {
            throw new IllegalStateException("O serviço ainda não está elegível para repasse.");
        }
    }

    public static BigDecimal validarPagamento(
            ServicePayoutStatus payoutStatus,
            PaymentStatus paymentStatus,
            boolean disputeOpen,
            BigDecimal amount,
            BigDecimal maxAmount,
            String transferReference) {
        if (payoutStatus != ServicePayoutStatus.PENDENTE) {
            throw new IllegalStateException("Este repasse já foi registrado como pago.");
        }
        if (paymentStatus != PaymentStatus.REPASSE_PENDENTE) {
            throw new IllegalStateException("O pagamento não está na fila de repasse.");
        }
        if (disputeOpen) {
            throw new IllegalStateException("O repasse está bloqueado enquanto houver disputa aberta.");
        }
        if (amount == null || maxAmount == null || amount.signum() <= 0
                || amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Informe um valor positivo que não ultrapasse o total pago.");
        }
        if (transferReference == null || transferReference.isBlank()
                || transferReference.trim().length() > 500) {
            throw new IllegalArgumentException("Informe o identificador ou comprovante da transferência Pix.");
        }
        try {
            return amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("O valor do repasse deve ter no máximo duas casas decimais.");
        }
    }
}
