import requests
import base64
import json
import uuid
from http import HTTPStatus
from .utils import SITE, INVALID_PASSWD, USERNAME_NOT_EXISTS, REFRESH_COOKIE, CHAR_EXISTS, CHAR_NAME_NOT_EXISTS, INVALID_ID, JWT_ACTIVE_CHAR, NEW_PASSWD
from .utils import User, LoginUser, check, setup, end


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

    return LoginUser(user.username, user.password, user.character, jwt, refresh_token, {'Authorization': 'Bearer ' + jwt})


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
    assert login.character is not None, 'User has no character??? Setup failed somehow'
    jwt_content = json.loads(base64.b64decode(login.jwt.split('.')[1] + '=='))
    print('JWT content', jwt_content)
    assert jwt_content[JWT_ACTIVE_CHAR] == login.character.id, 'JWT does not have active character'

    return login


def test_login_password_change(login: LoginUser) -> LoginUser:
    # CAMBIAR CONTRASEÑA
    print('\n==== PASSWORD CHANGE ====')
    # Usuario no existe
    r = requests.post(
        f'{SITE}/users/{USERNAME_NOT_EXISTS}/password',
        headers={'Authorization': 'Bearer ' + login.jwt},
        json={'current_password': login.password, 'new_password': NEW_PASSWD}
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Contraseña incorrecta
    r = requests.post(
        f'{SITE}/users/{login.username}/password',
        headers={'Authorization': 'Bearer ' + login.jwt},
        json={'current_password': INVALID_PASSWD, 'new_password': NEW_PASSWD}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # Correcto
    r = requests.post(
        f'{SITE}/users/{login.username}/password',
        headers={'Authorization': 'Bearer ' + login.jwt},
        json={'current_password': login.password, 'new_password': NEW_PASSWD}
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
        json={'username': login.username, 'password': login.password}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # La nueva funciona
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': login.username, 'password': NEW_PASSWD}
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


def test_login(user: User) -> None:
    print('\n==== LOGIN AND REFRESH TESTS ===================================')

    login = test_login_login(user)
    login = test_login_refresh(login)
    login = test_login_password_change(login)
    test_login_logout(login)


if __name__ == '__main__':
    user = setup()
    test_login(user)
    end()
