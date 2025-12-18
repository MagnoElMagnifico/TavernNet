package tavernnet.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecuritySchemes({
    @SecurityScheme(type = SecuritySchemeType.HTTP, name = "jwt", in = SecuritySchemeIn.HEADER),
    @SecurityScheme(type = SecuritySchemeType.HTTP, name = "refresh", in = SecuritySchemeIn.HEADER)
})

@OpenAPIDefinition(
    info = @Info(
        title = "TavernNet",
        description = "REST API para red social inspirada en Dragones y Mazmorras",
        summary = """
                            TavernNet es una red social en la que los usuarios pueden
                            simular interacciones mediante sus personajes de Dragones
                            y Mazmorras. También se incluyen funcionalidades para
                            la realización de partidas online.
                        """,
        contact = @Contact(
            name = "Marcos Granja Grille, Jeremías Alvarenga Gamón",
            email = "marcos.granja@rai.usc.es , jeremiasezequiel.alvarenga@rai.usc.es"
            ),
        license=@License(name = "Apache 2.0"),
        version = "0.1.0"
    ),
    servers = {
        @Server(
            url = "localhost:8080",
            description = "API de desarrollo"
        )
    },
    security = {
        @SecurityRequirement(
            name = "jwt"
        ),
        @SecurityRequirement(
            name = "refresh"
        ),
    }
)

public class OASConfiguration {
}
