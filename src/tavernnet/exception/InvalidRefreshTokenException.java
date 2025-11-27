package tavernnet.exception;

import javax.naming.AuthenticationException;

public class InvalidRefreshTokenException extends AuthenticationException {
    private final String token;

    public InvalidRefreshTokenException(String token) {
        super("Invalid refresh jwt: \"%s\"".formatted(token));
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
