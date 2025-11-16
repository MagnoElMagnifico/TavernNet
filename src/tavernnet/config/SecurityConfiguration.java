package tavernnet.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import tavernnet.service.AuthService;
import tavernnet.utils.JwtFilter;

@Configuration
@EnableMethodSecurity()
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;
    private final AuthService auth;

    @Autowired
    public SecurityConfiguration(JwtFilter jwtFilter, AuthService auth) {
        this.jwtFilter = jwtFilter;
        this.auth = auth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize
                // Se deben autenticar todas las peticiones.
                // Si no llevan el jwt, el filtro autenticará y se rechazará desde los controladores
                .anyRequest().authenticated()
            )
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterAfter(jwtFilter, BasicAuthenticationFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return auth.loadRoleHierarchy();
    }
}
