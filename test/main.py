import requests
import uuid
import random
import re
import json
import base64
from dataclasses import dataclass
from http import HTTPStatus

'''
Script de pruebas para la API REST usando `requests`.

Ejecución: `python3 main.py` \\
Requisitos: requests

```bash
pip install requests
# or
python -m venv .venv
source .venv/bin/activate
pip install requests
```
'''

# ==== DEFINICIONES GLOBALES ==================================================

@dataclass
class Character:
    id: str
    name: str

@dataclass
class User:
    username: str
    password: str
    character: Character | None

@dataclass
class LoginUser:
    username: str
    password: str
    character: Character | None
    jwt: str
    refresh: str


SITE = 'http://localhost:8080'
REFRESH_COOKIE = '__Secure-RefreshToken'
JWT_ACTIVE_CHAR = 'act_ch'

NEW_USER_PREFIX = 'testuser'
NEW_USER_PASSW = 'passwd+1234'
NEW_USER_CHAR_NAME = 'Test Character'
NEW_USER_CHAR_NAME2 = 'Other Test Character'
NEW_PASSWD = 'new&secure_passd1234'

ADMIN_USER = User('marcos', '1234', None)
CHAR_EXISTS = Character('693a939bbbd81020255f01e3', 'Zarion')
USER_EXISTS = User('jeremias', 'password', CHAR_EXISTS)

USERNAME_NOT_EXISTS = 'user-not-found'
INVALID_PASSWD = 'invalid-password'
CHAR_NAME_NOT_EXISTS = 'some random character name'
INVALID_ID = '6926c3037385de4eb73a2e1f'


number_of_petitions = 0


# ==== FUNCIONES DE AYUDA =====================================================

def print_summary(r: requests.Response, msg: str | None = None, max_len = 150):
    # Acortar el cuerpo para ver solo la primera parte
    body = r.text[: min(max_len, len(r.text))]

    # Borrar saltos de línea y espacios innecesarios
    body = re.sub(r"\s{2,}|\n", "", body, flags=re.MULTILINE)

    msg = '' if msg is None else msg + ': '
    print(f'{msg}{r.request.method} {r.url} -> {r.status_code} {HTTPStatus(r.status_code).phrase} {body}')


def print_response(response: requests.Response):
    try:
        body = response.json()
    except Exception:
        body = response.text

    print('PETITION:', response.request.method, response.url)
    print(response.request.headers)
    print(response.request.body)
    print('RESPONSE:', response.status_code, HTTPStatus(response.status_code).phrase)
    print(response.headers)
    print(body)


def check(response: requests.Response, allowed: HTTPStatus, msg: str | None = None):
    global number_of_petitions
    number_of_petitions += 1

    if allowed.value == response.status_code:
        if response.status_code >= 400 and len(response.text) >= 1_000:
            print_summary(response, msg='WARN: response too long' + (': ' + msg if msg is not None else ''))
        else:
            print_summary(response, msg=msg)
    else:
        print(f'\n==== ERROR (unexpected status {allowed} {allowed.phrase}) ====')
        print_response(response)
        assert False


# ==== ESTABLECER ESTADO INICIAL ==============================================

def test_setup() -> User:
    '''
    Crea un usuario y un personaje sobre el que poder ejecutar las operaciones.
    Realmente también prueba estas dos operaciones, pero no comprueba nada más.
    '''

    print("==== SETUP ====")
    user = User(f'{NEW_USER_PREFIX}{random.randint(0,999):03d}', NEW_USER_PASSW, None)
    print(f'User to use: "{user.username}":"{user.password}"')

    # Crear usuario
    r = requests.post(f'{SITE}/users', json={'username': user.username, 'password': user.password})
    # Comprobar estado, hay posibilidades de que el nombre generado
    # aleatoriamente ya exista
    check(r, HTTPStatus.CREATED)

    # Autenticar
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': user.password}
    )
    check(r, HTTPStatus.OK)
    json = r.json()
    header = {'Authorization': f'{json.get("token_type")} {json.get("access_token")}'}

    # Crear personaje
    r = requests.post(
        f'{SITE}/users/{user.username}/characters',
        json={
            'name': NEW_USER_CHAR_NAME,
            'race': 'elf',
            'languages': ['Common', 'Elfic'],
            'alignment': 'CHAOTIC_NEUTRAL',
            'combat': {'ac': 15, 'hp': 29, 'speed': 30, 'initiative': 2},
            'actions': [],
        },
        headers=header
    )
    check(r, HTTPStatus.CREATED)

    # Intentar obtener el ID del personaje desde la cabecera Location
    char_id = r.headers.get('location').split('/')[-1]
    user.character = Character(char_id, NEW_USER_CHAR_NAME)
    return user


