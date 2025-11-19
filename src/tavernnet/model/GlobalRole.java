package tavernnet.model;

import org.springframework.security.core.GrantedAuthority;

public enum GlobalRole implements GrantedAuthority {
    ADMIN, USER;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
