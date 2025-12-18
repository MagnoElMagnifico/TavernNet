import requests
from http import HTTPStatus
from .utils import LoginUser, check, check_value, setup, end, is_valid_objectid, random_text
from .utils import SITE, RANDOM_WORDS, INVALID_ID, USER_EXISTS, USERNAME_NOT_EXISTS


def test_parties(login: LoginUser) -> str:
    print('\n==== PARTIES ====')
    # CREAR PARTY
    # Requiere autenticación
    r = requests.post(f'{SITE}/parties')
    check(r, HTTPStatus.UNAUTHORIZED)

    # Correcto sin cuerpo: campos por defecto
    r = requests.post(
        f'{SITE}/parties',
        headers=login.header,
        json={}
    )
    check(r, HTTPStatus.CREATED)
    empty_party_id = r.headers.get('location').split('/')[-1]
    check_value(r, is_valid_objectid(empty_party_id))

    # Personajes no encontrados
    r = requests.post(
        f'{SITE}/parties',
        headers=login.header,
        json={
            'name': random_text(RANDOM_WORDS, mininum=3, maximum=10, max_char=64),
            'description': random_text(RANDOM_WORDS, mininum=3, maximum=20),
            'initial_members': [INVALID_ID],
        },
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    assert USER_EXISTS.character is not None and login.character is not None, 'User has no character??? Setup failed somehow'
    party_name = random_text(RANDOM_WORDS, mininum=1, maximum=1)
    r = requests.post(
        f'{SITE}/parties',
        headers=login.header,
        json={
            'name': party_name,
            'description': random_text(RANDOM_WORDS, mininum=3, maximum=20),
            'initial_members': [login.character.id, USER_EXISTS.character.id],
        },
    )
    check(r, HTTPStatus.CREATED)
    party_id = r.headers.get('location').split('/')[-1]
    check_value(r, is_valid_objectid(party_id))


    # VER/BUSCAR PARTIES
    # Limites de paginación
    r = requests.get(f'{SITE}/parties?page=100000')
    check(r, HTTPStatus.OK)
    json = r.json()
    check_value(r, json.get('_embedded') is None)

    # Otros limites
    r = requests.get(f'{SITE}/parties?page=-1')
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    r = requests.get(f'{SITE}/parties?count=-1')
    check(r, HTTPStatus.UNPROCESSABLE_ENTITY)

    # Correcto
    r = requests.get(f'{SITE}/parties?count=7')
    check(r, HTTPStatus.OK)
    json = r.json()
    data = json.get('_embedded')
    check_value(r, data is not None)
    data = data['summaryList']

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
    check_value(r, all(is_valid_objectid(party['id']) for party in data))

    # Comprobar que la search funciona: solo debe salir una party
    r = requests.get(f'{SITE}/parties?search={party_name}')
    check(r, HTTPStatus.OK)
    json = r.json()
    data = json.get('_embedded')
    check_value(r, data is not None and len(data['summaryList']) == 1 and data['summaryList'][0]['id'] == party_id)


    # VER PARTY INDIVIDUAL
    # No existe
    r = requests.get(f'{SITE}/parties/{INVALID_ID}')
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.get(f'{SITE}/parties/{party_id}')
    check(r, HTTPStatus.OK)
    json = r.json()

    # Campos de la party correctos
    check_value(r, is_valid_objectid(json['id']))
    check_value(r, json['dm'] == login.username)
    check_value(r, json['name'] == party_name)
    check_value(r, json.get('ownerId') is None)

    # Informacion sobre los miembros
    check_value(r, all(member.get('id')    is not None for member in json['members']))
    check_value(r, all(member.get('name')  is not None for member in json['members']))
    check_value(r, all(member.get('user')  is not None for member in json['members']))
    check_value(r, all(member.get('level') is not None for member in json['members']))


    # AÑADIR MIEMBROS A LA PARTY
    # Verificar que la party está vacía
    r = requests.get(f'{SITE}/parties/{empty_party_id}')
    check(r, HTTPStatus.OK)
    check_value(r, r.json().get('members') is None or len(r.json().get('members')) == 0)

    # Obtener personajes de un usuario para añadirlos a la party
    r = requests.get(f'{SITE}/users/{USER_EXISTS.username}')
    check(r, HTTPStatus.OK)
    check_value(r, r.json().get('characters') is not None)
    char_ids = list(map(lambda char: char['id'], r.json().get('characters')))
    print(f'Characters to add from user {USER_EXISTS.username}: {char_ids}')

    # Requiere autenticación
    r = requests.post(
        f'{SITE}/parties/{empty_party_id}/members',
        json=char_ids
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # No es DM
    r = requests.post(
        f'{SITE}/auth/login',
        json={'username': USER_EXISTS.username, 'password': USER_EXISTS.password}
    )
    check(r, HTTPStatus.OK)
    jwt: str = r.json().get('access_token')
    print(f'JWT ({len(jwt)}): {jwt}')

    r = requests.post(
        f'{SITE}/parties/{empty_party_id}/members',
        headers={'Authentication': 'Bearer ' + jwt},
        json=char_ids,
    )
    check(r, HTTPStatus.FORBIDDEN)

    # Personajes que no existen
    r = requests.post(
        f'{SITE}/parties/{empty_party_id}/members',
        headers=login.header,
        json=[INVALID_ID],
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.post(
        f'{SITE}/parties/{empty_party_id}/members',
        headers=login.header,
        json=char_ids
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Verificar que se han añadido
    r = requests.get(f'{SITE}/parties/{empty_party_id}')
    check(r, HTTPStatus.OK)
    check_value(r, r.json().get('members') is not None and len(r.json().get('members')) == 2)


    # QUITAR MIEMBROS
    # Requiere autenticación
    r = requests.delete(f'{SITE}/parties/{empty_party_id}/members/{char_ids[0]}')
    check(r, HTTPStatus.UNAUTHORIZED)

    # No es DM
    r = requests.delete(
        f'{SITE}/parties/{empty_party_id}/members/{char_ids[0]}',
        headers={'Authentication': 'Bearer ' + jwt},
    )
    check(r, HTTPStatus.FORBIDDEN)

    # No existe el miembro
    r = requests.delete(
        f'{SITE}/parties/{empty_party_id}/members/{INVALID_ID}',
        headers=login.header
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.delete(
        f'{SITE}/parties/{empty_party_id}/members/{char_ids[0]}',
        headers=login.header
    )
    check(r, HTTPStatus.NO_CONTENT)

    # Verificar que se ha borrado
    r = requests.get(f'{SITE}/parties/{empty_party_id}')
    check(r, HTTPStatus.OK)
    members = r.json().get('members')
    check_value(r, members is not None and len(members) == 1 and members[0]['id'] == char_ids[1])


    # EDITAR DM
    # Requiere autenticación
    r = requests.put(
        f'{SITE}/parties/{empty_party_id}/dm',
        json={'username': USER_EXISTS.username}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # No es DM
    r = requests.put(
        f'{SITE}/parties/{empty_party_id}/members/dm',
        headers={'Authentication': 'Bearer ' + jwt},
        json={'username': USER_EXISTS.username}
    )
    check(r, HTTPStatus.FORBIDDEN)

    # No existe el usuario
    r = requests.put(
        f'{SITE}/parties/{empty_party_id}/members/dm',
        headers=login.header,
        json={'username': USERNAME_NOT_EXISTS}
    )
    check(r, HTTPStatus.NOT_FOUND)

    # No existe el usuario
    r = requests.put(
        f'{SITE}/parties/{empty_party_id}/members/dm',
        headers=login.header,
        json={'username': USER_EXISTS.username}
    )
    check(r, HTTPStatus.NO_CONTENT)


    # BORRAR PARTY
    # Requiere autenticación
    r = requests.delete(f'{SITE}/parties/{empty_party_id}')
    check(r, HTTPStatus.UNAUTHORIZED)

    # No es DM: se ha cambiado antes
    r = requests.delete(
        f'{SITE}/parties/{empty_party_id}',
        headers=login.header,
    )
    check(r, HTTPStatus.FORBIDDEN)

    # Correcto
    r = requests.delete(
        f'{SITE}/parties/{empty_party_id}',
        headers={'Authentication': 'Bearer ' + jwt},
    )
    check(r, HTTPStatus.NO_CONTENT)

    # Comprobar que no existe
    r = requests.get(f'{SITE}/parties/{empty_party_id}')
    check(r, HTTPStatus.NOT_FOUND)


    return party_id


def test_messages(party_id: str, login: LoginUser):
    assert USER_EXISTS.character is not None and login.character is not None, 'User has no character??? Setup failed somehow'

    # SETUP: iniciar sesión con miembros y no miembros
    #
    # Este usuario tiene 2 personajes, uno que es miembro
    # (USER_EXISTS.character) y otro que no. Buscar el ID del que no para
    # comprobar que no puede mandar mensajes
    r = requests.get(f'{SITE}/users/{USER_EXISTS.username}')
    check(r, HTTPStatus.OK)
    check_value(r, r.json().get('characters') is not None)

    char_ids = list(map(lambda char: char['id'], r.json().get('characters')))
    print(f'Characters to add from user {USER_EXISTS.username}: {char_ids}')

    not_member = None
    if USER_EXISTS.character.id == char_ids[0]:
        not_member = char_ids[1]
    elif USER_EXISTS.character.id == char_ids[1]:
        not_member = char_ids[0]
    else:
        assert False

    # Iniciar sesión como un usuario cualquiera sin personaje
    r = requests.post(
        f'{SITE}/auth/login',
        json={
            'username': USER_EXISTS.username,
            'password': USER_EXISTS.password,
        },
    )
    check(r, HTTPStatus.OK)
    not_char_jwt: str = r.json().get('access_token')
    print(f'JWT ({len(not_char_jwt)}): {not_char_jwt}')

    # Iniciar sesión como ese personaje para usar su JWT
    r = requests.post(
        f'{SITE}/auth/login',
        json={
            'username': USER_EXISTS.username,
            'password': USER_EXISTS.password,
            'character_id': not_member
        },
    )
    check(r, HTTPStatus.OK)
    not_member_jwt: str = r.json().get('access_token')
    print(f'JWT ({len(not_member_jwt)}): {not_member_jwt}')

    # Lo mismo para un miembro
    r = requests.post(
        f'{SITE}/auth/character-login',
        headers=login.header,
        json={'character_id': login.character.id},
    )
    check(r, HTTPStatus.OK)
    member_jwt: str = r.json().get('access_token')

    # El usuario que se dio por parametros es el DM
    dm_jwt = login.jwt


    # ENVIAR MENSAJES
    # Sin autorización
    r = requests.post(
        f'{SITE}/parties/{party_id}/messages',
        json={'text': random_text(RANDOM_WORDS, mininum=1, maximum=50)}
    )
    check(r, HTTPStatus.UNAUTHORIZED)

    # No ha seleccionado personaje
    r = requests.post(
        f'{SITE}/parties/{party_id}/messages',
        headers={'Authorization': 'Bearer ' + not_char_jwt},
        json={'text': random_text(RANDOM_WORDS, mininum=1, maximum=50)}
    )
    check(r, HTTPStatus.BAD_REQUEST)


    # Personaje no miembro
    r = requests.post(
        f'{SITE}/parties/{party_id}/messages',
        headers={'Authorization': 'Bearer ' + not_member_jwt},
        json={'text': random_text(RANDOM_WORDS, mininum=1, maximum=50)}
    )
    check(r, HTTPStatus.FORBIDDEN)

    # No se ha encontrado la party
    r = requests.post(
        f'{SITE}/parties/{INVALID_ID}/messages',
        headers={'Authorization': 'Bearer ' + member_jwt},
        json={'text': random_text(RANDOM_WORDS, mininum=1, maximum=50)}
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.post(
        f'{SITE}/parties/{party_id}/messages',
        headers={'Authorization': 'Bearer ' + member_jwt},
        json={'text': random_text(RANDOM_WORDS, mininum=1, maximum=50)}
    )
    check(r, HTTPStatus.CREATED)

    # Mensaje como DM siendo miembro
    r = requests.post(
        f'{SITE}/parties/{party_id}/messages',
        headers={'Authorization': 'Bearer ' + member_jwt},
        json={
            'author': {'name': 'Aldeano', 'race': 'human'},
            'text': random_text(RANDOM_WORDS, mininum=1, maximum=50)}
    )
    check(r, HTTPStatus.FORBIDDEN)

    # Mensaje como DM
    r = requests.post(
        f'{SITE}/parties/{party_id}/messages',
        headers={'Authorization': 'Bearer ' + dm_jwt},
        json={
            'author': {'name': 'Aldeano', 'race': 'human'},
            'text': random_text(RANDOM_WORDS, mininum=1, maximum=50)}
    )
    check(r, HTTPStatus.CREATED)


    # LEER MENSAJES
    # Se necesita autenticación
    r = requests.get(f'{SITE}/parties/{party_id}/messages')
    check(r, HTTPStatus.UNAUTHORIZED)

    # Falta personaje y no es DM
    r = requests.get(
        f'{SITE}/parties/{party_id}/messages',
        headers={'Authorization': 'Bearer ' + not_char_jwt},
    )
    check(r, HTTPStatus.FORBIDDEN)

    # No es miembro
    r = requests.get(
        f'{SITE}/parties/{party_id}/messages',
        headers={'Authorization': 'Bearer ' + not_member_jwt},
    )
    check(r, HTTPStatus.FORBIDDEN)

    # No existe
    r = requests.get(
        f'{SITE}/parties/{INVALID_ID}/messages',
        headers={'Authorization': 'Bearer ' + member_jwt},
    )
    check(r, HTTPStatus.NOT_FOUND)

    # Correcto
    r = requests.get(
        f'{SITE}/parties/{party_id}/messages',
        headers={'Authorization': 'Bearer ' + member_jwt},
    )
    check(r, HTTPStatus.OK)

    # Correcto como DM
    r = requests.get(
        f'{SITE}/parties/{party_id}/messages',
        headers={'Authorization': 'Bearer ' + dm_jwt},
    )
    check(r, HTTPStatus.OK)


def test_parties_messages(login: LoginUser):
    party_id = test_parties(login)
    test_messages(party_id, login)


if __name__ == '__main__':
    user = setup()
    test_parties_messages(user)
    end()

