package tavernnet.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/** Representa un RefreshToken y otros datos relacionados que se necesite */
@RedisHash("refresh_token")
public record RefreshToken(
    @Id
    @NotBlank
    String uuid,

    // NOTA: necesario para crear un objeto Authentication
    // Usuario al que corresponde este dato
    @NotBlank
    String username,

    // NOTA: necesario para crear un objeto Authentication (lista de autoridades)
    // Rol del usuario: ADMIN o USER
    GlobalRole role,

    // NOTA: necesario para que al hacer refresh, se mantenga el mismo personaje activo
    @Nullable
    String activeCharacter,

    // Tiempo de vida de la entrada en segundos
    @Min(value = 1, message = "TTL minimum of 1")
    @TimeToLive
    long ttl
) {}
