package com.acabaaqui.services;

import com.acabaaqui.model.PasswordResetChallenge;
import com.acabaaqui.model.User;
import com.acabaaqui.model.UserCredential;
import com.acabaaqui.persistence.PasswordResetChallengeRepository;
import com.acabaaqui.persistence.UserCredentialRepository;
import com.acabaaqui.persistence.UserRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@ApplicationScoped
public class PasswordResetService {

    private static final Logger LOG = Logger.getLogger(PasswordResetService.class);
    private static final String HMAC_PURPOSE = "password-reset";
    private static final int CODE_TTL_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_SENDS_PER_HOUR = 3;
    private static final int MAX_ATTEMPTS = 5;

    @Inject
    UserRepository userRepository;

    @Inject
    UserCredentialRepository credentialRepository;

    @Inject
    PasswordResetChallengeRepository challengeRepository;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    VerificationCodeService codeService;

    @Inject
    Mailer mailer;

    @Transactional
    public void solicitar(String email) {
        String normalizedEmail = normalizarEmail(email);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        challengeRepository.removerExpiradosAntesDe(now.minusHours(1));

        User user = userRepository.buscarUsuarioPorEmail(normalizedEmail);
        if (user == null || Boolean.FALSE.equals(user.getAtivo())) {
            return;
        }

        UserCredential credential = credentialRepository.buscarPorUsuarioId(user.getId());
        if (credential == null || Boolean.FALSE.equals(credential.getAtivo())) {
            return;
        }

        PasswordResetChallenge challenge = challengeRepository.buscarPorUsuarioComLock(user.getId());
        if (challenge != null && !podeEnviarNovamente(challenge, now)) {
            return;
        }

        String code = codeService.gerarCodigo();
        if (challenge == null) {
            challenge = new PasswordResetChallenge();
            challenge.setUsuarioId(user.getId());
            challenge.setCriadoEm(now);
            challenge.setInicioJanelaEnvioEm(now);
            challenge.setEnviosNaJanela(1);
        } else {
            atualizarContagemEnvios(challenge, now);
        }
        atualizarCodigo(challenge, normalizedEmail, code, now);
        if (challenge.getId() == null) {
            challengeRepository.salvar(challenge);
        }
        enviarEmail(normalizedEmail, code);
    }

    @Transactional
    public void reenviar(String email) {
        String normalizedEmail = normalizarEmail(email);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        challengeRepository.removerExpiradosAntesDe(now.minusHours(1));

        User user = userRepository.buscarUsuarioPorEmail(normalizedEmail);
        if (user == null || Boolean.FALSE.equals(user.getAtivo())) {
            return;
        }
        UserCredential credential = credentialRepository.buscarPorUsuarioId(user.getId());
        if (credential == null || Boolean.FALSE.equals(credential.getAtivo())) {
            return;
        }

        PasswordResetChallenge challenge = challengeRepository.buscarPorUsuarioComLock(user.getId());
        if (challenge == null || !podeEnviarNovamente(challenge, now)) {
            return;
        }

        atualizarContagemEnvios(challenge, now);
        String code = codeService.gerarCodigo();
        atualizarCodigo(challenge, normalizedEmail, code, now);
        enviarEmail(normalizedEmail, code);
    }

    @Transactional
    public boolean confirmar(String email, String code, String newPassword) {
        String normalizedEmail = normalizarEmail(email);
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres");
        }

        User user = userRepository.buscarUsuarioPorEmail(normalizedEmail);
        if (user == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PasswordResetChallenge challenge = challengeRepository.buscarPorUsuarioComLock(user.getId());
        if (challenge == null || !challenge.getExpiraEm().isAfter(now)
                || challenge.getTentativas() >= MAX_ATTEMPTS) {
            if (challenge != null && !challenge.getExpiraEm().isAfter(now)) {
                challengeRepository.remover(challenge);
            }
            return false;
        }

        if (code == null || !code.matches("\\d{6}")
                || !codeService.verificar(HMAC_PURPOSE, normalizedEmail, code, challenge.getCodigoHmac())) {
            challenge.setTentativas(challenge.getTentativas() + 1);
            return false;
        }

        UserCredential credential = credentialRepository.buscarPorUsuarioId(user.getId());
        if (credential == null || Boolean.FALSE.equals(credential.getAtivo())) {
            return false;
        }

        passwordHasher.definirSenha(credential, newPassword);
        credential.setVersaoToken((credential.getVersaoToken() == null ? 0 : credential.getVersaoToken()) + 1);
        user.setEmailVerificado(true);
        challengeRepository.remover(challenge);
        return true;
    }

    private void atualizarCodigo(PasswordResetChallenge challenge, String email, String code,
                                 LocalDateTime now) {
        challenge.setCodigoHmac(codeService.calcularHmac(HMAC_PURPOSE, email, code));
        challenge.setTentativas(0);
        challenge.setUltimoEnvioEm(now);
        challenge.setExpiraEm(now.plusMinutes(CODE_TTL_MINUTES));
    }

    private boolean podeEnviarNovamente(PasswordResetChallenge challenge, LocalDateTime now) {
        if (now.isBefore(challenge.getUltimoEnvioEm().plusSeconds(RESEND_COOLDOWN_SECONDS))) {
            return false;
        }
        return !now.isBefore(challenge.getInicioJanelaEnvioEm().plusHours(1))
                || challenge.getEnviosNaJanela() < MAX_SENDS_PER_HOUR;
    }

    private void atualizarContagemEnvios(PasswordResetChallenge challenge, LocalDateTime now) {
        if (!now.isBefore(challenge.getInicioJanelaEnvioEm().plusHours(1))) {
            challenge.setInicioJanelaEnvioEm(now);
            challenge.setEnviosNaJanela(1);
        } else {
            challenge.setEnviosNaJanela(challenge.getEnviosNaJanela() + 1);
        }
    }

    private void enviarEmail(String email, String code) {
        try {
            mailer.send(Mail.withText(email, "Redefinição de senha - AcabaAqui",
                    "Seu código para redefinir a senha é " + code + ". Ele expira em " + CODE_TTL_MINUTES
                            + " minutos. Se você não solicitou a redefinição, ignore esta mensagem."));
        } catch (RuntimeException exception) {
            LOG.warn("Falha ao enviar mensagem de redefinição de senha.");
        }
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 150 || !normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        return normalized;
    }
}