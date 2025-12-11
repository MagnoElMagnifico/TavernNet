package tavernnet.service;

import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import tavernnet.exception.InvalidCredentialsException;
import tavernnet.exception.ResourceNotFoundException;
import tavernnet.model.*;
import tavernnet.model.Character;
import tavernnet.repository.CharacterRepository;
import tavernnet.repository.RefreshTokenRepository;
import tavernnet.repository.UserRefreshTokenRepository;
import tavernnet.repository.UserRepository;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@NullMarked
@Service("auth")
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String AUTH_TYPE = "Bearer";
    private static final String JWT_ROLE = "role";
    private static final String JWT_ACTIVE_CHARACTER = "act_ch";

    private final AuthenticationManager authMng;
    private final KeyPair keyPair;
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final UserRefreshTokenRepository userRefreshRepo;
    private final MongoTemplate mongo;
    private final CharacterRepository charRepo;

    // NOTA: estos no pueden ser null por el valor por defecto dado
    /** Duracion del jwt (default: 15min) */
    @Value("${jwt.ttl:PT15M}")
    private Duration jwtTtl;
    /** Duracion del refresh jwt (default: 72h) */
    @Value("${jwt.ttl:PT72H}")
    private Duration refreshTtl;

    @Autowired
    public AuthService(
        AuthenticationManager authMng,
        KeyPair keyPair,
        PasswordEncoder passwordEncoder,

        RefreshTokenRepository refreshRepo,
        UserRefreshTokenRepository userRefreshRepo,
        UserRepository userRepo,
        MongoTemplate mongo,
        CharacterRepository charRepo
    ) {
        this.authMng = authMng;
        this.keyPair = keyPair;
        this.passwordEncoder = passwordEncoder;

        this.refreshRepo = refreshRepo;
        this.userRefreshRepo = userRefreshRepo;
        this.userRepo = userRepo;
        this.mongo = mongo;
        this.charRepo = charRepo;
    }

    // ==== TIPOS DE DATOS NECESARIOS ==========================================

    // La respuesta HTTP al usuario tendrá solo el JWT, pero también hay que
    // devolver al controlador el valor del refresh jwt para que configure la
    // cookie adecuadamente.
    public record LoginResponse (
        User.LoginResponse body,
        RefreshToken refreshToken
    ) {}

    // ==== RESPUESTAS A LOS ENDPOINTS =========================================

    /** Autentica un usuario a partir de sus credenciales */
    public LoginResponse login(User.LoginRequest login) throws ResourceNotFoundException {
        log.debug("POST /auth/login for user=\"{}\"", login.username());

        // Autenticar al usuario
        // Primero se crea un objeto Authentication con la propiedad
        // authentication=false, hace que `isAuthenticated()` devuelva false.
        // Luego se le pasa al AuthenticationManager.
        // El manager irá a la BD con el metodo UserService.loadUserByUsername()
        // Comprueba su contraseña usando el PasswordManager
        // Crea un objeto Authentication y lo devuelve
        // (hace otras tareas a mayores y por eso se usa esta función)
        Authentication auth = authMng.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                login.username(),
                login.password()
            )
        );
        // Añadir esto al contexto, para que Spring sepa que ahora estamos autenticados
        // Principalmente para que la excepcion AccessDeniedException devuelva un
        // 401 en lugar de un 403, cosa que se deduce a partir de este contexto.
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("POST /auth/login successful for user=\"{}\"", login.username());

        // Como se está leyendo a traves de un UserService, el principal de este
        // objeto Authentication será un User (solo en este caso)
        User user = (User) auth.getPrincipal();

        // Esto no debería pasar nunca, pero por si acaso
        if (user == null) {
            throw new BadCredentialsException("unreachable error");
        }

        String activeChar = login.activeCharacter();
        String activeCharOid = null;
        if (activeChar != null) {
            // Si el formato del ID es valido, dar un error directamente
            if (!ObjectId.isValid(activeChar)) {
                throw new ResourceNotFoundException("Character", login.activeCharacter());
            }
            ObjectId oid = new ObjectId(activeChar);

            // Validar que el personaje existe
            Character character = charRepo.getCharacterById(oid);
            if (character == null) {
                throw new ResourceNotFoundException("Character", login.activeCharacter());
            }

            // Validar que el usuario tiene permisos
            if (user.getRole() != GlobalRole.ADMIN && !character.getOwnerId().equals(user.getUsername())) {
                throw new AccessDeniedException("User not owner of character " + character.getOwnerId());
            }

            activeCharOid = oid.toHexString();
        }

        // Generar JWT y RefreshToken
        String jwt = generateJwt(login.username(), user.getRole(), activeCharOid);
        RefreshToken refreshToken = generateRefreshToken(login.username(), user.getRole(), activeCharOid);

        log.debug("POST /auth/login created tokens JWT={} Refresh={}", jwt, refreshToken.uuid());

        return new LoginResponse(
            new User.LoginResponse(jwt, AUTH_TYPE, jwtTtl),
            refreshToken
        );
    }

    public LoginResponse loginCharacter(ObjectId characterId) throws ResourceNotFoundException, InvalidCredentialsException {
        User.AuthUser authUser = getAuthUser();
        log.debug("POST /auth/login-character for user=\"{}\"", authUser.username());

        // No es necesario verificar que el personaje existe porque ya se hace al validar los permisos
        /*if (!charRepo.existsById(characterId)) {
            throw new ResourceNotFoundException("Character", characterId.toHexString());
        }*/

        // Usando el JWT de la peticion, se cambia el personaje activo
        // Esto implica generar un nuevo JWT
        String jwt = generateJwt(authUser.username(), authUser.role(), characterId.toHexString());
        RefreshToken refreshToken = generateRefreshToken(authUser.username(), authUser.role(), characterId.toHexString());

        log.debug("POST /auth/login-character created tokens JWT={} Refresh={}", jwt, refreshToken.uuid());

        return new LoginResponse(
            new User.LoginResponse(jwt, AUTH_TYPE, jwtTtl),
            refreshToken
        );
    }

    /** Vuelve a autenticar un usuario a partir de su refresh token */
    public LoginResponse refresh(String refreshToken) throws InvalidCredentialsException {
        // Buscar en Redis el username al que corresponde este UUID
        RefreshToken token = refreshRepo
            .findById(refreshToken)
            .orElseThrow(() -> new InvalidCredentialsException(InvalidCredentialsException.CredentialType.REFRESH_TOKEN, refreshToken));

        log.debug("POST /auth/refresh found user=\"{}\" with Refresh=\"{}\" activeChar=\"{}\"", token.username(), refreshToken, token.activeCharacter());

        // NOTA: si los usuarios se borran, también se eliminan sus tokens de
        // Redis. En caso de querer implementar la posibilidad de banear cuentas,
        // se debería comprobar aquí.

        String newJwt = generateJwt(token.username(), token.role(), token.activeCharacter());
        RefreshToken newRefreshToken = generateRefreshToken(token.username(), token.role(), token.activeCharacter());

        log.debug("POST /auth/refresh new tokens JWT={} Refresh={}", newJwt, newRefreshToken.uuid());

        return new LoginResponse(
            new User.LoginResponse(newJwt, AUTH_TYPE, jwtTtl),
            newRefreshToken
        );
    }

    public void logout() throws InvalidCredentialsException {
        User.AuthUser user = getAuthUser();
        log.debug("POST /auth/logout user=\"{}\"", user.username());

        // Invalidar tokens del usuario
        deleteRefreshTokensByUsername(user.username());
    }

    // NOTA: se implementa aquí porque está más relacionado con la seguridad que
    // con la gestion (creación, lectura, borrado) de usuarios.
    public RefreshToken changePassword(
        String userId, User.PasswordChangeRequest request
    ) throws ResourceNotFoundException, InvalidCredentialsException {
        // Comprobar contraseña vieja
        User user = userRepo
            .findByUsername(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(InvalidCredentialsException.CredentialType.PASSWORD, request.currentPassword());
        }

        // Actualizar contraseña en la BD
        user.setPassword(request.newPassword(), passwordEncoder);
        userRepo.save(user);
        log.debug("POST /users/{}/password changed for user=\"{}\"", user.getUsername(), user.getUsername());

        // Rotar refresh tokens
        User.AuthUser authUser = getAuthUser();
        RefreshToken newRefreshToken = generateRefreshToken(
            user.getUsername(),
            user.getRole(),
            authUser.activeCharacter() == null ? null : authUser.activeCharacter().toHexString()
        );
        log.debug("POST /users/{}/password new Refresh={}", user.getUsername(), newRefreshToken.uuid());

        return newRefreshToken;
    }

    // ==== VALIDACIÓN =========================================================

    /** Usado por el filtro para comprobar la <code>Authorization</code> cookie */
    public Authentication validateAuthHeader(String value) throws JwtException {
        // Eliminar la parte de Bearer
        String jwt = value.replaceFirst("^Bearer ", "");

        // Verificar JWT
        Claims claims = Jwts.parser()
            .verifyWith(keyPair.getPublic())
            .build()
            .parseSignedClaims(jwt)
            .getPayload();

        // Extraer datos relevantes del JWT
        // En caso de que el ID del personaje activo sea incorrecto, dejar a null
        String activeCharId = claims.get(JWT_ACTIVE_CHARACTER, String.class);
        ObjectId activeCharIdOid = null;
        if (activeCharId != null) {
            if (ObjectId.isValid(activeCharId)) {
                activeCharIdOid = new ObjectId(activeCharId);
            } else {
                log.warn("Invalid ObjectId in active character JWT user={} charid={}", claims.getSubject(), activeCharId);
            }
        }

        User.AuthUser authUser = new User.AuthUser(
            claims.getSubject(),
            activeCharIdOid,
            GlobalRole.valueOf(claims.get(JWT_ROLE, String.class))
        );

        // NOTA: esto puede dar problemas si se borra el usuario, ya que el JWT
        // seguirá siendo válido. El JWT tiene una corta duración, por lo que no
        // es mucho problema, de lo contrario, habría que chequear aquí la BD.

        // Devolver objeto autenticado para poner en el contexto global
        return UsernamePasswordAuthenticationToken.authenticated(
            authUser,
            jwt,
            authUser.role().asAuthorities()
        );
    }

    // ==== VALIDACIÓN DE PERMISOS =============================================

    public boolean isUserOwner(String collection, String objectId, User.AuthUser user) throws ResourceNotFoundException {
        Ownable resource = mongo.findById(objectId, Ownable.class, collection);
        if (resource == null) {
            log.debug("Not found user owner of \"{}\" Collection=\"{}\"", objectId, collection);
            throw new ResourceNotFoundException(collection, objectId);
        }

        log.debug(
            "User owner of \"{}\" is \"{}\" AuthUser=\"{}\" Collection=\"{}\"",
            objectId, resource.getOwnerId(), user.username(), collection
        );
        return resource.getOwnerId().equals(user.username());
    }

    public boolean isCharOwner(String collection, String objectId, User.AuthUser user) throws ResourceNotFoundException {
        Ownable resource = mongo.findById(objectId, Ownable.class, collection);
        if (resource == null) {
            log.debug("Not found character owner of \"{}\" Collection=\"{}\"", objectId, collection);
            throw new ResourceNotFoundException(collection, objectId);
        }

        log.debug(
            "Character owner of \"{}\" is \"{}\" AuthUser=\"{}\" Collection=\"{}\"",
            objectId, resource.getOwnerId(), user.username(), collection
        );
        return resource.getOwnerId().equals(user.activeCharacter());
    }

    public boolean isCharacterOwnerByName(String userId, String characterName, User.AuthUser user) throws ResourceNotFoundException {
        if (!userRepo.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }

        Character c = charRepo.getCharacterByName(userId, characterName);
        if (c == null) {
            throw new ResourceNotFoundException("Character", characterName);
        }

        return c.getOwnerId().equals(user.username());
    }

    // ==== GENERACIÓN DE TOKENS ===============================================

    private String generateJwt(String username, GlobalRole role, @Nullable String activeCharacter) {
        log.debug("JWT user=\"{}\" role=\"{}\" activeChar=\"{}\"", username, role, activeCharacter);
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(jwtTtl)))
            .notBefore(Date.from(Instant.now()))
            .claim(JWT_ROLE, role.toString())
            .claim(JWT_ACTIVE_CHARACTER, activeCharacter)
            .signWith(keyPair.getPrivate(), Jwts.SIG.ES256)
            .compact();
    }

    private RefreshToken generateRefreshToken(String username, GlobalRole role, @Nullable String activeCharacter) {
        // Generar refresh token: en este caso es un UUID
        UUID uuid = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken(
            uuid.toString(),
            username,
            role,
            activeCharacter,
            refreshTtl.toSeconds()
        );

        UserRefreshToken userRefresh = new UserRefreshToken(
            username,
            refreshToken.uuid(),
            refreshToken.ttl()
        );

        // Almacenar refresh token en Redis, limpiando los anteriores.
        // Gracias a esto, es posible revocar tokens de usuarios, permitiendo
        // implementar logout (tanto por el usuario como si hay brechas de
        // seguridad)
        deleteRefreshTokensByUsername(username);
        refreshRepo.save(refreshToken);
        userRefreshRepo.save(userRefresh);

        log.debug("RefreshToken id=\"{}\" user=\"{}\" activeChar=\"{}\"", uuid, username, activeCharacter);
        return refreshToken;
    }

    // ==== FUNCIONES DE AYUDA =================================================

    private void deleteRefreshTokensByUsername(String username) {
        var optionalUrt = userRefreshRepo.findById(username);
        optionalUrt.ifPresent(urt -> {
            // Borrar tokens
            refreshRepo.deleteById(urt.uuid());
            // Borrar la entrada del usuario
            userRefreshRepo.deleteById(urt.username());
        });
    }

    private static User.AuthUser getAuthUser() throws InvalidCredentialsException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Esto no debería ejecutarse nunca si los métodos del controlador están
        // bien anotados con los permisos.
        if (auth == null || auth.getPrincipal() == null) {
            // Lanzar esta excepción para que el status sea 401
            throw new InvalidCredentialsException(InvalidCredentialsException.CredentialType.JWT, "<empty>");
        }

        return (User.AuthUser) auth.getPrincipal();
    }

}
