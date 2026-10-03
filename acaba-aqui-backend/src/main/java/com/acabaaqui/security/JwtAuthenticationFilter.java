package com.acabaaqui.security;

import com.acabaaqui.services.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    @Inject
    AuthService authService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        String normalizedPath = path == null ? "" : path.replaceAll("^/+|/+$", "");
        if ("OPTIONS".equals(requestContext.getMethod())
                || ("POST".equals(requestContext.getMethod()) && isPublicPost(normalizedPath))) {
            return;
        }

        String authorization = requestContext.getHeaderString("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            abortUnauthorized(requestContext);
            return;
        }

        try {
                Claims claims = authService.validarToken(authorization.substring("Bearer ".length()));
            List<String> roles = claims.get("roles", List.class);
            String userId = claims.getSubject();
            SecurityContext original = requestContext.getSecurityContext();
            requestContext.setSecurityContext(new AuthenticatedSecurityContext(
                    userId, roles == null ? List.of() : roles, original.isSecure(), original.getAuthenticationScheme()));
        } catch (RuntimeException exception) {
            abortUnauthorized(requestContext);
        }
    }

    private boolean isPublicPost(String path) {
        return "auth/login".equals(path)
                || "auth/email-verification/request".equals(path)
                || "auth/email-verification/resend".equals(path)
                || "auth/email-verification/confirm".equals(path);
    }

    private void abortUnauthorized(ContainerRequestContext requestContext) {
        requestContext.abortWith(jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", "Bearer")
                .build());
    }

    private record AuthenticatedSecurityContext(String userId, List<String> roles, boolean secure, String scheme)
            implements SecurityContext {
        @Override
        public Principal getUserPrincipal() {
            return () -> userId;
        }

        @Override
        public boolean isUserInRole(String role) {
            return roles.contains(role);
        }

        @Override
        public boolean isSecure() {
            return secure;
        }

        @Override
        public String getAuthenticationScheme() {
            return scheme == null ? "Bearer" : scheme;
        }
    }
}