# ==== PRUEBAS NO AUTH ========================================================

def test_noauth(user: User):
    '''
    Probar todas las operaciones que no requieren autenticación.
    Recibe como parámetro un usuario que no exista sobre el que hacer las pruebas.
    '''

    print('\n==== NO AUTHENTICATION ========================================')
    test_noauth_user(user)
    test_noauth_post()
    test_noauth_party()


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
    assert len(json['page']) <= 7, f'elements in the page is incorrect, got {len(json['page'])}, expected 7 or less'
    assert json['page_number'] == 0, f'got page_number {json['page_number']}, expected 0'

    # Limites de la paginacion
    r = requests.get(f'{SITE}/users?search={NEW_USER_PREFIX}&page=100')
    check(r, HTTPStatus.OK)
    json = r.json()
    assert len(json['page']) == 0, f'page out of limit gives {len(json['page'])} elements'

    # Otros limites
    r = requests.get(f'{SITE}/users?search={NEW_USER_PREFIX}&page=-1')
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

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
    assert recv_username == user.username, f'Checking response username, expected {user.username}, got {json.get('username')}'
    assert len(recv_characters) == 1, f'Expected 1 character, got {len(recv_characters)}'
    assert len(recv_character_names_set) == len(recv_character_names), f'There are {len(recv_character_names) - len(recv_character_names_set)} repeated names'
    assert user.character.name in recv_character_names, 'Created character not found in response'


    # CONSULTAR PERSONAJES
    print('\n==== CHARACTERS ====')

    # TODO: pendiente de borrar: esta operación es redundante ya que esta misma información se ve en /users/{id}
    r = requests.get(f'{SITE}/users/{USERNAME_NOT_EXISTS}/characters')
    check(r, HTTPStatus.NOT_FOUND)

    r = requests.get(f'{SITE}/users/{user.username}/characters')
    check(r, HTTPStatus.OK)

    # Igual que antes
    recv_character_names = list(map(lambda character: character['name'], r.json()))
    recv_character_names_set = set(recv_character_names)
    print('Received character names:', recv_character_names)
    assert len(recv_characters) == 1, f'Expected 1 character, got {len(recv_characters)}'
    assert len(recv_character_names_set) == len(recv_character_names), f'There are {len(recv_character_names) - len(recv_character_names_set)} repeated names'
    assert user.character.name in recv_character_names, 'Created character not found in response'


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


def test_noauth_post():
    print('\n==== POSTS ====')
    # TODO: completar estos tests
    # VER/BUSCAR POSTS
    # VER POST INDIVIDUAL
    # VER COMENTARIOS DE UN POST
    pass


def test_noauth_party():
    print('\n==== PARTIES ====')
    # TODO: completar estos tests
    # VER/BUSCAR PARTIES
    # VER PARTY INDIVIDUAL
    pass

# ==== PRUEBAS LOGIN ==========================================================

def test_login(user: User) -> LoginUser:
    '''
    - Login
    - Refresh
    - Cambiar contraseña
    - Cambiar de personaje activo (debe probar después de crear un personaje)
    - Logout

    Recibe como parámetros unas credenciales válidas y devuelve un usuario
    autenticado para continuar las pruebas.
    '''

    print('\n==== LOGIN AND REFRESH TESTS ===================================')

    login = test_login_login(user)
    login = test_login_refresh(login)
    login = test_login_password_change(login)
    test_login_logout(login)


    # LOGIN PARA EL RESTO DE PRUEBAS
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': NEW_PASSWD}
    )
    check(r, HTTPStatus.OK)
    jwt = r.json().get('access_token')
    refresh_token = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(jwt)}): {jwt}')
    print('RefreshToken:', refresh_token)

    return LoginUser(user.username, NEW_PASSWD, user.character, jwt, refresh_token)


