package com.acabaaqui.services;

import com.acabaaqui.model.ServiceExecutionStatus;

import java.time.LocalDateTime;
import java.util.Locale;

public final class ServiceDisputePolicy {

    public static final int MAX_FILES_PER_PARTICIPANT = 10;
    public static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    public static final int MAX_FILENAME_LENGTH = 255;
    public static final int MAX_TEXT_LENGTH = 1000;
    public static final long EVIDENCE_WINDOW_DAYS = 7;
    public static final long RETENTION_DAYS = 180;

    private ServiceDisputePolicy() { }

    public static void validarAbertura(
            ServiceExecutionStatus status,
            LocalDateTime startedAt,
            LocalDateTime approvedAt) {
        if (startedAt == null || approvedAt != null
                || (status != ServiceExecutionStatus.EM_ANDAMENTO
                    && status != ServiceExecutionStatus.CONCLUIDA_PELO_PRESTADOR)) {
            throw new IllegalStateException("A disputa só pode ser aberta após o início e antes da aprovação do serviço.");
        }
    }

    public static LocalDateTime validarPrazoEvidencia(
            int existingFiles,
            LocalDateTime deadline,
            LocalDateTime now) {
        if (deadline == null || now.isAfter(deadline)) {
            throw new IllegalStateException("O prazo de 7 dias para anexar evidências foi encerrado.");
        }
        if (existingFiles >= MAX_FILES_PER_PARTICIPANT) {
            throw new IllegalStateException("Cada parte pode anexar no máximo 10 evidências.");
        }
        return deadline;
    }

    public static ValidatedEvidence validarEvidencia(String filename, String contentType, byte[] content) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do arquivo.");
        }
        String safeName = filename.replace('\\', '/');
        safeName = safeName.substring(safeName.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (safeName.isBlank() || safeName.length() > MAX_FILENAME_LENGTH) {
            throw new IllegalArgumentException("O nome do arquivo deve ter até 255 caracteres.");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("O arquivo está vazio.");
        }
        if (content.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Cada evidência pode ter no máximo 10 MB.");
        }

        String extension = safeName.substring(safeName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String expectedType;
        boolean signatureMatches;
        switch (extension) {
            case "jpg", "jpeg" -> {
                expectedType = "image/jpeg";
                signatureMatches = content.length >= 3
                        && (content[0] & 0xff) == 0xff
                        && (content[1] & 0xff) == 0xd8
                        && (content[2] & 0xff) == 0xff;
            }
            case "png" -> {
                expectedType = "image/png";
                signatureMatches = content.length >= 8
                        && (content[0] & 0xff) == 0x89
                        && content[1] == 'P' && content[2] == 'N' && content[3] == 'G'
                        && (content[4] & 0xff) == 0x0d && (content[5] & 0xff) == 0x0a
                        && (content[6] & 0xff) == 0x1a && (content[7] & 0xff) == 0x0a;
            }
            case "pdf" -> {
                expectedType = "application/pdf";
                signatureMatches = content.length >= 5
                        && content[0] == '%' && content[1] == 'P' && content[2] == 'D'
                        && content[3] == 'F' && content[4] == '-';
            }
            default -> throw new IllegalArgumentException("Formato não permitido. Envie JPG, PNG ou PDF.");
        }
        if (!signatureMatches || !mimeTypeMatches(expectedType, contentType)) {
            throw new IllegalArgumentException("O conteúdo do arquivo não corresponde ao formato informado.");
        }
        return new ValidatedEvidence(safeName, expectedType, content);
    }

    private static boolean mimeTypeMatches(String expectedType, String contentType) {
        String normalized = contentType == null
                ? ""
                : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()
                || "application/octet-stream".equals(normalized)
                || "application/binary".equals(normalized)) {
            return true;
        }
        return switch (expectedType) {
            case "image/jpeg" -> "image/jpeg".equals(normalized) || "image/jpg".equals(normalized);
            case "image/png" -> "image/png".equals(normalized);
            case "application/pdf" -> "application/pdf".equals(normalized)
                    || "application/x-pdf".equals(normalized);
            default -> false;
        };
    }

    public static String validarTexto(String value, String field) {
        if (value == null || value.isBlank() || value.trim().length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(field + " deve conter entre 1 e 1000 caracteres.");
        }
        return value.trim();
    }

    public record ValidatedEvidence(String filename, String contentType, byte[] content) { }
}
