TavernNet es una red social temática inspirada en _Dungeons & Dragons_ (DnD).
En ella, los perfiles de los usuarios se representan como fichas de personaje,
con atributos, clases y estadísticas. Los usuarios podrán:

- Crear y gestionar varios personajes.
- Crear publicaciones de texto o imagen en un feed público. Otros usuarios
  podrán dejar comentarios e impresiones.
- Seguir a otros usuarios y formar grupos de aventuras o unirse a gremios.
- Enviar mensajes directos a otros usuarios o grupos de chat (mesas virtuales)
  donde los usuarios podrán jugar a las campañas. Por tanto, podrán tirar dados
  en función de sus estadísticas de personaje.

> [!NOTE]
> No pretendemos implementar todas estas funcionalidades, solo aquellas partes
> que se correspondan mejor con lo se vea en la materia.

# Ejecutar

Primero se debe generar un certificado válido para firmar tokens de
autenticación JWT, que use curva elíptica:

```bash
# Generar clave privada ECC
openssl ecparam -name prime256v1 -genkey -noout -out jwt_ec.key.pem

# Generar certificado autofirmado (válido 10 años)
openssl req -new -x509 -key jwt_ec.key.pem -sha256 -days 3650 -subj "/CN=jwt" -out jwt_ec.crt.pem

# Empaquetar en un p12
openssl pkcs12 -export \
  -inkey jwt_ec.key.pem \
  -in jwt_ec.crt.pem \
  -name jwt \
  -out keys.p12 \
  -passout pass:XXXX \
  -keypbe AES-256-CBC \
  -certpbe AES-256-CBC \
  -macalg sha256 \
  -maciter \
  -iter 10000
```

Guarda el archivo `keys.p12` generado en `resources` con ese mismo nombre, y
luego configura las contraseñas como variables del entorno en un archivo `.env`.

Luego, para ejecutar una versión de desarrollo (con _live-reloading_) ejecuta el
script `./rundev.sh` y usa `./gradlew build` para recompilar.

Luego, para subir algunos datos de prueba a la base de datos:

```bash
docker cp db_script.js tavernnet-database-1:/tmp
docker exec tavernnet-database-1 mongosh /tmp/db_script.js
```

Para la versión de producción, usa el `.jar` generado.

# Características

- [ ] Perfiles de usuario con fichas de personaje
- [ ] Una misma cuenta puede tener varios personajes

Publicaciones:

- [ ] Texto e imágenes
- [ ] Comentarios
- [ ] Generar _feed_ para el usuario

Interacciones:

- [ ] Amistades entre usuarios
- [ ] Unirse a gremios
- [ ] Creación de _parties_

Mensajes:

- [ ] Mensajes directos entre personajes
- [ ] Creación de grupos de chat
- [ ] Mecánica para tirar dados con las _stats_ del personaje
- [ ] Encuentros/combates en el chat (tomar turnos por orden de iniciativa;
    movimiento dentro de un mapa, acción y _bonus action_)

<!-- TODO: Cómo integrar el Dungeon Master? -->

# Diseño de la API

Usuarios y autenticacion:

| Verbo    | URL                                 | Descripción                                    | Autenticacion |
|----------|-------------------------------------|------------------------------------------------|---------------|
| `GET`    | `/users?search=xxx&page=0&count=10` | Buscar por nombre de usuario                   | No            |
| `POST`   | `/users`                            | Crear nuevo usuario                            | *No*          |
| `GET`    | `/users/{userid}`                   | Consultar perfil de usuario                    | No            |
| `DELETE` | `/users/{userid}`                   | Borrar usuario                                 | Si            |
| `PATCH`  | `/users/{userid}`                   | Cambiar contraseña del usuario                 | Si            |
| `POST`   | `/auth/login`                       | Iniciar sesion                                 | *No*          |
| `POST`   | `/auth/logout`                      | Cierra sesion (ADMIN puede sobre otro usuario) | Si            |
| `POST`   | `/auth/refresh`                     | Genera un nuevo token sin contraseña           | Si            |

Personajes:

