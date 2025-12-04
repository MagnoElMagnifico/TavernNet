package tavernnet.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

/** Para evitar que haya varios tokens del mismo usuario, se añaden estas
 * entradas para comprobar que token tiene cada usuario.
 * <br/>
 * Si esta entrada no existe, se puede crear un RefreshToken normal sin problemas.
 * Pero si esta existe, primero se debera borrar el anterior, almacenado aqui.
 */
@RedisHash("user_refresh_token")
public record UserRefreshToken (
    @Id
    @NotBlank(message = "User must be not null or blank")
    String username,

    @NotBlank(message = "RefreshToken value must be not null or blank")
    String uuid
) {}
