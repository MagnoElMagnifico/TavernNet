package tavernnet.utils;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tavernnet.service.AuthService;

import java.io.IOException;

/** Se usa para añadir al contexto de la peticion un objeto <code>Authentication</code>
 * que sirva para identificar al usuario. Se hace en todas las peticiones.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final AuthService auth;

    @Autowired
    public JwtFilter(AuthService auth) {
        this.auth = auth;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException, JwtException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Si no hay cabecera, no autenticar
        if(header == null || !header.startsWith("Bearer ")){
            chain.doFilter(request, response);
            return;
        }

        // Comprobar jwt de la cabecera
        try {
            SecurityContextHolder
                .getContext()
                .setAuthentication(auth.validateAuthHeader(header));
        } catch (JwtException e) {
            // Token inválido o expirado: limpiar contexto y responder
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
            return;
        }

        chain.doFilter(request, response);
    }
}