| Verbo    | URL                                           | Descripción                    | Autenticacion |
|----------|-----------------------------------------------|--------------------------------|---------------|
| `POST`   | `/users/{userid}/characters`                  | Crear personaje                | Si            |
| `GET`    | `/users/{userid}/characters/{character-name}` | Consultar stats de personaje   | No            |
| `PATCH`  | `/users/{userid}/characters/{character-name}` | Editar stats de personaje      | Si            |
| `DELETE` | `/users/{userid}/characters/{character-name}` | Borrar el personaje            | Si            |

Creación de posts:

| Verbo    | URL                                        | Descripción                  | Autenticacion |
|----------|--------------------------------------------|------------------------------|---------------|
| `GET`    | `/posts?page=0&count=10`                   | Lista de últimos posts       | No            |
| `POST`   | `/posts`                                   | Crear un post                | Si            |
| `GET`    | `/posts/{postid}`                          | Consultar un post            | No            |
| `DELETE` | `/posts/{postid}`                          | Borrar un post               | Si            |
| `POST`   | `/posts/{postid}/like`                     | Dar un like a un post        | Si            |
| `DELETE` | `/posts/{postid}/like`                     | Quitar un like a un post     | Si            |
| `GET`    | `/posts/{postid}/comments?page=0&count=10` | Obtener lista de comentarios | No            |
| `POST`   | `/posts/{postid}/comments`                 | Enviar comentario a un post  | Si            |

Mensajes:

| Verbo    | URL                                         | Descripción                            | Autenticacion     |
|----------|---------------------------------------------|----------------------------------------|-------------------|
| `GET`    | `/parties?search=xxx&page=0&count=10`       | Buscar _parties_ existentes            | No                |
| `POST`   | `/parties`                                  | Crear una nueva _party_                | Si                |
| `GET`    | `/parties/{party-id}`                       | Obtener miembros de la _party_ y DM    | No                |
| `POST`   | `/parties/{party-id}`                       | Añadir/quitar miembros de la _party_   | Si (DM)           |
| `PATCH`  | `/parties/{party-id}`                       | Administrar _party_                    | Si (DM)           |
| `DELETE` | `/parties/{party-id}`                       | Borrar _party_                         | Si (DM)           |
| `DELETE` | `/parties/{party-id}?member={character-id}` | Borrar miembro de la _party_           | Si (DM)           |
| `GET`    | `/parties/{party-id}/messages`              | Obtener ultimos mensajes de la _party_ | *Si* (Miembro/DM) |
| `POST`   | `/parties/{party-id}/messages`              | Enviar mensajes                        | Si (Miembro/DM)   |

NOTA: La notificación de nuevos mensajes requiere _pulling_.

# Diseño de la base de datos

<!-- https://www.w3schools.com/mongodb/mongodb_schema_validation.php -->
<!-- https://json-schema.org/learn/miscellaneous-examples -->

![](TavernNet.png)

# Estilo del código

Normas generales:

- Código en inglés pero los comentarios en español, escritos apropiadamente con
  sus tildes y `ñ`s.

- Se usarán 4 espacios de indentación y codificación UTF-8 con terminaciones LF.
  Esto se puede comprobar con los siguientes comandos:
  ```bash
  fd -tf -H -E .git -x file
  rg '\t'
  ```

- `PascalCase` para nombres de clases, `camelCase` para métodos y variables,
  `UPPER_SNAKE_CASE` para constantes, `alllowercase` para los paquetes; según
  dicta la convención en Java.

- Restringir la longitud de las líneas a 80 caracteres. Si se supera, la línea
  se separará en varias indentadas, cada una iniciando por un operador o después
  de una coma. Se permiten excepciones puntuales (_imports_, _strings_ o código
  alineado).

- Se usarán comentarios separadores para organizar el código en secciones
  temáticas.

Estilo general y espaciado:

- Las llaves de bucles y condicionales irán en la misma línea, según dicta la
  convención en Java.
- Espacios entre cada operador, palabra reservada y antes de cada llave (excepto
  puntos, comas y paréntesis).
- Evitar muchos niveles de anidamiento, usar _early return_ cuando sea posible.
- Evitar bucles y condicionales sin llaves de una única línea, usar al menos 2.
- Evitar espacios innecesarios: al final de línea, líneas con solo espacios,
  espacios duplicados, etc; incluyendo líneas en blanco.

