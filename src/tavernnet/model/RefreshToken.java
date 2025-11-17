package tavernnet.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@RedisHash
public record RefreshToken(
    @Id
    @NotBlank(message = "RefreshToken value must be not null or blank")
    String uuid,

    @NotBlank(message = "User must be not null or blank")
    String user,

    // Tiempo de vida en segundos
    @Min(value = 1, message = "TTL minimum of 1")
    @TimeToLive
    long ttl
) {}
