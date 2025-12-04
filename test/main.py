import requests
import uuid
import random
import re
from collections import namedtuple
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

SITE = 'http://localhost:8080'
REFRESH_COOKIE = '__Secure-RefreshToken'

User = namedtuple('User', ['username', 'password'])
LoginUser = namedtuple('User', ['username', 'password', 'jwt', 'refresh'])

# ==== FUNCIONES DE AYUDA =====================================================

def check(response: requests.Response, allowed: HTTPStatus):
    if allowed.value == response.status_code:
        if response.status_code >= 400 and len(response.text) >= 1_000:
            print_summary(response, msg='WARN: response too long')
        return

    print(f'\n==== ERROR (unexpected status {allowed} {allowed.phrase}) ====')
    print_response(response)
    raise SystemExit(1)


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


# ==== PRUEBAS NO AUTH ========================================================

def test_noauth(user: User):
    '''
    Probar todas las operaciones que no requieren autenticación.
    Recibe como parámetro un usuario que no exista sobre el que hacer las pruebas.
    '''

    print('\n==== SIN AUTENTICAR ============================================')
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

    print('\n==== USUARIOS ====')

    # BUSCAR USUARIOS
    r = requests.get(f'{SITE}/users?search=test&page=0&count=7')
    check(r, HTTPStatus.OK)
    print_summary(r, msg='Users list paginated')

    json = r.json()
    # El número de elementos es el mismo que el solicitado
    assert len(json['page']) <= 7, f'elements in the page is incorrect, got {len(json['page'])}, expected 7 or less'
    assert json['page_number'] == 0, f'got page_number {json['page_number']}, expected 0'

    # Limites de la paginacion
    r = requests.get(f'{SITE}/users?search=test&page=100')
    check(r, HTTPStatus.OK)
    print_summary(r, msg='Users list pagination out of bounds')
    json = r.json()
    assert len(json['page']) == 0, f'page out of limit gives {len(json['page'])} elements'

    # Otros limites
    r = requests.get(f'{SITE}/users?search=test&page=-1')
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)
    print_summary(r, msg='Invalid params to pagination')


    # CREAR USUARIO
    # Correcto
    r = requests.post(f'{SITE}/users', json={'username': user.username, 'password': user.password})
    check(r, HTTPStatus.CREATED)
    print_summary(r, msg=f'Created user {user.username}')

    # Repetido
    r = requests.post(f'{SITE}/users', json={'username': user.username, 'password': user.password})
    check(r, HTTPStatus.CONFLICT)
    print_summary(r, msg=f'User {user.username} already exists')

    # VER USUARIO INDIVIDUAL
    # Correcto
    r = requests.get(f'{SITE}/users/{user.username}')
    check(r, HTTPStatus.OK)
    print_summary(r, 'Get user OK')
    # TODO: Incluye sus personajes
    # TODO: No existe

    # TODO: CONSULTAR PERSONAJE
    # - No existe personaje
    # - No existe el usuario


def test_noauth_post():
    print('\n==== POSTS ====')
    # VER/BUSCAR POSTS
    # VER POST INDIVIDUAL
    # VER COMENTARIOS DE UN POST
    pass


