package tavernnet.exception;

public class InvalidCredentialsException extends Exception {
    public enum CredentialType {
        REFRESH_TOKEN,
        PASSWORD,
        JWT;

        @Override
        public String toString() {
            return switch (this) {
                case REFRESH_TOKEN -> "Refresh Token";
                case PASSWORD -> "Password";
                case JWT -> "JWT";
            };
        }
    }

    private final CredentialType type;
    private final String token;

    public InvalidCredentialsException(CredentialType type, String token) {
        super("Invalid %s: \"%s\"".formatted(type.toString(), token));
        this.token = token;
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public CredentialType getType() {
        return type;
    }
}
