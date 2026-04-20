package borgol.auth.endpoint;

import borgol.auth.dto.*;
import borgol.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.*;

/**
 * SOAP endpoint – dispatches incoming requests to AuthService.
 *
 * All three operations are routed by the XML element local name:
 *   RegisterUserRequest  → register()
 *   LoginUserRequest     → login()
 *   ValidateTokenRequest → validateToken()
 *
 * Namespace must match XSD targetNamespace and package-info.java:
 *   http://num.edu.mn/soapauth
 */
@Endpoint
public class AuthEndpoint {

    private static final String NAMESPACE = "http://num.edu.mn/soapauth";

    @Autowired
    private AuthService authService;

    // ── RegisterUser ──────────────────────────────────────────────────────────

    /**
     * POST /ws  with RegisterUserRequest payload.
     * Creates a new user account and returns the assigned userId.
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "RegisterUserRequest")
    @ResponsePayload
    public RegisterUserResponse register(@RequestPayload RegisterUserRequest request) {
        System.out.printf("[SOAP Endpoint] RegisterUser: username=%s email=%s%n",
                          request.getUsername(), request.getEmail());
        return authService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword());
    }

    // ── LoginUser ─────────────────────────────────────────────────────────────

    /**
     * POST /ws  with LoginUserRequest payload.
     * Validates credentials and returns a JWT token on success.
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "LoginUserRequest")
    @ResponsePayload
    public LoginUserResponse login(@RequestPayload LoginUserRequest request) {
        System.out.printf("[SOAP Endpoint] LoginUser: email=%s%n", request.getEmail());
        return authService.login(request.getEmail(), request.getPassword());
    }

    // ── ValidateToken ─────────────────────────────────────────────────────────

    /**
     * POST /ws  with ValidateTokenRequest payload.
     * Called by the JSON service middleware for every protected request.
     * Returns valid=true + userId/username when the token is good.
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "ValidateTokenRequest")
    @ResponsePayload
    public ValidateTokenResponse validateToken(@RequestPayload ValidateTokenRequest request) {
        return authService.validateToken(request.getToken());
    }
}
