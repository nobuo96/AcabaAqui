package com.acabaaqui.services;

import com.acabaaqui.model.User;
import com.acabaaqui.model.UserCredential;
import com.acabaaqui.persistence.UserRepository;
import com.acabaaqui.persistence.UserCredentialRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository repository;

    @Inject
    UserCredentialRepository credentialRepository;

    @Inject
    PasswordHasher passwordHasher;

    @ConfigProperty(name = "app.jwt.secret")
    String jwtSecret;

    @ConfigProperty(name = "app.jwt.issuer", defaultValue = "acaba-aqui")
    String jwtIssuer;

    @ConfigProperty(name = "app.jwt.duration-seconds", defaultValue = "3600")
    long tokenDurationSeconds;

    public String autenticar(String email, String senha) {
        User user = repository.buscarUsuarioPorEmail(email);
        UserCredential credential = user == null ? null : credentialRepository.buscarPorUsuarioId(user.getId());
        if (user == null || credential == null || Boolean.FALSE.equals(credential.getAtivo())
            || !passwordHasher.verificarSenha(senha, credential)
            || Boolean.FALSE.equals(user.getAtivo())) {
            throw new WebApplicationException("E-mail ou senha inválidos", Response.Status.UNAUTHORIZED);
        }

        return gerarToken(user);
    }

    public String gerarToken(User user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(jwtIssuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(tokenDurationSeconds)))
                .claim("roles", List.of(user.getPerfil().toString()))
                .signWith(signingKey())
                .compact();
    }

    public long getDuracaoTokenSegundos() {
        return tokenDurationSeconds;
    }

    public String getPerfil(String token) {
        Claims claims = validarToken(token);
        List<String> roles = claims.get("roles", List.class);
        if (roles == null || roles.isEmpty()) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        return roles.get(0);
    }

    public Integer getUsuarioId(String token) {
        return Integer.valueOf(validarToken(token).getSubject());
    }

    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(jwtIssuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}