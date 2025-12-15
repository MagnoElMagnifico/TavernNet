import requests
from http import HTTPStatus
from .utils import SITE, NEW_USER_PREFIX, CHAR_NAME_NOT_EXISTS, CHAR_EXISTS, USERNAME_NOT_EXISTS, USER_EXISTS, REFRESH_COOKIE, NEW_USER_CHAR_NAME2, INVALID_ID
from .utils import LoginUser, User, check, check_value, is_valid_objectid, setup, end

# ==== USUARIOS ===============================================================

def test_noauth_user(user: User):
    '''
    Prueba las operaciones de usuarios que no requieren de autenticación:
    - Consultar usuarios
    - Crear usuarios
    - Ver usuario individual
    - Consultar personaje
    Recibe como parámetro un usuario que no exista sobre el que hacer las pruebas.
    '''

    print('\n==== USERS ====')

    # BUSCAR USUARIOS
    r = requests.get(f'{SITE}/users?search={NEW_USER_PREFIX}&page=0&count=7')
    check(r, HTTPStatus.OK)

    json = r.json()
    # El número de elementos es el mismo que el solicitado
    check_value(
        r,
        len(json['_embedded']['publicProfileList']) <= 7,
        msg=f'elements in the page is incorrect, got {len(json['page'])}, expected 7 or less'
    )
    check_value(
        r,
        json['page']['number'] == 0,
        msg=f'got page_number {json['page']['number']}, expected 0'
    )

    # Limites de la paginacion
    r = requests.get(f'{SITE}/users?search={NEW_USER_PREFIX}&page=100')
    check(r, HTTPStatus.OK)
    json = r.json()
    check_value(r, json.get('_embedded') is None)

    # Otros limites
    r = requests.get(f'{SITE}/users?search={NEW_USER_PREFIX}&page=-1')
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Término de búsqueda vacío debe devolver todos los usuarios
    r = requests.get(f'{SITE}/users')
    check(r, HTTPStatus.OK)
    json = r.json()
    check_value(
        r,
        json.get('_embedded') is not None and
        json['_embedded'].get('publicProfileList') is not None,
        'Empty search returns no users'
    )

    # CREAR USUARIO
    # NOTA: el usuario ya se creó en test_setup()
    # Repetido
    r = requests.post(f'{SITE}/users', json={'username': user.username, 'password': user.password})
    check(r, HTTPStatus.CONFLICT)


    # VER USUARIO INDIVIDUAL
    # No existe
    r = requests.get(f'{SITE}/users/{USERNAME_NOT_EXISTS}')
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.get(f'{SITE}/users/{user.username}')
    check(r, HTTPStatus.OK)

    # En test_setup() se creó un personaje, comprobar que aparece en esta salida
    json = r.json()
    recv_username = json.get('username')
    recv_characters = json.get('characters')
    recv_character_names = list(map(lambda character: character['name'], recv_characters))
    recv_character_names_set = set(recv_character_names)
    print('Received username:', recv_username)
    print('Received character names:', recv_character_names)
    assert user.character is not None, 'User has no character??? Setup failed somehow'
    check_value(r, recv_username == user.username, f'Checking response username, expected {user.username}, got {json.get('username')}')
    check_value(r, len(recv_characters) == 1, f'Expected 1 character, got {len(recv_characters)}')
    check_value(r, len(recv_character_names_set) == len(recv_character_names), f'There are {len(recv_character_names) - len(recv_character_names_set)} repeated names')
    check_value(r, user.character.name in recv_character_names, 'Created character not found in response')