def test_login_login(user: User):
    # INICIAR SESIÓN
    print('\n==== LOGIN ====')
    # No existe el usuario
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': USERNAME_NOT_EXISTS, 'password': INVALID_PASSWD}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # Contraseña incorrecta
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': INVALID_PASSWD}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # Correcto
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': user.password}
    )
    check(r, HTTPStatus.OK)
    jwt: str = r.json().get('access_token')
    refresh_token: str = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(jwt)}): {jwt}')
    print('RefreshToken:', refresh_token)

    # Si se especifica un personaje, que ese no exista
    r = requests.post(
        f'{SITE}/auth/login',
        json={
            'username': user.username,
            'password': user.password,
            'active_character': CHAR_NAME_NOT_EXISTS,
        }
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Si se especifica un personaje, que este sea de otro usuario
    r = requests.post(
        f'{SITE}/auth/login',
        json={
            'username': user.username,
            'password': user.password,
            'active_character': CHAR_EXISTS.id
        }
    )
    check(r, HTTPStatus.FORBIDDEN)

    # Correcto con un personaje
    assert user.character is not None, 'User has no character??? Setup failed somehow'
    r = requests.post(
        f'{SITE}/auth/login',
        json={
            'username': user.username,
            'password': user.password,
            'active_character': user.character.id
        }
    )
    check(r, HTTPStatus.OK)
    jwt: str = r.json().get('access_token')
    refresh_token: str = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(jwt)}): {jwt}')
    print('RefreshToken:', refresh_token)

    # Comprobar que el nuevo JWT contiene un campo con el personaje activo
    jwt_content = json.loads(base64.b64decode(jwt.split('.')[1] + '=='))
    print('JWT content', jwt_content)
    assert jwt_content[JWT_ACTIVE_CHAR] == user.character.id, 'JWT does not have active character'


    # CAMBIAR PERSONAJE ACTIVO
    print('\n==== CHANGE ACTIVE CHARACTER ====')
    # El personaje no existe
    r = requests.post(
        f'{SITE}/auth/character-login',
        headers={'Authorization': 'Bearer ' + jwt},
        json={'character_id': INVALID_ID},
    )
    check(r, HTTPStatus.NOT_FOUND)

    # No soy dueño del personaje
    r = requests.post(
        f'{SITE}/auth/character-login',
        headers={'Authorization': 'Bearer ' + jwt},
        json={'character_id': CHAR_EXISTS.id},
    )
    check(r, HTTPStatus.FORBIDDEN)

    # Correcto
    r = requests.post(
        f'{SITE}/auth/character-login',
        headers={'Authorization': 'Bearer ' + jwt},
        json={'character_id': user.character.id},
    )
    check(r, HTTPStatus.OK)
    jwt: str = r.json().get('access_token')
    refresh_token: str = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(jwt)}): {jwt}')
    print('RefreshToken:', refresh_token)

    # Comprobar que el nuevo JWT contiene un campo con el personaje activo
    jwt_content = json.loads(base64.b64decode(jwt.split('.')[1] + '=='))
    print('JWT content', jwt_content)
    assert jwt_content[JWT_ACTIVE_CHAR] == user.character.id, 'JWT does not have active character'

    return LoginUser(user.username, user.password, user.character, jwt, refresh_token)


def test_login_refresh(login: LoginUser) -> LoginUser:
    # REFRESH
    print('\n==== REFRESH ====')
    # Token inválido
    r = requests.post(
        f'{SITE}/auth/refresh',
        cookies={REFRESH_COOKIE: str(uuid.uuid4())}
    )
    check(r, HTTPStatus.UNAUTHORIZED)


    # Correcto
    r = requests.post(
        f'{SITE}/auth/refresh',
        cookies={REFRESH_COOKIE: login.refresh}
    )
    check(r, HTTPStatus.OK)
    login.jwt = r.json().get('access_token')
    login.refresh = r.cookies.get(REFRESH_COOKIE)
    print(f'New JWT ({len(login.jwt)}): {login.jwt})')
    print('New refresh:', login.refresh)

    # Comprobar que mantiene la selección de personaje anterior
    assert user.character is not None, 'User has no character??? Setup failed somehow'
    jwt_content = json.loads(base64.b64decode(login.jwt.split('.')[1] + '=='))
    print('JWT content', jwt_content)
    assert jwt_content[JWT_ACTIVE_CHAR] == user.character.id, 'JWT does not have active character'

    return login


