TarvernNet es una red social temática inspirada en _Dungeons & Dragons_ (DnD).
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

Linux / Mac:

```bash
docker compose up
./gradlew bootRun
```

Windows:

```cmd
docker compose up
gradlew.bat bootRun
```

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

# Estilo del código

Normas generales:

- Código en inglés pero los comentarios en español, escritos apropiadamente con
  sus tildes y `ñ`s.

- Se usarán 4 espacios de indentación y codificación UTF-8 con terminaciones LF.
  Esto se puede comprobar con los siguiente comandos:
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

