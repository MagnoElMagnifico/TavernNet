package tavernnet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import tavernnet.utils.ValidObjectId;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;

@NullMarked
@Document(collection = "users")
public class User implements UserDetails, Ownable {

    // ==== REPRESENTACIONES INTERNAS ==========================================

    /** Creado a partir de un JWT */
    public record AuthUser (
        // `sub` / `subject` del usuario autenticado
        String username,

        @Nullable
        // Personaje activo que realiza la operación, `act_ch` en el JWT.
        // Puede ser null en algunos casos:
        //
        // - Si el usuario es ADMIN
        // - Crear/modificar/borrar personajes o cambiar el personaje seleccionado
        // - Cambiar la contraseña, cerrar sesión o borrar el usuario
        // - Administración de parties (si el usuario es DM)
        ObjectId activeCharacter,

        // Rol del usuario para generar la lista de authorities
        // `role` en el JWT
        GlobalRole role
    ) {
        public AuthUser(String username, GlobalRole role) {
            this(username, null, role);
        }

        public Authentication toAuth() {
            return UsernamePasswordAuthenticationToken.authenticated(
                this,
                null,
                role.asAuthorities()
            );
        }
    }

    // ==== DTOs: RESPONSES ====================================================

    /** DTO de respuesta al mostrar usuarios */
    public record PublicProfile(
        // TODO: profile picture
        @NotBlank String username,
        LocalDateTime creation,
        Collection<Character> characters
    ) {
        public PublicProfile(User user, Collection<Character> characters) {
            this(
                user.username,
                user.creation,
                characters
            );
        }
    }

    // TODO: version del public profile pero que use Character.Summary (para el listado de usuarios)

    /** DTO de lo que recibe el usuario tras iniciar sesion  */
    public record LoginResponse (
        @NotBlank(message = "Token must be not null or blank")
        @JsonProperty("access_token")
        String jwt,

        @NotBlank(message = "Type must be not null or blank")
        @JsonProperty("token_type")
        String type,

        @NotBlank(message = "Expiration must be not null")
        Duration expiresIn
    ) {}

    // ==== DTOs: LOGIN ========================================================

    /** DTO de lo que envia el usuario cuando quiere iniciar sesion  */
    public record LoginRequest (
        @NotBlank(message = "Username must be not null or blank")
        @Size(max = 64, message = "Username maximum length is 64 characters")
        String username,

        @NotBlank(message = "Username must be not null or blank")
        String password,

        // Posibilidad de iniciar sesion directamente con un personaje concreto
        @JsonProperty("active_character")
        @Nullable
        String activeCharacter
    ) {}

    public record LoginCharacterRequest (
        @JsonProperty("character_id")
        @ValidObjectId
        ObjectId characterId
    ) {}

    // Cuerpo de la petición de cambiar la contraseña
    public record PasswordChangeRequest(
        @NotBlank(message = "Current password must be not null or blank")
        @JsonProperty("current_password")
        String currentPassword,
        @NotBlank(message = "New password must be not null or blank")
        @JsonProperty("new_password")
        String newPassword
    ) {}

    // ==== USUARIO REAL =======================================================

    @Id
    @NotBlank(message = "Username must be not null or blank")
    @Size(max = 64, message = "Username maximum length is 64 characters")
    private final String username;

    /** <strong>IMPORTANTE</strong>: Debe usarse <code>PasswordEncoder</code>. */
    @NotBlank(message = "Password must be not null or blank")
    @Field(name = "password")
    @JsonIgnore
    private String passwordHash;

    @Valid private final GlobalRole role;
    @Valid private final LocalDateTime creation;

    // TODO: profile picture

    // ==== CONSTRUCTORES ======================================================

    public User(
        @NotBlank String username,
        @NotBlank String passwordHash,
        @Valid GlobalRole role,
        @Valid LocalDateTime creation
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.creation = creation;
    }

    // ==== GETTERS ============================================================

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public LocalDateTime getCreation() {
        return creation;
    }

    public GlobalRole getRole() {
        return role;
    }

    // ==== OTROS MÉTODOS ======================================================

    public void setPassword(@NotBlank String password, @Valid PasswordEncoder passwordEncoder) {
        passwordHash = Objects.requireNonNull(passwordEncoder.encode(password));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.asAuthorities();
    }

    @Override
    public String getOwnerId() {
        // El usuario es dueño de si mismo
        return username;
    }
}
