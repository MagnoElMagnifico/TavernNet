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
NEW_PASSWD = 'new&secure_passd1234'

ADMIN_USER = User('marcos', '1234', None)
CHAR_EXISTS = Character('6926c3037385de4eb73a2e1d', 'Zarion')
USER_EXISTS = User('jeremias', 'password', CHAR_EXISTS)

USERNAME_NOT_EXISTS = 'user-not-found'
INVALID_PASSWD = 'invalid-password'
CHAR_NAME_NOT_EXISTS = 'some random character name'
INVALID_ID = '6926c3037385de4eb73a2e1f'


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
    new_password = 'nueva-contraseña1234'

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
        # TODO: completar con los campos necesarios
        json={'name': NEW_USER_CHAR_NAME, 'user': user.username},
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

    # CONSULTAR PERSONAJE
    print('\n==== CHARACTERS ====')
    # No existe el usuario
    r = requests.get(f'{SITE}/users/{USERNAME_NOT_EXISTS}/characters/{CHAR_NAME_NOT_EXISTS}')
    check(r, HTTPStatus.NOT_FOUND)

    # No existe personaje
    r = requests.get(f'{SITE}/users/{user.username}/characters/{CHAR_NAME_NOT_EXISTS}')
    check(r, HTTPStatus.NOT_FOUND)

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

    # Comprobar que el nuevo JWT contiene un campo con el usuario activo
    jwt_content = json.loads(base64.b64decode(jwt.split('.')[1] + '=='))
    print('JWT content', jwt_content)
    assert jwt_content[JWT_ACTIVE_CHAR] == user.character.id, 'JWT does not have active character'


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
        cookies={REFRESH_COOKIE: refresh_token}
    )
    check(r, HTTPStatus.OK)
    jwt = r.json().get('access_token')
    refresh_token = r.cookies.get(REFRESH_COOKIE)
    print(f'New JWT ({len(jwt)}): {jwt})')
    print('New refresh:', refresh_token)


    # CAMBIAR CONTRASEÑA
    print('\n==== PASSWORD CHANGE ====')
    # Usuario no existe
    r = requests.post(
        f'{SITE}/users/{USERNAME_NOT_EXISTS}/password',
        headers={'Authorization': 'Bearer ' + jwt},
        json={'current_password': user.password, 'new_password': NEW_PASSWD}
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Contraseña incorrecta
    r = requests.post(
        f'{SITE}/users/{user.username}/password',
        headers={'Authorization': 'Bearer ' + jwt},
        json={'current_password': INVALID_PASSWD, 'new_password': NEW_PASSWD}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # Correcto
    r = requests.post(
        f'{SITE}/users/{user.username}/password',
        headers={'Authorization': 'Bearer ' + jwt},
        json={'current_password': user.password, 'new_password': NEW_PASSWD}
    )
    check(r, HTTPStatus.NO_CONTENT)
    new_refresh_token = r.cookies.get(REFRESH_COOKIE)

    # Comprobar que el refresh token cambia
    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: refresh_token})
    check(r, HTTPStatus.UNAUTHORIZED)

    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: new_refresh_token})
    check(r, HTTPStatus.OK)
    jwt = r.json().get('access_token')
    refresh_token = r.cookies.get(REFRESH_COOKIE)
    print(f'New JWT ({len(jwt)}): {jwt})')
    print('New refresh:', refresh_token)

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
    jwt = r.json().get('access_token')
    new_refresh_token = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(jwt)}): {jwt}')
    print('RefreshToken:', new_refresh_token)

    # Hemos hecho un nuevo login sin hacer logout, probar que el refresh token
    # anterior no vale
    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: refresh_token})
    check(r, HTTPStatus.UNAUTHORIZED)

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


    # LOGOUT
    print('\n==== LOGOUT ====')
    # El usuario no habia iniciado sesión
    r = requests.post(f'{SITE}/auth/logout')
    check(r, HTTPStatus.UNAUTHORIZED)

    r = requests.post(f'{SITE}/auth/logout', headers={'Authorization': 'Bearer ' + jwt })
    check(r, HTTPStatus.NO_CONTENT)

    # Comprobar que el RefrestToken ya no es valido
    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: new_refresh_token})
    check(r, HTTPStatus.UNAUTHORIZED)


    # OTROS
    # - Probar cualquier operacion login con un JWT inválido


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

    return LoginUser(user.username, NEW_PASSWD, None, jwt, refresh_token)


# ==== PRUEBAS QUE REQUIEREN LOGIN ============================================

def test_auth(login: LoginUser):
    '''
    - Borrar personajes
    - Borrar usuario
    '''
    print('\n==== AUTHENTICATED TESTS =======================================')
    headers = {'Authorization': 'Bearer ' + login.jwt}

    # TODO: completar estos tests
    # CREAR PERSONAJE
    # - Usuario no existe
    # - Nombre duplicado
    # - Validacion
    # EDITAR PERSONAJE
    # - Usuario no existe
    # - Personaje no existe
    # - JsonPath invalido
    # - Path incorrecto
    # - Valor no permitido
    #
    # CREAR UN POST
    # BORRAR UN POST
    # CREAR UN COMENTARIO
    # DAR LIKE
    # QUITAR LIKE
    #
    # CREAR PARTY
    # AÑADIR MIEMBROS A LA PARTY
    # EDITAR PARTY
    # ...
    #
    # Probar que estas operaciones no se pueden hacer sobre recursos de los que
    # no son dueños o sin autenticar. Repetir estas operaciones como ADMIN y ver
    # que funcionan.

    # BORRAR PERSONAJE
    # - Usuario no existe
    # - Personaje no existe

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
        f'{SITE}/auth/refresh',
        cookies={REFRESH_COOKIE: login.refresh}
    )
    check(r, HTTPStatus.UNAUTHORIZED)


if __name__ == '__main__':
    # Crear usuario y personaje para las pruebas
    user = test_setup()

    test_noauth(user)
    login_user = test_login(user)
    test_auth(login_user)

    print('\n==== ALL TESTS PASSED ====')

