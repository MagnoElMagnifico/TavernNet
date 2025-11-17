package tavernnet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;

@Configuration
public class AuthenticationConfiguration {
    // Esto creo que lee los valores por defecto de resources/application.properties
    @Value("${keystore.location:classpath:keys.p12}")
    private String ksLocation;
    @Value("${keystore.private.name:jwt}")
    private String keyName;
    @Value("${keystore.password}")
    private String ksPassword;
    @Value("${keystore.private.password}")
    private String keyPassword;

    // Especificar que PasswordEncoder usar. Este se utiliza para evitar que las
    // contraseñas se almacenen en texto claro en la base de datos, por lo que
    // se usan algoritmos de hashing y salts para ocultarlas.
    //
    // En este caso, se deja que Spring decida el mejor algoritmo a usar; y
    // permite ser compatible con otros algoritmos previos que se hayan estado
    // usando.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // Configurar el PasswordEncoder para el AuthenticationManager
    @Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

    // Obtiene clave pública y privada del archivo keys.p12. Esto se usará para
    // cifrar los tokens JWT y validar que son correctos y, por tanto, la sesión
    // del usuario es legítima.
    @Bean
    public KeyPair jwtSignatureKeys() {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(ResourceUtils.getFile(ksLocation)), ksPassword.toCharArray());
            return new KeyPair(
                ks.getCertificate(keyName).getPublicKey(),
                (PrivateKey) ks.getKey(keyName, keyPassword.toCharArray())
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
