package tavernnet.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import tavernnet.controller.ErrorController;
import tavernnet.service.AuthService;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Se usa para añadir al contexto de la peticion un objeto <code>Authentication</code>
 * que sirva para identificar al usuario. Se hace en todas las peticiones.
 */
@Component
@NullMarked
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private final AuthService auth;
    private final ObjectMapper objMapper;

    @Autowired
    public JwtFilter(AuthService auth, ObjectMapper objMapper) {
        this.auth = auth;
        this.objMapper = objMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Si no hay cabecera, no autenticar
        if(header == null || !header.startsWith("Bearer ")){
            chain.doFilter(request, response);
            return;
        }

        try {
            // Comprobar jwt de la cabecera
            SecurityContextHolder
                .getContext()
                .setAuthentication(auth.validateAuthHeader(header));
        }

        // Token inválido o expirado: limpiar contexto y responder con error
        catch (JwtException ex) {
            SecurityContextHolder.clearContext();
            log.warn("Invalid credentials {}: {}", request.getRequestURI(), ex.getMessage());

            // Configurar el problem details
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
            problem.setTitle("Invalid credentials");
            problem.setDetail(ex.getMessage());
            problem.setType(getType("invalid-credentials"));
            problem.setProperty("path", request.getRequestURI());

            sendError(response, problem);
            return;
        }

        // Si no ha habido errores, continuar
        chain.doFilter(request, response);
    }

    private URI getType(String name) {
        return MvcUriComponentsBuilder
            .fromController(ErrorController.class)
            .pathSegment("error", name)
            .build()
            .toUri();
    }

   private void sendError(HttpServletResponse response, ProblemDetail problem) throws IOException {
       // Enviar manualmente, ya que esto se ejecuta antes de Spring
       response.reset();
       response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
       response.setContentType("application/problem+json");
       response.setCharacterEncoding(StandardCharsets.UTF_8.name());

       // Serializar ProblemDetails
       objMapper.writeValue(response.getOutputStream(), problem);
       response.flushBuffer();
   }
}
