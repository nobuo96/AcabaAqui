package com.acabaaqui.services;

import com.acabaaqui.model.PerfilUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SocialAuthService {

    @Inject
    SocialProviderVerificationService providerVerificationService;

    @Inject
    SocialAccountService socialAccountService;

    public LoginResult autenticar(String provider, String credential, PerfilUser profile,
                                  String name, String phone) {
        SocialProviderVerificationService.VerifiedIdentity identity =
                providerVerificationService.verify(provider, credential);
        return socialAccountService.autenticar(identity, profile, name, phone);
    }

    public record LoginResult(
            boolean requerPerfil,
            String nome,
            String email,
            String token,
            Integer userId,
            String perfil,
            long expiresInSeconds) { }
}
