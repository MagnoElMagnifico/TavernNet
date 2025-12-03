package tavernnet.model;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public enum GlobalRole implements GrantedAuthority {
    // Usuarios con todos los permisos: pueden modificar recursos de otros
    // Por tanto, ADMIN incluye USER
    ADMIN,
    // Rol por defecto, solo puede modificar sus propios recursos
    // Todos los usuarios tienen este rol.
    USER;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }

    public Collection<? extends GrantedAuthority> asAuthorities() {
        return List.of(this);
    }
}
