import requests
from http import HTTPStatus
from .utils import SITE, RANDOM_WORDS, REFRESH_COOKIE, INVALID_ID, USER_EXISTS
from .utils import User, LoginUser, random_text, check, check_value, is_valid_objectid, setup, end

random_post = lambda: {
    'title': random_text(RANDOM_WORDS, mininum=3, maximum=10, max_char=64),
    'content': random_text(RANDOM_WORDS, mininum=7, maximum=40)
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
    check(r, HTTPStatus.FORBIDDEN)

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

    return jwt_with_char


def test_post_search(login: LoginUser, jwt_with_char: str):
    # VER/BUSCAR POSTS
    print('\n==== POST SEARCH ====')

    # Limites de la paginacion
    r = requests.get(f'{SITE}/posts?page=100000')
    check(r, HTTPStatus.OK)
    json = r.json()
    check_value(r, json.get('_embedded') is None)

    # Otros limites
    r = requests.get(f'{SITE}/posts?page=-1')
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Correcto
    r = requests.get(f'{SITE}/posts?count=7')
    check(r, HTTPStatus.OK)
    json = r.json()
    data = json.get('_embedded')
    check_value(r, data is not None)
    data = data['postViewList']

    # El número de elementos es el mismo que el solicitado
    check_value(
        r,
        len(data) <= 7,
        msg=f'elements in the page is incorrect, got {len(data)}, expected 7 or less'
    )
    check_value(
        r,
        json['page']['size'] == 7,
        msg=f'expected count to be 7, got {json['page']['size']}'
    )
    check_value(
        r,
        json['page']['number'] == 0,
        msg=f'got page_number {json['page']['number']}, expected 0'
    )

    # Verificar que los IDs están en hexadecimal
    check_value(r, all(is_valid_objectid(post['id']) for post in data))

    # Verificar que, al no estar registrados, no se da informacion sobre si se
    # ha dado like
    check_value(r, all(post.get('liked') is None for post in data))


    # Verificar que el autor tiene campos utiles
    assert login.character is not None, 'User has no character??? Setup failed somehow'
    r = requests.get(
        f'{SITE}/posts?author={login.character.id}',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.OK)
    json = r.json()
    data = json['_embedded']['postViewList']

    # El autor debe ser igual al solicitado
    check_value(r, all(post['author']['id'] == login.character.id for post in data))
    check_value(r, all(post['author']['name'] == login.character.name for post in data))
    check_value(r, all(post['author']['user'] == login.username for post in data))

    # Como el usuario no ha dado like, se reporta como tal
    check_value(r, all(post.get('liked') == False for post in data))

    # Obtener un post para la siguiente prueba
    post_id = data[0]['id']


    # VER POST INDIVIDUAL
    print('\n==== POST ====')

    # No se encuentra
    r = requests.get(f'{SITE}/posts/{INVALID_ID}')
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.get(f'{SITE}/posts/{post_id}')
    check(r, HTTPStatus.OK)
    json = r.json()

    # Mismas verificaciones que antes
    check_value(r, is_valid_objectid(json['id']), msg='Found non-hex ID in response')
    check_value(r, json.get('liked') is None)
    check_value(r, json['author']['id'] == login.character.id)
    check_value(r, json['author']['name'] == login.character.name)
    check_value(r, json['author']['user'] == login.username)

    # Comprobar ahora que al iniciar sesión, no ha dado like
    r = requests.get(
        f'{SITE}/posts/{post_id}',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.OK)
    check_value(r, r.json().get('liked') == False)
    return post_id


def test_post_comments(login: LoginUser, jwt_with_char, post_id):
    print('\n==== COMMENTS ====')
    # CREAR UN COMENTARIO
    # Requiere autenticación
    r = requests.post(
        f'{SITE}/posts/{post_id}/comments',
        json={'content': random_text(RANDOM_WORDS)}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # Debe de haber un personaje activo
    r = requests.post(
        f'{SITE}/posts/{post_id}/comments',
        headers=login.header,
        json={'content': random_text(RANDOM_WORDS)}
    )
    check(r, HTTPStatus.FORBIDDEN)

    # No existe
    r = requests.post(
        f'{SITE}/posts/{INVALID_ID}/comments',
        headers={'Authorization': 'Bearer ' + jwt_with_char},
        json={'content': random_text(RANDOM_WORDS)}
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Campo incorrecto
    r = requests.post(
        f'{SITE}/posts/{post_id}/comments',
        headers={'Authorization': 'Bearer ' + jwt_with_char},
        json={'contenido': random_text(RANDOM_WORDS)}
    )
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Correcto
    for _ in range(10):
        r = requests.post(
            f'{SITE}/posts/{post_id}/comments',
            headers={'Authorization': 'Bearer ' + jwt_with_char},
            json={'content': random_text(RANDOM_WORDS)}
        )
        check(r, HTTPStatus.CREATED)


    # VER COMENTARIOS DE UN POST
    # Limites de la paginacion
    r = requests.get(f'{SITE}/posts/{post_id}/comments?page=100000')
    check(r, HTTPStatus.OK)
    json = r.json()
    check_value(r, json.get('_embedded') is None)

    # Otros limites: count -1
    r = requests.get(f'{SITE}/posts/{post_id}/comments?count=-1')
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Correcto
    r = requests.get(f'{SITE}/posts/{post_id}/comments?count=3&page=2')
    check(r, HTTPStatus.OK)
    json = r.json()
    data = json.get('_embedded')
    check_value(r, data is not None)
    data = data['commentList']

    # Comprobar que el número de elementos es correcto
    check_value(
        r,
        len(data) <= 3,
        msg=f'elements in the page is incorrect, got {len(data)}, expected 7 or less'
    )
    check_value(
        r,
        json['page']['size'] == 3,
        msg=f'expected count to be 7, got {json['page']['size']}'
    )
    check_value(
        r,
        json['page']['number'] == 2,
        msg=f'got page_number {json['page']['number']}, expected 2'
    )

    # Verificar que los IDs están en hexadecimal
    check_value(r, all(is_valid_objectid(comment['author']['id']) for comment in data))


def test_post_likes(login: LoginUser, jwt_with_char: str, post_id: str):
    # DAR LIKE
    # Requiere autenticación
    r = requests.post(f'{SITE}/posts/{post_id}/like')
    check(r, HTTPStatus.UNAUTHORIZED)

    # Esto deberia fallar porque por que no se ha activado un personaje
    r = requests.post(f'{SITE}/posts/{post_id}/like', headers=login.header)
    check(r, HTTPStatus.FORBIDDEN)

    # Post no existe
    r = requests.post(
        f'{SITE}/posts/{INVALID_ID}/like',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.post(
        f'{SITE}/posts/{post_id}/like',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.CREATED)

    # Error: ya se ha dado like
    r = requests.post(
        f'{SITE}/posts/{post_id}/like',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.CONFLICT)

    # Comprobar que es cierto
    r = requests.get(
        f'{SITE}/posts/{post_id}',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.OK)
    check_value(r, r.json().get('liked') == True)


    # QUITAR LIKE
    # Requiere autenticación
    r = requests.delete(f'{SITE}/posts/{post_id}/like')
    check(r, HTTPStatus.UNAUTHORIZED)

    # Esto deberia fallar porque por que no se ha activado un personaje
    r = requests.delete(f'{SITE}/posts/{post_id}/like', headers=login.header)
    check(r, HTTPStatus.FORBIDDEN)

    # Post no existe
    r = requests.delete(
        f'{SITE}/posts/{INVALID_ID}/like',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.delete(
        f'{SITE}/posts/{post_id}/like',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.NO_CONTENT)

    # Error: no se ha dado like
    r = requests.delete(
        f'{SITE}/posts/{post_id}/like',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.CONFLICT)

    # Comprobar que es cierto
    r = requests.get(
        f'{SITE}/posts/{post_id}',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.OK)
    check_value(r, r.json().get('liked') == False)


def test_post_delete(login: LoginUser, jwt_with_char: str, post_id: str):
    # BORRAR UN POST
    # Requiere autenticacion
    r = requests.delete(f'{SITE}/posts/{post_id}')
    check(r, HTTPStatus.UNAUTHORIZED)

    # No hay un personaje activo
    r = requests.delete(f'{SITE}/posts/{post_id}', headers=login.header)
    check(r, HTTPStatus.FORBIDDEN)

    # Post no existe
    r = requests.delete(
        f'{SITE}/posts/{INVALID_ID}',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.NOT_FOUND)

    # No es el dueño del post
    # Primero obtener un id de otro post
    assert USER_EXISTS.character is not None, 'User has no character??? Setup failed somehow'
    r = requests.get(f'{SITE}/posts?author={USER_EXISTS.character.id}&count=1')
    check(r, HTTPStatus.OK)
    other_post_id = r.json()['_embedded']['postViewList'][0]['id']

    r = requests.delete(
        f'{SITE}/posts/{other_post_id}',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.FORBIDDEN)

    # Correcto
    r = requests.delete(
        f'{SITE}/posts/{post_id}',
        headers={'Authorization': 'Bearer ' + jwt_with_char}
    )
    check(r, HTTPStatus.NO_CONTENT)

    # Ahora el post no existe, ni se pueden obtener comentarios
    r = requests.get(f'{SITE}/posts/{post_id}')
    check(r, HTTPStatus.NOT_FOUND)

    r = requests.get(f'{SITE}/posts/{post_id}/comments')
    check(r, HTTPStatus.NOT_FOUND)


def test_posts(login: LoginUser):
    print('\n==== POSTS ======================================================')
    jwt_with_char = test_post_create(login)
    post_id = test_post_search(login, jwt_with_char)
    test_post_comments(login, jwt_with_char, post_id)
    test_post_likes(login, jwt_with_char, post_id)
    test_post_delete(login, jwt_with_char, post_id)


if __name__ == '__main__':
    user = setup()
    test_posts(user)
    end()

