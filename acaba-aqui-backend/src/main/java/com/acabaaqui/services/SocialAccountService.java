package com.acabaaqui.services;

import com.acabaaqui.model.PerfilUser;
import com.acabaaqui.model.SocialIdentity;
import com.acabaaqui.model.User;
import com.acabaaqui.model.UserCredential;
import com.acabaaqui.persistence.SocialIdentityRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@ApplicationScoped
public class SocialAccountService {

    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    SocialIdentityRepository socialIdentityRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserService userService;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    AuthService authService;

    @Transactional
    public SocialAuthService.LoginResult autenticar(
            SocialProviderVerificationService.VerifiedIdentity identity, PerfilUser requestedProfile,
            String requestedName, String phone) {
        SocialIdentity linkedIdentity = socialIdentityRepository.buscar(identity.provider(), identity.subject());
        if (linkedIdentity != null) {
            User user = userRepository.buscarUsuarioPorId(linkedIdentity.getUsuarioId());
            if (user == null || Boolean.FALSE.equals(user.getAtivo())) {
                throw new WebApplicationException("Conta desativada ou indisponível.", Response.Status.FORBIDDEN);
            }
            return loginResult(user);
        }

        String normalizedEmail = identity.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.buscarUsuarioPorEmail(normalizedEmail) != null) {
            throw new WebApplicationException(
                    "Este e-mail já possui uma conta. Entre usando o método de acesso cadastrado para essa conta.",
                    Response.Status.CONFLICT);
        }

        if (requestedProfile == null) {
            return new SocialAuthService.LoginResult(
                    true, identity.name(), normalizedEmail, null, null, null, 0);
        }
        if (requestedProfile != PerfilUser.cliente && requestedProfile != PerfilUser.prestador) {
            throw new IllegalArgumentException("Escolha Cliente ou Prestador.");
        }
        if (phone == null || phone.isBlank() || phone.trim().length() > 20) {
            throw new IllegalArgumentException("Informe um telefone válido de até 20 caracteres.");
        }
        String name = identity.name().isBlank() ? requestedName : identity.name();
        if (name == null || name.isBlank() || name.trim().length() > 100
                || normalizedEmail.length() > 150 || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new WebApplicationException("O provedor não retornou dados de perfil válidos.",
                    Response.Status.BAD_REQUEST);
        }

        User user = new User();
        user.setNome(name.trim());
        user.setEmail(normalizedEmail);
        user.setTelefone(phone.trim());
        user.setPerfil(requestedProfile);
        user.setEmailVerificado(true);

        String unusablePassword = generateUnusablePassword();
        UserCredential credential = new UserCredential();
        passwordHasher.definirSenha(credential, unusablePassword);
        userService.validarCadastro(user, unusablePassword);
        userService.cadastrarUsuarioVerificado(user, credential);

        SocialIdentity socialIdentity = new SocialIdentity();
        socialIdentity.setUsuarioId(user.getId());
        socialIdentity.setProvedor(identity.provider());
        socialIdentity.setSubject(identity.subject());
        socialIdentityRepository.adicionar(socialIdentity);

        return loginResult(user);
    }

    private SocialAuthService.LoginResult loginResult(User user) {
        String token = authService.gerarToken(user);
        return new SocialAuthService.LoginResult(
                false, null, null, token, user.getId(), user.getPerfil().toString(),
                authService.getDuracaoTokenSegundos());
    }

    private String generateUnusablePassword() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