def test_noauth_party():
    print('\n==== PARTIES ====')
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

    print('\n==== PRUEBAS DE AUTENTICACIÓN ==================================')

    # INICIAR SESIÓN
    print('\n==== LOGIN ====')
    # No existe el usuario
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': 'usuario_que_no_existe', 'password': 'contraseña'}
    )
    check(r, HTTPStatus.UNAUTHORIZED)
    print_summary(r, msg='User does not exist')

    # Contraseña incorrecta
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': 'invalid-password'}
    )
    check(r, HTTPStatus.UNAUTHORIZED)
    print_summary(r, msg='Invalid password')

    # Si se especifica un personaje, que ese no exista
    r = requests.post(
        f'{SITE}/auth/login',
        json={
            'username': user.username,
            'password': user.password,
            'active_character': 'personaje-no-existente'
        }
    )
    check(r, HTTPStatus.NOT_FOUND)
    print_summary(r, msg='Character not found')

    # TODO: login con personaje que funcione

    # Correcto
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': user.password}
    )
    check(r, HTTPStatus.OK)
    print_summary(r)
    jwt = r.json().get('access_token')
    refresh_token = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(jwt)}): {jwt}')
    print('RefreshToken:', refresh_token)


    # REFRESH
    print('\n==== REFRESH ====')
    # Token inválido
    r = requests.post(
        f'{SITE}/auth/refresh',
        cookies={REFRESH_COOKIE: str(uuid.uuid4())}
    )
    print_summary(r, msg='Invalid refresh token')

    # Correcto
    r = requests.post(
        f'{SITE}/auth/refresh',
        cookies={REFRESH_COOKIE: refresh_token}
    )
    print_summary(r)
    jwt2 = r.json().get('access_token')
    refresh_token2 = r.cookies.get(REFRESH_COOKIE)
    print(f'New JWT ({len(jwt2)}): {jwt2})')
    print('New refresh:', refresh_token2)


    # CAMBIAR CONTRASEÑA
    print('\n==== CAMBIAR CONTRASEÑA ====')
    new_password = 'new&secure_passd1234'
    # Usuario no existe
    r = requests.post(
        f'{SITE}/users/user-not-exists/password',
        headers={'Authorization': 'Bearer ' + jwt2},
        json={'current_password': user.password, 'new_password': new_password}
    )
    # No es 404 porque cuando se intentan validar los permisos, no se encuentra
    # el usuario.
    check(r, HTTPStatus.FORBIDDEN)
    print_summary(r, msg='User not found')

    # Contraseña incorrecta
    r = requests.post(
        f'{SITE}/users/{user.username}/password',
        headers={'Authorization': 'Bearer ' + jwt2},
        json={'current_password': 'invalid-password', 'new_password': new_password}
    )
    check(r, HTTPStatus.UNAUTHORIZED)
    print_summary(r, msg='Invalid password')

    # Correcto
    r = requests.post(
        f'{SITE}/users/{user.username}/password',
        headers={'Authorization': 'Bearer ' + jwt2},
        json={'current_password': user.password, 'new_password': new_password}
    )
    check(r, HTTPStatus.NO_CONTENT)
    print_summary(r, msg='Password change OK')
    refresh_token3 = r.cookies.get(REFRESH_COOKIE)

    # Comprobar que el refresh token cambia
    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: refresh_token2})
    check(r, HTTPStatus.UNAUTHORIZED)
    print_summary(r, msg='Password changes RefreshToken')

    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: refresh_token3})
    check(r, HTTPStatus.OK)
    print_summary(r, msg='Password changes RefreshToken')
    jwt4 = r.json().get('access_token')
    refresh_token4 = r.cookies.get(REFRESH_COOKIE)
    print(f'New JWT ({len(jwt4)}): {jwt4})')
    print('New refresh:', refresh_token4)

    # Probar a loguearse otra vez y que la contraseña antigua falla
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': user.password}
    )
    check(r, HTTPStatus.UNAUTHORIZED)
    print_summary(r, msg='Out-of-date password')

    # La nueva funciona
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': new_password}
    )
    check(r, HTTPStatus.OK)
    print_summary(r)
    jwt5 = r.json().get('access_token')
    refresh_token5 = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(jwt5)}): {jwt5}')
    print('RefreshToken:', refresh_token5)

    # Hemos hecho un nuevo login sin hacer logout, probar que el refresh token
    # anterior no vale
    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: refresh_token4})
    check(r, HTTPStatus.UNAUTHORIZED)
    print_summary(r, msg='New login changes RefreshToken')

    # CAMBIAR PERSONAJE ACTIVO
    # - El personaje no existe
    print('\n==== CAMBIAR PERSONAJE ACTIVO ====')
    # TODO: hay que crear un personaje aquí

    # LOGOUT
    print('\n==== LOGOUT ====')
    # El usuario no habia iniciado sesión
    r = requests.post(f'{SITE}/auth/logout')
    check(r, HTTPStatus.UNAUTHORIZED)
    print_summary(r, msg='Logout with no session')

    r = requests.post(f'{SITE}/auth/logout', headers={'Authorization': 'Bearer ' + jwt5 })
    check(r, HTTPStatus.NO_CONTENT)
    print_summary(r, msg='Logout OK')

    # Comprobar que el RefrestToken ya no es valido
    r = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: refresh_token5})
    check(r, HTTPStatus.UNAUTHORIZED)
    print_summary(r, msg='Logout from previous user')


    # OTROS
    # - Probar cualquier operacion login con un JWT inválido


    # LOGIN PARA EL RESTO DE PRUEBAS
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': user.username, 'password': new_password}
    )
    check(r, HTTPStatus.OK)
    print_summary(r)
    jwt = r.json().get('access_token')
    refresh_token = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(jwt)}): {jwt}')
    print('RefreshToken:', refresh_token)

    return LoginUser(user.username, new_password, jwt, refresh_token)

