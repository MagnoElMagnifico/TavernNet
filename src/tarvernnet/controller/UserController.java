package tarvernnet;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tarvernnet.model.User;

@RestController
@RequestMapping("/users")
public class UserController {

    // Servicio para obtener todos los usuarios
    @GetMapping
    public List<User> getAllUsers() {
        // Lóxica para recuperar a lista de usuarios
        try {
            return ResponseEntity.ok(bookService.getBook(isbn));
        } catch (BookNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

    }

    // Servicio para obtener un usuario por ID
    @GetMapping("/{user-id}")
    public User getUserById(@PathVariable String id) {
        // Lóxica para atopar un usuario polo seu ID
        
    }
    
    // Servicio para crear un nuevo usuario
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User nuevoUser) {
        // Lóxica para gardar o novo usuario e devolver o estado de éxito
        
    }
}
