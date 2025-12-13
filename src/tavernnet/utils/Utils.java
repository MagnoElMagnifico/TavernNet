package tavernnet.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import tavernnet.exception.InvalidCredentialsException;
import tavernnet.model.User;

import java.net.URI;

public class Utils {
    public static URI getUrl(String methodName, Class<?> clazz, Object args) {
        return MvcUriComponentsBuilder.fromMethodName(clazz, methodName, args)
            .build()
            .toUri();
    }

    public static URI getUrl(String methodName, Class<?> clazz, Object arg1, Object arg2) {
        return MvcUriComponentsBuilder.fromMethodName(clazz, methodName, arg1, arg2)
            .build()
            .toUri();
    }

    public static User.AuthUser getAuthUser() throws InvalidCredentialsException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Esto no debería ejecutarse nunca si los métodos del controlador están
        // bien anotados con los permisos.
        if (auth == null || auth.getPrincipal() == null) {
            // Lanzar esta excepción para que el status sea 401 / 403
            throw new InvalidCredentialsException(InvalidCredentialsException.CredentialType.JWT, "<empty>");
        }

        return (User.AuthUser) auth.getPrincipal();
    }
}
