package tavernnet.controller;

import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import tavernnet.exception.InvalidRefreshTokenException;
import tavernnet.model.RefreshToken;
import tavernnet.model.User;
import tavernnet.service.AuthService;

@RestController
@RequestMapping("auth")
public class AuthController {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "__Secure-RefreshToken";
    private final AuthService auth;

    @Autowired
    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    private ResponseCookie getResponseCookie(RefreshToken refresh) {
        // Es importante configurar este path para evitar que el refresh jwt
        // se envíe en otras peticiones también. Queremos que solo se mande con
        // el endpoint /auth/refresh.
        String refreshPath = MvcUriComponentsBuilder
            .fromMethodName(
                AuthController.class,
                "refresh",
                ""
            )
            .build()
            .toUri()
            .getPath();

        // Enviar el refresh jwt como una cookie segura
        return ResponseCookie
            .from(
                REFRESH_TOKEN_COOKIE_NAME,
                refresh.uuid()
            )
            .secure(true)
            .httpOnly(true)
            .sameSite(Cookie.SameSite.STRICT.toString())
            .path(refreshPath)
            .maxAge(refresh.ttl())
            .build();
    }

    @PreAuthorize("isAnonymous()")
    @PostMapping("login")
    @NullMarked
    public ResponseEntity<User.@Valid LoginResponse> login(
        @RequestBody @Valid User.LoginRequest user
    ) throws AuthenticationException {
        // Autenticar al usuario creando un objeto Authentication, además de
        // su JWT y el refresh jwt. El JWT se devolverá en el cuerpo de la
        // respuesta, pero el refresh jwt solo podrá ir en la cookie.
        var response = auth.login(user);
        return ResponseEntity
            .ok()
            // Configurar la cabecera Authorization
            .headers(h -> h.setBearerAuth(response.body().jwt()))
            // Ahora añadir la cookie configurada
            .header(HttpHeaders.SET_COOKIE, getResponseCookie(response.refreshToken()).toString())
            // Finalmente añadir el cuerpo
            .body(response.body());
    }

    /** Regenerar un JWT a partir de un <code>RefreshToken</code> */
    @PostMapping("refresh")
    @PreAuthorize("isAuthenticated()")
    @NullMarked
    public ResponseEntity<User.@Valid LoginResponse> refresh(
        @RequestHeader(name=REFRESH_TOKEN_COOKIE_NAME) String refreshToken
    ) throws InvalidRefreshTokenException {
        // Igual que antes, pero ahora se hace a partir del refresh token
        var response = auth.refresh(refreshToken);
        return ResponseEntity
            .ok()
            // Configurar la cabecera Authorization con el nuevo JWT
            .headers(h -> h.setBearerAuth(response.body().jwt()))
            // Actualizar la cookie con el nuevo refresh jwt
            .header(HttpHeaders.SET_COOKIE, getResponseCookie(response.refreshToken()).toString())
            // Añadir el cuerpo con el JWT para el usuario
            .body(response.body());
    }

    /**
     * Revocar <code>RefreshToken</code> para que el usuario tenga que hacer
     * login manualmente otra vez.
     */
    @PostMapping("logout")
    @PreAuthorize("isAuthenticated()")
    @NullMarked
    public ResponseEntity<Void> logout(
        @RequestHeader(name=HttpHeaders.AUTHORIZATION) String jwt
    ) {
        // Borrar el refresh token de redis
        auth.logout(jwt);

        // Desactivar la cookie del refresh token
        ResponseCookie cookie = ResponseCookie
            .from(REFRESH_TOKEN_COOKIE_NAME, null)
            .build();

        return ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build();
    }
}
