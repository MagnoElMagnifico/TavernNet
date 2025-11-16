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

Usuarios y personajes:

| Verbo    | URL                                           | Descripción                    | Autenticacion |
|----------|-----------------------------------------------|--------------------------------|---------------|
| `POST`   | `/users`                                      | Crear nuevo usuario            | No            |
| `GET`    | `/users/{userid}`                             | Consultar perfil de usuario    | No            |
| `DELETE` | `/users/{userid}`                             | Borrar usuario                 | Si            |
| `PATCH`  | `/users/{userid}`                             | Cambiar contraseña del usuario | Si            |
| `POST`   | `/users/{userid}/characters`                  | Crear personaje                | Si            |
| `GET`    | `/users/{userid}/characters/{character-name}` | Consultar stats de personaje   | No            |
| `PATCH`  | `/users/{userid}/characters/{character-name}` | Editar stats de personaje      | Si            |
| `DELETE` | `/users/{userid}/characters/{character-name}` | Borrar el personaje            | Si            |

Creación de posts:

| Verbo    | URL                        | Descripción                  | Autenticacion |
|----------|----------------------------|------------------------------|---------------|
| `GET`    | `/posts?for={characterid}` | Lista de últimos posts       | No            |
| `POST`   | `/posts`                   | Crear un post                | Si            |
| `GET`    | `/posts/{postid}`          | Consultar un post            | No            |
| `DELETE` | `/posts/{postid}`          | Borrar un post               | Si            |
| `POST`   | `/posts/{postid}/like`     | Dar un like a un post        | Si            |
| `DELETE` | `/posts/{postid}/like`     | Quitar un like a un post     | Si            |
| `GET`    | `/posts/{postid}/comments` | Obtener lista de comentarios | No            |
| `POST`   | `/posts/{postid}/comments` | Enviar comentario a un post  | Si            |

Mensajes:

| Verbo       | URL                                            | Descripción                                         | Autenticacion |
|-------------|------------------------------------------------|-----------------------------------------------------|---------------|
| `GET`       | `/messages?to={cid}`                           | Obtener chats del usuario `cid`                     | Si            |
| `GET`       | `/messages?to={cid1}&from={cid2}&since={date}` | Obtener chat entre `cid1` (logeado) y `cid2`        | Si            |
| `POST`      | `/messages`                                    | Enviar mensajes como `cid1` a `cid2` (en el cuerpo) | Si            |
| `POST`      | `/groups`                                      | Crear nuevo grupo                                   | Si            |
| `GET`       | `/groups?of={cid}`                             | Obtener grupos a los que pertenece `cid`            | Si            |
| `GET`       | `/groups/{groupid}`                            | Obtener mensajes del grupo                          | Si            |
| `POST`      | `/groups/{groupid}`                            | Enviar mensajes al grupo                            | Si            |
| `PUT/PATCH` | `/groups/{groupid}`                            | Editar/administrar grupo                            | Si            |
| `DELETE`    | `/groups/{groupid}`                            | Borrar grupo                                        | Si            |

NOTA: La notificación de nuevos mensajes requiere _pulling_.

TODO:

- Paginación de mensajes y comentarios
- Editar, responder y borrar mensajes/comentarios
- Tirar dados
- Borrar y editar comentarios
- Gremios y _parties_

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