def test_login_password_change(login: LoginUser) -> LoginUser:
    # CAMBIAR CONTRASEÑA
    print('\n==== PASSWORD CHANGE ====')
    # Usuario no existe
    r = requests.post(
        f'{SITE}/users/{USERNAME_NOT_EXISTS}/password',
        headers={'Authorization': 'Bearer ' + login.jwt},
        json={'current_password': user.password, 'new_password': NEW_PASSWD}
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Contraseña incorrecta
    r = requests.post(
        f'{SITE}/users/{user.username}/password',
        headers={'Authorization': 'Bearer ' + login.jwt},
        json={'current_password': INVALID_PASSWD, 'new_password': NEW_PASSWD}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # Correcto
    r = requests.post(
        f'{SITE}/users/{user.username}/password',
        headers={'Authorization': 'Bearer ' + login.jwt},
        json={'current_password': user.password, 'new_password': NEW_PASSWD}
    )
    check(r, HTTPStatus.NO_CONTENT)
    new_refresh_token = r.cookies.get(REFRESH_COOKIE)

    # Comprobar que el refresh token cambia
    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: login.refresh})
    check(r, HTTPStatus.UNAUTHORIZED)

    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: new_refresh_token})
    check(r, HTTPStatus.OK)
    login.jwt = r.json().get('access_token')
    old_refresh_token = r.cookies.get(REFRESH_COOKIE)
    print(f'New JWT ({len(login.jwt)}): {login.jwt})')
    print('New refresh:', old_refresh_token)

    # Probar a loguearse otra vez y que la contraseña antigua falla
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': user.password}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # La nueva funciona
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': NEW_PASSWD}
    )
    check(r, HTTPStatus.OK)
    login.jwt = r.json().get('access_token')
    login.refresh = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(login.jwt)}): {login.jwt}')
    print('RefreshToken:', login.refresh)

    # Hemos hecho un nuevo login sin hacer logout, probar que el refresh token
    # anterior no vale
    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: old_refresh_token})
    check(r, HTTPStatus.UNAUTHORIZED)

    return login


def test_login_logout(login: LoginUser):
    # LOGOUT
    print('\n==== LOGOUT ====')
    # El usuario no habia iniciado sesión
    r = requests.post(f'{SITE}/auth/logout')
    check(r, HTTPStatus.UNAUTHORIZED)

    r = requests.post(f'{SITE}/auth/logout', headers={'Authorization': 'Bearer ' + login.jwt })
    check(r, HTTPStatus.NO_CONTENT)

    # Comprobar que el RefrestToken ya no es valido
    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: login.refresh})
    check(r, HTTPStatus.UNAUTHORIZED)


# ==== PRUEBAS QUE REQUIEREN LOGIN ============================================

def test_auth(login: LoginUser):
    print('\n==== AUTHENTICATED TESTS =======================================')
    headers = {'Authorization': 'Bearer ' + login.jwt}

    test_auth_posts(login, headers)
    test_auth_parties(login, headers)
    test_auth_characters(login, headers)
    test_auth_user(login, headers)

    # TODO: ADMIN
    # Probar que estas operaciones no se pueden hacer sobre recursos de los que
    # no son dueños o sin autenticar. Repetir estas operaciones como ADMIN y ver
    # que funcionan.


def test_auth_posts(login: LoginUser, headers: dict[str,str]):
    # TODO: completar estos tests
    # CREAR UN POST
    # BORRAR UN POST
    # CREAR UN COMENTARIO
    # DAR LIKE
    # QUITAR LIKE
    pass


def test_auth_parties(login: LoginUser, headers: dict[str,str]):
    # TODO: completar estos tests
    # CREAR PARTY
    # AÑADIR MIEMBROS A LA PARTY
    # EDITAR PARTY
    # ...
    pass


