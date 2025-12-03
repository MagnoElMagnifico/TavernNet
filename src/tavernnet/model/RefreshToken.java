package tavernnet.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/** Representa un RefreshToken y otros datos relacionados que se necesite */
@RedisHash("refresh_token")
public record RefreshToken(
    @Id
    @NotBlank(message = "RefreshToken value must be not null or blank")
    String uuid,

    // NOTA: necesario para crear un objeto Authentication
    // Usuario al que corresponde este dato
    @NotBlank(message = "User must be not null or blank")
    String username,

    // NOTA: necesario para crear un objeto Authentication (lista de autoridades)
    // Rol del usuario: ADMIN o USER
    GlobalRole role,

    // Tiempo de vida de la entrada en segundos
    @Min(value = 1, message = "TTL minimum of 1")
    @TimeToLive
    long ttl
) {}