# ==== PRUEBAS QUE REQUIEREN LOGIN ============================================

def test_auth(login: LoginUser):
    print('\n==== PRUEBAS DE QUE REQUIEREN LOGIN ============================')
    # TODO: operaciones login
    # BORRAR USUARIO
    # - No existe
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
    # BORRAR PERSONAJE
    # - Usuario no existe
    # - Personaje no existe
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

    # Intentar borrar sin autenticar
    # r = requests.delete(f'{SITE}/users/{user.username}')
    # check(r, HTTPStatus.UNAUTHORIZED)
    # print('Verified delete without auth is UNAUTHORIZED')
    pass


'''
def test_auth_user():
    Probar endpoints de usuarios, autenticado como usuario

    # Volvemos a logear para obtener un JWT válido para borrar el usuario
    jwt_for_delete, refresh_for_delete, _ = login(username, new_password)
    print('Re-login to get credentials for delete')

    # Probar a borrar otro usuario (comprobación de permisos) -- si existe 'other' intenta borrar
    other = 'other'
    print('\n-- Permisos: intentar borrar otro usuario (debe DENEGARSE) --')
    r = requests.delete(f'{SITE}/users/{other}', headers={'Authorization': 'Bearer ' + jwt_for_delete})
    if r.status_code in (HTTPStatus.FORBIDDEN, HTTPStatus.UNAUTHORIZED, HTTPStatus.NOT_FOUND):
        print('Attempt to delete other user correctly denied or not found (status', r.status_code, ')')
    else:
        print('Unexpected status when deleting other user:', r.status_code)
        pprint_resp(r)

    # Borrar el usuario propio
    print('\n==== BORRAR USUARIO PROPIO ====')
    r = requests.delete(f'{SITE}/users/{username}', headers={'Authorization': 'Bearer ' + jwt_for_delete})
    check(r, HTTPStatus.NO_CONTENT)
    print('User deleted')

    # Tras borrar el usuario, el refresh token asociado debería dejar de funcionar
    print('\n-- Comprobar refresh tras borrar usuario (debe fallar) --')
    # usamos el refresh_for_delete que obtuvimos antes
    r_after_del = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: refresh_for_delete})
    if r_after_del.status_code == HTTPStatus.OK:
        print('ERROR: refresh token sigue válido tras borrar usuario (posible bug)')
        pprint_resp(r_after_del)
        raise SystemExit(1)
    else:
        print('Refresh rejected after user deletion as expected (status', r_after_del.status_code, ')')

    # Y el JWT viejo tampoco debe permitir acciones autenticadas
    print('\n-- Comprobar uso de JWT tras borrar usuario (debe fallar) --')
    r_jwt_after_del = requests.get(f'{SITE}/users/{username}', headers={'Authorization': 'Bearer ' + jwt_for_delete})
    if r_jwt_after_del.status_code in (HTTPStatus.UNAUTHORIZED, HTTPStatus.FORBIDDEN, HTTPStatus.NOT_FOUND):
        print('JWT rejected after deletion as expected (status', r_jwt_after_del.status_code, ')')
    else:
        print('ERROR: JWT todavía válido tras borrar usuario (status', r_jwt_after_del.status_code, ')')
        pprint_resp(r_jwt_after_del)
        raise SystemExit(1)
'''

if __name__ == '__main__':
    user = User(f'testuser{random.randint(0,999):03d}', 'passwd+1234')
    new_password = 'nueva-contraseña1234'

    print(f'User to use: "{user.username}":"{user.password}"')
    test_noauth(user)
    login_user = test_login(user)
    test_auth(login_user)

    print('\n==== ALL TESTS PASSED ====')