def test_auth_characters(login: LoginUser, headers: dict[str,str]):
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
        headers=headers,
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
        headers=headers,
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
        headers=headers,
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
        headers=headers,
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Campos extra largos
    r = requests.post(
        f'{SITE}/users/{login.username}/characters',
        headers=headers,
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
        headers=headers,
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
        headers=headers,
        json=[{'op': 'replace', 'path': '/stats/strength', 'value': 10}],
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Personaje no existe
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{CHAR_NAME_NOT_EXISTS}',
        headers=headers,
        json=[{'op': 'replace', 'path': '/stats/strength', 'value': 10}],
    )
    check(r, HTTPStatus.NOT_FOUND)

    # JsonPath invalido
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{login.character.name}',
        headers=headers,
        json=[{'operacion': 'replace', 'campo': 'stats/strength', 'valor': 5}],
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY) # TODO: debería ser 400 pero se están validando los campos por ser null

    # Operación desconocida
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{login.character.name}',
        headers=headers,
        json=[{'op': 'cambiar', 'path': '/stats/strength', 'value': 5}],
    )
    check(r, HTTPStatus.BAD_REQUEST)

    # # TODO: esto se acepta, aunque tecnicamente no es un error, solo que no se hacen cambios
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
        headers=headers,
        json=[
            {'op': 'replace', 'path': '/stats/strength', 'value': -9999},
            {'op': 'replace', 'path': '/race', 'value': 'x' * 500},
        ],
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Se intentan modificar valores que no se pueden cambiar
    r = requests.patch(
        f'{SITE}/users/{login.username}/characters/{login.character.name}',
        headers=headers,
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
        headers=headers,
        json=[{'op': 'replace', 'path': '/stats/strength', 'value': 12}]
    )
    check(r, HTTPStatus.OK)

    # La API devuelve el personaje actualizado
    stat_strength = r.json().get('stats').get('strength')
    assert stat_strength == 12, f'Expected 12, got {stat_strength}'


    # BORRAR PERSONAJE
    # Requiere login
    r = requests.delete(f'{SITE}/users/{login.username}/characters/{login.character.name}')
    check(r, HTTPStatus.UNAUTHORIZED)

    # Usuario no existe
    r = requests.delete(f'{SITE}/users/{USERNAME_NOT_EXISTS}/characters/{login.character.name}', headers=headers)
    check(r, HTTPStatus.NOT_FOUND)

    # Personaje no existe
    r = requests.delete(f'{SITE}/users/{login.username}/characters/{CHAR_NAME_NOT_EXISTS}', headers=headers)
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.delete(f'{SITE}/users/{login.username}/characters/{login.character.name}', headers=headers)
    check(r, HTTPStatus.NO_CONTENT)

    # Verificar que el personaje ahora no existe
    r = requests.get(f'{SITE}/users/{login.username}/characters/{login.character.name}', headers=headers)
    check(r, HTTPStatus.NOT_FOUND)

    # TODO: comprobar que pasa con los posts/comentarios/parties del personaje
    # - Posts/comentarios: aún existen, pero el autor aparece como borrado/null/...
    # - Parties: salirse de la party


def test_auth_user(login: LoginUser, headers: dict[str,str]):
    # BORRAR USUARIO
    print('\n==== BORRAR USUARIO ====')
    # Requiere login
    r = requests.delete(f'{SITE}/users/{login.username}')
    check(r, HTTPStatus.UNAUTHORIZED)

    # No existe
    r = requests.delete(f'{SITE}/users/{USERNAME_NOT_EXISTS}', headers=headers)
    check(r, HTTPStatus.NOT_FOUND)

    # Usuario del que no se tiene permisos
    r = requests.delete(f'{SITE}/users/{USER_EXISTS.username}', headers=headers)
    check(r, HTTPStatus.FORBIDDEN)

    # Correcto
    r = requests.delete(f'{SITE}/users/{login.username}', headers=headers)
    check(r, HTTPStatus.NO_CONTENT)

    # No existe ahora
    r = requests.delete(f'{SITE}/users/{login.username}', headers=headers)
    check(r, HTTPStatus.NOT_FOUND)

    # Comprobar que no se puede hacer refresh
    r = requests.post(
        f'{SITE}/auth/gefresh',
        cookies={REFRESH_COOKIE: login.refresh}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # TODO: comprobar que pasa con todos los recursos asociados al usuario
    # - Personajes
    # - Parties de las que es DM ==> probablemente dar error, forzar a transferir el DM
    # - Recursos del personaje: posts/comentarios/likes/parties


if __name__ == '__main__':
    # Crear usuario y personaje para las pruebas
    user = test_setup()

    test_noauth(user)
    login_user = test_login(user)
    test_auth(login_user)

    print(f'\nALL TESTS PASSED: {number_of_petitions} petitions')

