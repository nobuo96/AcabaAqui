package com.acabaaqui.services;

import com.acabaaqui.model.ServiceExecutionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceDisputePolicyTest {

    @Test
    void allowsOpeningOnlyAfterStartAndBeforeApproval() {
        LocalDateTime started = LocalDateTime.now();
        assertDoesNotThrow(() -> ServiceDisputePolicy.validarAbertura(
                ServiceExecutionStatus.EM_ANDAMENTO, started, null));
        assertDoesNotThrow(() -> ServiceDisputePolicy.validarAbertura(
                ServiceExecutionStatus.CONCLUIDA_PELO_PRESTADOR, started, null));
        assertThrows(IllegalStateException.class, () -> ServiceDisputePolicy.validarAbertura(
                ServiceExecutionStatus.AGUARDANDO_INICIO, null, null));
        assertThrows(IllegalStateException.class, () -> ServiceDisputePolicy.validarAbertura(
                ServiceExecutionStatus.APROVADA, started, started));
    }

    @Test
    void enforcesEvidenceWindowAndPerParticipantFileLimit() {
        LocalDateTime deadline = LocalDateTime.now();
        assertEquals(deadline, ServiceDisputePolicy.validarPrazoEvidencia(9, deadline, deadline));
        assertThrows(IllegalStateException.class, () -> ServiceDisputePolicy.validarPrazoEvidencia(
                0, deadline, deadline.plusNanos(1)));
        assertThrows(IllegalStateException.class, () -> ServiceDisputePolicy.validarPrazoEvidencia(
                ServiceDisputePolicy.MAX_FILES_PER_PARTICIPANT, deadline, deadline.minusDays(1)));
    }

    @Test
    void acceptsOnlyMatchingJpegPngAndPdfSignatures() {
        byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
        byte[] pdf = "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        assertEquals("image/jpeg", ServiceDisputePolicy.validarEvidencia(
                "foto.jpg", "image/jpeg", jpeg).contentType());
        assertEquals("image/png", ServiceDisputePolicy.validarEvidencia(
                "foto.png", "image/png", png).contentType());
        assertEquals("application/pdf", ServiceDisputePolicy.validarEvidencia(
                "documento.pdf", "application/pdf", pdf).contentType());
        assertEquals("image/jpeg", ServiceDisputePolicy.validarEvidencia(
                "foto.jpg", "application/octet-stream", jpeg).contentType());
        assertEquals("image/jpeg", ServiceDisputePolicy.validarEvidencia(
                "foto.jpg", "image/jpg", jpeg).contentType());
        assertThrows(IllegalArgumentException.class, () -> ServiceDisputePolicy.validarEvidencia(
                "video.mp4", "video/mp4", jpeg));
        assertThrows(IllegalArgumentException.class, () -> ServiceDisputePolicy.validarEvidencia(
                "foto.png", "image/png", jpeg));
    }

    @Test
    void rejectsEvidenceLargerThanTenMegabytesAndSanitizesPath() {
        byte[] oversized = new byte[ServiceDisputePolicy.MAX_FILE_BYTES + 1];
        assertThrows(IllegalArgumentException.class, () -> ServiceDisputePolicy.validarEvidencia(
                "foto.jpg", "image/jpeg", oversized));

        byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
        assertEquals("foto.jpg", ServiceDisputePolicy.validarEvidencia(
                "C:\\fakepath\\foto.jpg", "image/jpeg", jpeg).filename());
        assertThrows(IllegalArgumentException.class, () -> ServiceDisputePolicy.validarTexto(" ", "O motivo"));
    }
}