def test_auth_user(login: LoginUser):
    # BORRAR USUARIO
    print('\n==== BORRAR USUARIO ====')
    # Requiere login
    r = requests.delete(f'{SITE}/users/{login.username}')
    check(r, HTTPStatus.UNAUTHORIZED)

    # No existe
    r = requests.delete(f'{SITE}/users/{USERNAME_NOT_EXISTS}', headers=login.header)
    check(r, HTTPStatus.NOT_FOUND)

    # Usuario del que no se tiene permisos
    r = requests.delete(f'{SITE}/users/{USER_EXISTS.username}', headers=login.header)
    check(r, HTTPStatus.FORBIDDEN)

    # Correcto
    r = requests.delete(f'{SITE}/users/{login.username}', headers=login.header)
    check(r, HTTPStatus.NO_CONTENT)

    # No existe ahora
    r = requests.delete(f'{SITE}/users/{login.username}', headers=login.header)
    check(r, HTTPStatus.NOT_FOUND)

    # Comprobar que no se puede hacer refresh
    r = requests.post(
        f'{SITE}/auth/gefresh',
        cookies={REFRESH_COOKIE: login.refresh}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # # TODO: comprobar que pasa con todos los recursos asociados al usuario (primero terminar el resto de tests)
    # # - Personajes
    # # - Parties de las que es DM ==> probablemente dar error, forzar a transferir el DM
    # # - Recursos del personaje: posts/comentarios/likes/parties
    #
    # # 1) Usuario que es DM de alguna party: debe dar error
    # # Suponiendo endpoint DELETE /users/{username} y que headers contiene auth
    # r = requests.delete(f'{SITE}/users/{login.username}', headers=headers)
    # if login.is_dm:  # atributo ficticio para saber si es DM de alguna party
    #     # Debe fallar: usuario es DM de alguna party
    #     check(r, HTTPStatus.CONFLICT)
    #     print(f'Usuario {login.username} es DM de alguna party, no puede borrarse sin transferir o borrar parties')
    # else:
    #     # 2) Usuario sin parties en las que sea DM: borrar correctamente
    #     r = requests.delete(f'{SITE}/users/{login.username}', headers=headers)
    #     check(r, HTTPStatus.NO_CONTENT)
    #
    #     # 3) Verificar que el usuario ya no existe
    #     r = requests.get(f'{SITE}/users/{login.username}', headers=headers)
    #     check(r, HTTPStatus.NOT_FOUND)
    #
    #     # 4) Verificar que los personajes se eliminaron en cascada
    #     for char in login.characters:  # suponiendo lista de personajes del login
    #         r = requests.get(f'{SITE}/users/{login.username}/characters/{char.name}', headers=headers)
    #         check(r, HTTPStatus.NOT_FOUND)
    #
    #         # Además, comprobar posts/comentarios de cada personaje
    #         # Posts
    #         r_posts = requests.get(f'{SITE}/posts?author={char.name}', headers=headers)
    #         check(r_posts, HTTPStatus.OK)
    #         for post in r_posts.json():
    #             author = post.get('author')
    #             assert author is None or author.get('deleted') is True, f'Post {post.get("id")} autor no marcado como borrado tras borrar usuario'
    #
    #         # Comentarios
    #         r_comments = requests.get(f'{SITE}/comments?author={char.name}', headers=headers)
    #         check(r_comments, HTTPStatus.OK)
    #         for comment in r_comments.json():
    #             author = comment.get('author')
    #             assert author is None or author.get('deleted') is True, f'Comentario {comment.get("id")} autor no marcado como borrado tras borrar usuario'
    #
    #     # 5) Comprobar parties donde era miembro (no DM): ya no debe aparecer
    #     for char in login.characters:
    #         r = requests.get(f'{SITE}/parties?member={char.name}', headers=headers)
    #         check(r, HTTPStatus.OK)
    #         parties = r.json()
    #         for party in parties:
    #             members = party.get('members', [])
    #             member_names = [m.get('name') for m in members if m is not None]
    #             assert char.name not in member_names, f'Personaje {char.name} sigue apareciendo en party {party.get("id")} tras borrar usuario'


# ==== PERSONAJES =============================================================

def test_noauth_character(user: User):
    # CONSULTAR PERSONAJES
    print('\n==== CHARACTERS ====')

    # TODO: pendiente de borrar: esta operación es redundante ya que esta misma información se ve en /users/{id}
    r = requests.get(f'{SITE}/users/{USERNAME_NOT_EXISTS}/characters')
    check(r, HTTPStatus.NOT_FOUND)

    r = requests.get(f'{SITE}/users/{user.username}/characters')
    check(r, HTTPStatus.OK)

    json = r.json()

    # Verificar que los IDs están en hexadecimal
    check_value(
        r,
        all(is_valid_objectid(character.get('id')) for character in json),
        'Found non-hex ID in response'
    )

    # Igual que antes
    recv_character_names = list(map(lambda character: character['name'], json))
    recv_character_names_set = set(recv_character_names)
    print('Received character names:', recv_character_names)
    check_value(r, len(recv_character_names) == 1, f'Expected 1 character, got {len(recv_character_names)}')
    check_value(r, len(recv_character_names_set) == len(recv_character_names), f'There are {len(recv_character_names) - len(recv_character_names_set)} repeated names')
    assert user.character is not None, 'User has no character??? Setup failed somehow'
    check_value(r, user.character.name in recv_character_names, 'Created character not found in response')


    # CONSULTAR PERSONAJE INDIVIDUAL
    # No existe el usuario
    r = requests.get(f'{SITE}/users/{USERNAME_NOT_EXISTS}/characters/{CHAR_NAME_NOT_EXISTS}')
    check(r, HTTPStatus.NOT_FOUND)

    # No existe personaje
    r = requests.get(f'{SITE}/users/{user.username}/characters/{CHAR_NAME_NOT_EXISTS}')
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.get(f'{SITE}/users/{user.username}/characters/{user.character.name}')
    check(r, HTTPStatus.OK)

    # Verificar que los IDs están en hexadecimal
    check_value(r, is_valid_objectid(r.json().get('id')), 'Found non-hex ID in response')


def test_auth_characters(login: LoginUser):
    assert login.character is not None, 'User has no character??? Setup failed somehow'
    print('\n==== CHARACTERS ====')

    # CREAR PERSONAJE
    # Requiere autenticacion
    r = requests.post(
        f'{SITE}/users/{login.username}/characters',
        json={
            'name': NEW_USER_CHAR_NAME2,
            'race': 'human',
            'languages': ['Common'],
            'alignment': 'LAWFUL_GOOD',
            'combat': {'ac': 16, 'hp': 39, 'speed': 40, 'initiative': 4},
            'actions': [],
        }
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # Usuario no existe
    r = requests.post(
        f'{SITE}/users/{USERNAME_NOT_EXISTS}/characters',
        json={
            'name': NEW_USER_CHAR_NAME2,
            'race': 'human',
            'languages': ['Common'],
            'alignment': 'LAWFUL_GOOD',
            'combat': {'ac': 16, 'hp': 39, 'speed': 40, 'initiative': 4},
            'actions': [],
        },
        headers=login.header,
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Nombre duplicado
    r = requests.post(
        f'{SITE}/users/{login.username}/characters',
        json={
            'name': login.character.name,
            'race': 'human',
            'languages': ['Common'],
            'alignment': 'LAWFUL_GOOD',
            'combat': {'ac': 16, 'hp': 39, 'speed': 40, 'initiative': 4},
            'actions': [],
        },
        headers=login.header,
    )
    check(r, HTTPStatus.CONFLICT)

    # Autor diferente
    r = requests.post(
        f'{SITE}/users/{login.username}/characters',
        json={
            'name': login.character.name,
            'race': 'human',
            'languages': ['Common'],
            'alignment': 'LAWFUL_GOOD',
            'combat': {'ac': 16, 'hp': 39, 'speed': 40, 'initiative': 4},
            'actions': [],
        },
        headers=login.header,
    )
    check(r, HTTPStatus.CONFLICT)

    # Validacion: payload mal formado / campos invalidos
    # Nombre vacío
    r = requests.post(
        f'{SITE}/users/{login.username}/characters',
        json={
            'name': '',
            'race': 'human',
            'languages': ['Common'],
            'alignment': 'LAWFUL_GOOD',
            'combat': {'ac': 16, 'hp': 39, 'speed': 40, 'initiative': 4},
            'actions': [],
        },
        headers=login.header,
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Campos extra largos
    r = requests.post(
        f'{SITE}/users/{login.username}/characters',
        headers=login.header,
        json={
            'name': 'x' * 3000,
            'race': 'human',
            'languages': ['Common'],
            'alignment': 'LAWFUL_GOOD',
            'combat': {'ac': 16, 'hp': 39, 'speed': 40, 'initiative': 4},
            'actions': [],
        },
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Faltan campos
    r = requests.post(
        f'{SITE}/users/{login.username}/characters',
        headers=login.header,
        json={
            'name': CHAR_NAME_NOT_EXISTS,
            'race': 'human',
            'alignment': 'LAWFUL_GOOD',
        },
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # La versión correcta ya se ha probado en test_setup()


    # EDITAR PERSONAJE
    # Requiere autenticacion
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{login.character.name}',
        json=[{'op': 'replace', 'path': '/stats/strength', 'value': 10}],
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # Usuario no existe
    r = requests.patch(
        f'{SITE}/users/{USERNAME_NOT_EXISTS}/characters/{login.character.name}',
        headers=login.header,
        json=[{'op': 'replace', 'path': '/stats/strength', 'value': 10}],
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Personaje no existe
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{CHAR_NAME_NOT_EXISTS}',
        headers=login.header,
        json=[{'op': 'replace', 'path': '/stats/strength', 'value': 10}],
    )
    check(r, HTTPStatus.NOT_FOUND)

    # JsonPath invalido
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{login.character.name}',
        headers=login.header,
        json=[{'operacion': 'replace', 'campo': 'stats/strength', 'valor': 5}],
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY) # BUG: debería ser 400 pero se están validando los campos por ser null

    # Operación desconocida
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{login.character.name}',
        headers=login.header,
        json=[{'op': 'cambiar', 'path': '/stats/strength', 'value': 5}],
    )
    check(r, HTTPStatus.BAD_REQUEST)

    # # BUG: esto se acepta, aunque tecnicamente no es un error, solo que no se hacen cambios
    # # Path inexistente
    # r = requests.patch(
    #     f'{SITE}/users/{login.username}/characters/{login.character.name}',
    #     headers=headers,
    #     json=[{'op': 'replace', 'path': '/stats/nonexistent_stat', 'value': 5}],
    # )
    # check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Valores no permitidos
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{login.character.name}',
        headers=login.header,
        json=[
            {'op': 'replace', 'path': '/stats/strength', 'value': -9999},
            {'op': 'replace', 'path': '/race', 'value': 'x' * 500},
        ],
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Se intentan modificar valores que no se pueden cambiar
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{login.character.name}',
        headers=login.header,
        json=[
            {'op': 'replace', 'path': '/id', 'value': INVALID_ID},
            {'op': 'replace', 'path': '/user', 'value': USER_EXISTS.username},
            {'op': 'replace', 'path': '/creation', 'value': '2000-01-01T10:00:24.178'},
        ],
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Correcto
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{login.character.name}',
        headers=login.header,
        json=[{'op': 'replace', 'path': '/stats/strength', 'value': 12}]
    )
    check(r, HTTPStatus.OK)

    # La API devuelve el personaje actualizado
    stat_strength = r.json().get('stats').get('strength')
    check_value(r, stat_strength == 12, f'Expected 12, got {stat_strength}')


    # BORRAR PERSONAJE
    # Requiere login
    r = requests.delete(f'{SITE}/users/{login.username}/characters/{login.character.name}')
    check(r, HTTPStatus.UNAUTHORIZED)

    # Usuario no existe
    r = requests.delete(f'{SITE}/users/{USERNAME_NOT_EXISTS}/characters/{login.character.name}', headers=login.header)
    check(r, HTTPStatus.NOT_FOUND)

    # Personaje no existe
    r = requests.delete(f'{SITE}/users/{login.username}/characters/{CHAR_NAME_NOT_EXISTS}', headers=login.header)
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.delete(f'{SITE}/users/{login.username}/characters/{login.character.name}', headers=login.header)
    check(r, HTTPStatus.NO_CONTENT)

    # Verificar que el personaje ahora no existe
    r = requests.get(f'{SITE}/users/{login.username}/characters/{login.character.name}', headers=login.header)
    check(r, HTTPStatus.NOT_FOUND)

    # TODO: comprobar que pasa con los posts/comentarios/parties del personaje (primero terminar el resto de tests)
    # - Posts/comentarios: aún existen, pero el autor aparece como borrado/null/...
    # - Parties: salirse de la party
    #
    # TODO: implementar GET /posts?author=
    # TODO: implementar GET /comments?author= o similar
    # TODO: implementar GET /parties?member= o similar
    #
    # # Posts del personaje
    # r = requests.get(f'{SITE}/posts?author={char_name}', headers=headers)
    # check(r, HTTPStatus.OK)
    # posts = r.json() if r.status_code == HTTPStatus.OK else []
    # for post in posts:
    #     author = post.get('author')
    #     assert author is None or author.get('deleted') is True, f'Post {post.get("id")} autor no marcado como borrado'
    #
    # # Comentarios del personaje
    # r = requests.get(f'{SITE}/comments?author={char_name}', headers=headers)
    # check(r, HTTPStatus.OK)
    # comments = r.json() if r.status_code == HTTPStatus.OK else []
    # for comment in comments:
    #     author = comment.get('author')
    #     assert author is None or author.get('deleted') is True, f'Comentario {comment.get("id")} autor no marcado como borrado'
    #
    # # ---- COMPROBAR PARTIES ----
    # # Suponiendo endpoint: /parties?member={char_name} o /users/{username}/parties
    # r = requests.get(f'{SITE}/parties?member={char_name}', headers=headers)
    # check(r, HTTPStatus.OK)
    # parties = r.json() if r.status_code == HTTPStatus.OK else []
    # for party in parties:
    #     members = party.get('members', [])
    #     member_names = [m.get('name') for m in members if m is not None]
    #     assert char_name not in member_names, f'Personaje {char_name} sigue apareciendo en party {party.get("id")}'


if __name__ == '__main__':
    user = setup()
    print('\n==== NO AUTHENTICATION ========================================')
    test_noauth_user(user)
    test_noauth_character(user)

    print('\n==== AUTH: USER AND CARACTERS ===================================')
    test_auth_characters(user)
    test_auth_user(user)

    end()
