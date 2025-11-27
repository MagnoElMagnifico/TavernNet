package tavernnet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tavernnet.exception.InvalidRefreshTokenException;
import tavernnet.model.Ownable;
import tavernnet.model.Permission;
import tavernnet.model.RefreshToken;
import tavernnet.model.User;
import tavernnet.repository.RefreshTokenRepository;
import tavernnet.repository.RoleRepository;
import tavernnet.repository.UserRepository;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authMng;
    private final KeyPair keyPair;
    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final RoleRepository roleRepo;
    private final MongoTemplate mongo;

    /** Duracion del jwt (default: 15min) */
    @Value("${jwt.ttl:PT15M}")
    private Duration jwtTtl;
    /** Duracion del refresh jwt (default: 72h) */
    @Value("${jwt.ttl:PT72H}")
    private Duration refreshTtl;

    @Autowired
    public AuthService(
        MongoTemplate mongo,
        AuthenticationManager authMng,
        KeyPair keyPair,
        UserRepository userRepo,
        RefreshTokenRepository refreshRepo,
        RoleRepository roleRepo
    ) {
        this.mongo = mongo;
        this.authMng = authMng;
        this.keyPair = keyPair;
        this.userRepo = userRepo;
        this.refreshRepo = refreshRepo;
        this.roleRepo = roleRepo;
    }

    // La respuesta HTTP al usuario tendrá solo el JWT, pero también hay que
    // devolver al controlador el valor del refresh jwt para que configure la
    // cookie adecuadamente.
    public record LoginResponse (
        User.LoginResponse body,
        RefreshToken refreshToken
    ) {}

    /** Autentica un usuario a partir de sus credenciales */
    public LoginResponse login(User.LoginRequest user) {
        // Autenticar al usuario
        Authentication auth = authMng.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                user.username(),
                user.password()
            )
        );

        // Generar JWT y RefreshToken
        String token = generateJwt(auth);
        RefreshToken refreshToken = generateRefreshToken(auth);

        return new LoginResponse(
            new User.LoginResponse(token, jwtTtl),
            refreshToken
        );
    }

    /** Vuelve a autenticar un usuario a partir de su refresh token */
    public LoginResponse refresh(String refreshToken) throws InvalidRefreshTokenException {
        // Buscar en redis el username al que corresponde este jwt
        var token = refreshRepo
            .findById(refreshToken)
            .orElseThrow(() -> new InvalidRefreshTokenException(refreshToken));

        // Buscar ahora el usuario completo
        User user = userRepo
            .findByUsername(token.user())
            .orElseThrow(() -> new UsernameNotFoundException(token.user()));

        // Autenticar al usuario
        Authentication auth = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            user.getAuthorities()
        );

        // Generar JWT y RefreshToken
        String newJwt = generateJwt(auth);
        RefreshToken newRefreshToken = generateRefreshToken(auth);

        // Borrar la entrada anterior
        refreshRepo.deleteById(refreshToken);

        return new LoginResponse(
            new User.LoginResponse(newJwt, jwtTtl),
            newRefreshToken
        );
    }

    public void logout(String jwt) {
        Authentication auth = validateAuthHeader(jwt);
        if (auth.getPrincipal() == null) {
            throw new RuntimeException("Internal Error");
        }

        User user = (User) auth.getPrincipal();

        // Invalidar tokens del usuario
        refreshRepo.deleteAllByUser(user.getUsername());
    }

    /** Usado por el filtro para comprobar la <code>Authorization</code> cookie */
    public Authentication validateAuthHeader(String value) throws JwtException {
        // Eliminar la parte de Bearer
        String jwt = value.replaceFirst("^Bearer ", "");

        // Verificar la firma
        Claims claims = Jwts.parser()
            .verifyWith(keyPair.getPublic())
            .build()
            .parseSignedClaims(jwt)
            .getPayload();

        // Comprobar si el usuario existe
        String username = claims.getSubject();
        User user = userRepo
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("'%s' not found".formatted(username)));

        // Si ha salido bien, autorizar
        return UsernamePasswordAuthenticationToken.authenticated(username, jwt, user.getAuthorities());
    }

    // TODO: esto no
    public RoleHierarchy loadRoleHierarchy() {
        RoleHierarchyImpl.Builder builder = RoleHierarchyImpl.withRolePrefix("");

        roleRepo.findAll().forEach(role -> {
            if (!role.includes().isEmpty()) {
                builder.role("ROLE_" + role.roleName()).implies(
                    role
                        .includes()
                        .stream()
                        .map(i -> "ROLE_" + i.roleName())
                        .toArray(String[]::new)
                );
            }

            if (!role.permissions().isEmpty()) {
                builder.role("ROLE_" + role.roleName()).implies(
                    role
                        .permissions()
                        .stream()
                        .map(Permission::toString)
                        .toArray(String[]::new)
                );
            }
        });

        return builder.build();
    }

    // ==== VALIDACIÓN DE PERMISOS =============================================

    public boolean isOwner(String collection, String id, Authentication auth) {
        Ownable resource = mongo.findById(id, Ownable.class, collection);
        if (resource == null) {
            return false;
        }

        return resource.getOwnerId().equals(auth.getName());
    }

    // ==== GENERACIÓN DE TOKENS ===============================================

    private String generateJwt(Authentication auth) {
        List<String> roles = auth.getAuthorities()
            .stream()
            .filter(authority -> authority instanceof SimpleGrantedAuthority)
            .map(GrantedAuthority::getAuthority)
            .toList();

        return Jwts.builder()
            .subject(auth.getName())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(jwtTtl)))
            .notBefore(Date.from(Instant.now()))
            .claim("roles", roles)
            .signWith(keyPair.getPrivate(), Jwts.SIG.ES256)
            .compact();
    }

    private RefreshToken generateRefreshToken(Authentication auth) {
        // Generar jwt: en este caso es un UUID
        UUID uuid = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken(
            uuid.toString(),
            auth.getName(),
            refreshTtl.toSeconds()
        );

        // Almacenar jwt en redis, limpiando los anteriores.
        // Gracias a esto, es posible revocar tokens de usuarios, permitiendo
        // implementar logout (tanto por el usuario como si hay brechas de
        // seguridad)
        refreshRepo.deleteAllByUser(auth.getName());
        refreshRepo.save(refreshToken);

        return refreshToken;
    }
}
