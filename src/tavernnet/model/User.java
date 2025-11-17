package tavernnet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

@Document(collection = "users")
public class User implements UserDetails {

    // ==== DTOs DE LOS USUARIOS ===============================================

    /** DTO de lo que envia el usuario cuando quiere iniciar sesion  */
    public record LoginRequest (
        @NotBlank(message = "Username must be not null or blank")
        String username,
        @NotBlank(message = "Username must be not null or blank")
        String password
    ) {
        public LoginRequest(User user) {
            this(user.username, user.passwordHash);
        }
    }

    /** DTO de lo que recibe el usuario tras iniciar sesion  */
    public record LoginResponse(
        @NotBlank(message = "Token must be not null or blank")
        String jwt,
        @NotBlank(message = "Expiration must be not null")
        @NotNull
        Duration expiresIn
    ) {}

    /** DTO de respuesta al listar usuarios */
    public record PublicProfile(
        @NotBlank
        String username,
        @NotNull
        LocalDateTime creation,
        @NotNull
        Collection<Character> characters
    ) {
        public PublicProfile(User user, Collection<Character> characters) {
            this(
                user.getUsername(),
                user.getCreation(),
                characters
            );
        }
    }

    // ==== USUARIO REAL =======================================================

    @Id
    @NotBlank(message = "Username must be not null or blank")
    private final String username;

    /** <strong>IMPORTANTE</strong>: Debe usarse <code>PasswordEncoder</code>. */
    @Field(name = "password")
    @JsonIgnore
    @NotBlank(message = "Password must be not null or blank")
    private final String passwordHash;

    private final Collection<Role> roles;

    private final LocalDateTime creation;

    public User(String username, String passwordHash, Collection<Role> roles, LocalDateTime creation) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.creation = creation;
    }

    @Override
    @NonNull
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @NonNull
    public String getUsername() {
        return username;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles
            .stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.roleName()))
            .toList();
    }

    // TODO: sin esto, @PreAuthorize("hasAuthority('USER:READ')") no funciona
    // porque ahora mismo solo se devuelven los roles, pero no los permisos de cada rol
    /*
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (Role role : roles) {
            collectAuthorities(role, authorities);
        }

        return authorities;
    }

    private void collectAuthorities(Role role, Set<GrantedAuthority> authorities) {

        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.roleName()));

        for (Permission p : role.getPermissions()) {
            authorities.add(new SimpleGrantedAuthority(p.resource() + ":" + p.action()));
        }

        for (Role included : role.getIncludes()) {
            collectAuthorities(included, authorities);
        }
    }
    */

    public LocalDateTime getCreation() {
        return creation;
    }
}
