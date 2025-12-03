package tavernnet.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import tavernnet.service.AuthService;
import tavernnet.utils.JwtFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;
    //private final AuthService auth;

    @Autowired
    public SecurityConfiguration(JwtFilter jwtFilter, AuthService auth) {
        this.jwtFilter = jwtFilter;
        //this.auth = auth;
    }

    /*
    @Bean
    public AuthService getAuthService() {
        return auth;
    }
    */

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.authorizeHttpRequests(authorize -> authorize
                // Necesarias para poder autenticarse, refrescar el token y crear usuarios
                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()

                // Las parties son todas privadas
                .requestMatchers(HttpMethod.POST, "/parties/**").authenticated()

                // En general, las operaciones de lectura estan permitidas
                .requestMatchers(HttpMethod.GET, "/**").permitAll()

                // El resto, todas deben llevar un JWT, o bien recibira un 401
                .anyRequest().authenticated()
            )
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Ejecutar nuestro filtro antes del de spring: si existe la cabecera, se pillara aqui
            .addFilterBefore(jwtFilter, BasicAuthenticationFilter.class)
            // Si el filtro lanza una excepcion, significa que el token no es valido: no esta autenticado
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                })
            )
            .build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl.Builder builder = RoleHierarchyImpl.withRolePrefix("ROLE_");
        builder.role("ADMIN").implies("USER");
        return builder.build();
    }
}
