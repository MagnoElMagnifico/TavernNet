import requests
from http import HTTPStatus
from .utils import SITE, RANDOM_WORDS, REFRESH_COOKIE
from .utils import User, LoginUser, random_text, check, is_valid_objectid, setup, end

random_post = lambda: {
    'title': random_text(RANDOM_WORDS, min=3, max=10),
    'content': random_text(RANDOM_WORDS, min=7, max=40)
}

def test_post_create(login: LoginUser):
    # CREAR UN POST
    print('\n==== POST CREATION ====')

    # Requiere autenticación
    r = requests.post(
        f'{SITE}/posts',
        json=random_post()
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # Campos invalidos
    r = requests.post(
        f'{SITE}/posts',
        json={'titulo': random_text(RANDOM_WORDS), 'otra cosa': 5},
        headers=login.header
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Esto deberia fallar porque por que no se ha activado un personaje
    r = requests.post(
        f'{SITE}/posts',
        json=random_post(),
        headers=login.header
    )
    check(r, HTTPStatus.BAD_REQUEST)

    # Activar el personaje y probar otra vez
    assert login.character is not None, 'User has no character??? Setup failed somehow'
    r = requests.post(
        f'{SITE}/auth/character-login',
        headers={'Authorization': 'Bearer ' + login.jwt},
        json={'character_id': login.character.id},
    )
    check(r, HTTPStatus.OK)
    jwt_with_char: str = r.json().get('access_token')
    login.refresh = r.cookies.get(REFRESH_COOKIE)
    print(f'JWT ({len(jwt_with_char)}): {jwt_with_char}')
    print('RefreshToken:', login.refresh)

    # Correcto
    for _ in range(10):
        r = requests.post(
            f'{SITE}/posts',
            json=random_post(),
            headers={'Authorization': 'Bearer ' + jwt_with_char},
        )
        check(r, HTTPStatus.CREATED)


def test_post_search(login: LoginUser):
    # VER/BUSCAR POSTS
    print('\n==== POST SEARCH ====')
    r = requests.get(
        f'{SITE}/posts&count=7',
        json=random_post(),
    )
    check(r, HTTPStatus.OK)

    json = r.json()
    # El número de elementos es el mismo que el solicitado
    assert len(json['page']) == 7, f'elements in the page is incorrect, got {len(json['page'])}, expected 7 or less'
    assert json['page_number'] == 0, f'got page_number {json['page_number']}, expected 0'

    # Verificar que los IDs están en hexadecimal
    assert all(is_valid_objectid(post.get('id')) for post in json), 'Found non-hex ID in response'

    # Verificar que el autor tiene campos utiles

    # TODO: verificar que el usuario actual no ha dado like

    # VER POST INDIVIDUAL
    print('\n==== POST  ====')
    # TODO: verificar que el usuario actual no ha dado like

    # CREAR UN COMENTARIO
    # TODO: verificar que los IDs están en hexadecimal
    # VER COMENTARIOS DE UN POST

    # DAR LIKE
    # TODO: verificar que los IDs están en hexadecimal
    # TODO: verificar que el usuario actual le ha dado like

    # QUITAR LIKE
    # BORRAR UN POST

def test_posts(login: LoginUser):
    print('\n==== POSTS ======================================================')
    test_post_create(login)
    test_post_search(login)


if __name__ == '__main__':
    user = setup()
    test_posts(user)
    end()

