package com.acabaaqui.services;

import com.acabaaqui.model.AlgoritmoCredential;
import com.acabaaqui.model.PendingEmailRegistration;
import com.acabaaqui.model.PerfilUser;
import com.acabaaqui.model.User;
import com.acabaaqui.model.UserCredential;
import com.acabaaqui.persistence.PendingEmailRegistrationRepository;
import com.acabaaqui.persistence.UserRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@ApplicationScoped
public class EmailVerificationService {

    private static final int CODE_TTL_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_SENDS_PER_HOUR = 3;
    private static final int MAX_ATTEMPTS = 5;

    @Inject
    PendingEmailRegistrationRepository pendingRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserService userService;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    VerificationCodeService codeService;

    @Inject
    AuthService authService;

    @Inject
    Mailer mailer;

    @Transactional
    public void solicitarCodigo(User user, String password) {
        prepararDados(user, password);
        String email = user.getEmail();
        if (userRepository.buscarUsuarioPorEmail(email) != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        pendingRepository.removerExpiradosAntesDe(now.minusHours(1));
        PendingEmailRegistration pending = pendingRepository.buscarPorEmailComLock(email);
        if (pending != null && !podeEnviarNovamente(pending, now)) {
            return;
        }

        UserCredential credential = new UserCredential();
        passwordHasher.definirSenha(credential, password);
        if (pending == null) {
            pending = new PendingEmailRegistration();
            pending.setEmail(email);
            pending.setCriadoEm(now);
            pending.setInicioJanelaEnvioEm(now);
            pending.setEnviosNaJanela(1);
        } else {
            atualizarContagemEnvios(pending, now);
        }

        pending.setNome(user.getNome().trim());
        pending.setTelefone(user.getTelefone().trim());
        pending.setPerfil(user.getPerfil());
        pending.setSenhaHash(credential.getSenhaHash());
        pending.setSalt(credential.getSalt());
        pending.setAlgoritmo(credential.getAlgoritmo().name());
        String code = codeService.gerarCodigo();
        pending.setCodigoHmac(codeService.calcularHmac(email, code));
        pending.setTentativas(0);
        pending.setUltimoEnvioEm(now);
        pending.setExpiraEm(now.plusMinutes(CODE_TTL_MINUTES));

        if (pending.getId() == null) {
            pendingRepository.salvar(pending);
        }

        mailer.send(Mail.withText(email, "Confirme seu e-mail - AcabaAqui",
                "Seu código de confirmação é " + code + ". Ele expira em " + CODE_TTL_MINUTES
                        + " minutos. Se você não solicitou este cadastro, ignore esta mensagem."));
    }

    @Transactional
    public void reenviarCodigo(String email) {
        String normalizedEmail = normalizarEmail(email);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        pendingRepository.removerExpiradosAntesDe(now.minusHours(1));
        PendingEmailRegistration pending = pendingRepository.buscarPorEmailComLock(normalizedEmail);
        if (pending == null || !podeEnviarNovamente(pending, now)
                || userRepository.buscarUsuarioPorEmail(normalizedEmail) != null) {
            return;
        }

        String code = codeService.gerarCodigo();
        pending.setCodigoHmac(codeService.calcularHmac(normalizedEmail, code));
        pending.setTentativas(0);
        pending.setUltimoEnvioEm(now);
        pending.setExpiraEm(now.plusMinutes(CODE_TTL_MINUTES));
        atualizarContagemEnvios(pending, now);

        mailer.send(Mail.withText(normalizedEmail, "Novo código de confirmação - AcabaAqui",
                "Seu novo código de confirmação é " + code + ". Ele expira em " + CODE_TTL_MINUTES
                        + " minutos. Se você não solicitou este cadastro, ignore esta mensagem."));
    }

    @Transactional
    public ConfirmationResult confirmar(String email, String code) {
        String normalizedEmail = normalizarEmail(email);
        PendingEmailRegistration pending = pendingRepository.buscarPorEmailComLock(normalizedEmail);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        pendingRepository.removerExpiradosAntesDe(now.minusHours(1));
        if (pending == null || !pending.getExpiraEm().isAfter(now)) {
            if (pending != null) {
                pendingRepository.remover(pending);
            }
            return ConfirmationResult.failure(ConfirmationStatus.INVALID_OR_EXPIRED);
        }

        if (pending.getTentativas() >= MAX_ATTEMPTS) {
            return ConfirmationResult.failure(ConfirmationStatus.TOO_MANY_ATTEMPTS);
        }

        if (code == null || !code.matches("\\d{6}")
                || !codeService.verificar(normalizedEmail, code, pending.getCodigoHmac())) {
            int attempts = pending.getTentativas() + 1;
            pending.setTentativas(attempts);
            return ConfirmationResult.failure(attempts >= MAX_ATTEMPTS
                    ? ConfirmationStatus.TOO_MANY_ATTEMPTS : ConfirmationStatus.INVALID_OR_EXPIRED);
        }

        if (userRepository.buscarUsuarioPorEmail(normalizedEmail) != null) {
            pendingRepository.remover(pending);
            return ConfirmationResult.failure(ConfirmationStatus.INVALID_OR_EXPIRED);
        }

        User user = new User();
        user.setNome(pending.getNome());
        user.setEmail(normalizedEmail);
        user.setTelefone(pending.getTelefone());
        user.setPerfil(pending.getPerfil());
        user.setEmailVerificado(true);

        UserCredential credential = new UserCredential();
        credential.setSenhaHash(pending.getSenhaHash());
        credential.setSalt(pending.getSalt());
        credential.setAlgoritmo(AlgoritmoCredential.valueOf(pending.getAlgoritmo()));

        User createdUser = userService.cadastrarUsuarioVerificado(user, credential);
        pendingRepository.remover(pending);
        String token = authService.gerarToken(createdUser);
        return new ConfirmationResult(ConfirmationStatus.VERIFIED, token, createdUser.getId(),
                createdUser.getPerfil().toString());
    }

    private void prepararDados(User user, String password) {
        if (user == null) {
            throw new IllegalArgumentException("Dados do cadastro inválidos");
        }
        user.setEmail(normalizarEmail(user.getEmail()));
        userService.validarCadastro(user, password);
    }

    private boolean podeEnviarNovamente(PendingEmailRegistration pending, LocalDateTime now) {
        if (now.isBefore(pending.getUltimoEnvioEm().plusSeconds(RESEND_COOLDOWN_SECONDS))) {
            return false;
        }
        return !now.isBefore(pending.getInicioJanelaEnvioEm().plusHours(1))
                || pending.getEnviosNaJanela() < MAX_SENDS_PER_HOUR;
    }

    private void atualizarContagemEnvios(PendingEmailRegistration pending, LocalDateTime now) {
        if (!now.isBefore(pending.getInicioJanelaEnvioEm().plusHours(1))) {
            pending.setInicioJanelaEnvioEm(now);
            pending.setEnviosNaJanela(1);
        } else {
            pending.setEnviosNaJanela(pending.getEnviosNaJanela() + 1);
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

    public enum ConfirmationStatus {
        VERIFIED,
        INVALID_OR_EXPIRED,
        TOO_MANY_ATTEMPTS
    }

    public record ConfirmationResult(ConfirmationStatus status, String token, Integer userId, String perfil) {
        private static ConfirmationResult failure(ConfirmationStatus status) {
            return new ConfirmationResult(status, null, null, null);
        }
    }
}