import random
import re
import requests
import string
from pathlib import Path
from dataclasses import dataclass
from http import HTTPStatus

# ==== TIPOS DE DATOS =========================================================

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
class LoginUser(User):
    jwt: str
    refresh: str
    header: dict[str, str]


# ==== CONSTANTES =============================================================

SITE = 'http://localhost:8080'
REFRESH_COOKIE = '__Secure-RefreshToken'
JWT_ACTIVE_CHAR = 'act_ch'


def _load_random_words(file: Path) -> list[str]:
    with open(file, 'r') as f:
        return [line.replace('\n', '') for line in f]

PACKAGE_PARENT = Path(__file__).resolve().parent
RANDOM_WORDS_FILE = PACKAGE_PARENT / 'words.txt'
RANDOM_WORDS = _load_random_words(RANDOM_WORDS_FILE)

NEW_USER_PREFIX = 'testuser'
NEW_USER_PASSW = 'passwd+1234'
NEW_USER_CHAR_NAME = 'Test Character'
NEW_USER_CHAR_NAME2 = 'Other Test Character'
NEW_PASSWD = 'new&secure_passd1234'

ADMIN_USER = User('marcos', '1234', None)
CHAR_EXISTS = Character('', 'Zarion') # No se sabe al inicio, se necesita mirar en la BD
USER_EXISTS = User('jeremias', 'password', CHAR_EXISTS)

USERNAME_NOT_EXISTS = 'user-not-found'
INVALID_PASSWD = 'invalid-password'
CHAR_NAME_NOT_EXISTS = 'some random character name'
INVALID_ID = '6926c3037385de4eb73a2e1f'

number_of_petitions = 0

# ==== FUNCIONES DE IMPRIMIR ==================================================

def _print_summary(r: requests.Response, msg: str | None = None, max_len = 200):
    # Acortar el cuerpo para ver solo la primera parte
    body = r.text[: min(max_len, len(r.text))]

    # Borrar saltos de línea y espacios innecesarios
    body = re.sub(r"\s{2,}|\n", "", body, flags=re.MULTILINE)

    msg = '' if msg is None else msg + ': '
    print(f'{msg}{r.request.method} {r.url} -> {r.status_code} {HTTPStatus(r.status_code).phrase} {body}')


def _print_response(response: requests.Response):
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


# ==== FUNCIONES DE VALIDACIÓN ================================================

def check(response: requests.Response, allowed: HTTPStatus, msg: str | None = None, quiet=False):
    global number_of_petitions
    number_of_petitions += 1

    if allowed.value == response.status_code:
        if not quiet:
            if response.status_code >= 400 and len(response.text) >= 1_000:
                _print_summary(
                    response,
                    msg='[WARN] response too long' + (': ' + msg if msg is not None else '')
                )
            else:
                _print_summary(response, msg=msg)
    else:
        print(f'\n==== PETITION ERROR: expected status {allowed} {allowed.phrase} ====')
        _print_response(response)
        assert False


def check_value(response: requests.Response, cond: bool, msg: str | None = None):
    if not cond:
        msg = '' if msg is None else ': ' + msg
        print(f'\n==== CHECK ERROR{msg} ====')
        _print_response(response)
        assert False


def is_valid_objectid(id):
    return (
        isinstance(id, str) and
        len(id) == 24 and
        all(c in string.hexdigits for c in id)
    )


# ==== ARCHIVO DE PALABRAS ====================================================

def random_text(words: list[str], mininum:int=3, maximum:int=40, max_char:None|int=None) -> str:
    n_words = random.randint(mininum, maximum)
    random_words = ' '.join(random.choice(words) for _ in range(n_words))

    # Cortar si se ha configurado
    if max_char is not None:
        random_words = random_words[: min(max_char, len(random_words))]

    return random_words


# ==== PREPARACIÓN DE LOS TESTS ===============================================

def setup() -> LoginUser:
    '''
    Crea un usuario y un personaje sobre el que poder ejecutar las operaciones.
    Realmente también prueba estas dos operaciones, pero no comprueba nada más.
    '''

    print("==== SETUP ====")
    user = LoginUser(f'{NEW_USER_PREFIX}{random.randint(0,999):03d}', NEW_USER_PASSW, None, '', '', {})
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

    user.jwt = json.get("access_token")
    user.refresh = r.cookies.get(REFRESH_COOKIE)
    user.header = {'Authorization': f'{json.get("token_type")} {user.jwt}'}

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
        headers=user.header
    )
    check(r, HTTPStatus.CREATED)

    # Intentar obtener el ID del personaje desde la cabecera Location
    char_id = r.headers.get('location').split('/')[-1]
    user.character = Character(char_id, NEW_USER_CHAR_NAME)

    # Buscar el ID de un personaje que exista
    global CHAR_EXISTS
    assert USER_EXISTS.character is not None
    assert USER_EXISTS.character.name == CHAR_EXISTS.name
    r = requests.get(f'{SITE}/users/{USER_EXISTS.username}/characters/{USER_EXISTS.character.name}')
    check(r, HTTPStatus.OK)
    CHAR_EXISTS.id = r.json().get('id')
    assert USER_EXISTS.character.id == CHAR_EXISTS.id

    return user


def end():
    print(f'\nALL TESTS PASSED: {number_of_petitions} petitions')
