import requests
import json
from .utils import setup, check, SITE, USER_EXISTS
from http import HTTPStatus


def _print_json(doc):
    print(json.dumps(doc, indent=4))


def test_model():
    # USUARIOS
    print('==== USER SUMMARY (with pagination) ====')
    r = requests.get(f'{SITE}/users?search={USER_EXISTS.username}&count=1')
    check(r, HTTPStatus.OK, quiet=True)
    _print_json(r.json())

    print('==== USER ====')
    r = requests.get(f'{SITE}/users/{USER_EXISTS.username}')
    check(r, HTTPStatus.OK, quiet=True)
    _print_json(r.json())


    # PERSONAJES
    print('==== CHARACTER ====')
    assert USER_EXISTS.character is not None, 'User has no character??? Setup failed somehow'
    r = requests.get(f'{SITE}/users/{USER_EXISTS.username}/characters/{USER_EXISTS.character.name}')
    check(r, HTTPStatus.OK, quiet=True)
    _print_json(r.json())


    # POSTS
    print('==== POST (with pagination) ====')
    r = requests.get(f'{SITE}/posts?author={USER_EXISTS.character.id}&count=1')
    check(r, HTTPStatus.OK, quiet=True)
    rjson = r.json()
    _print_json(rjson)

    print('==== POST ====')
    post_id = rjson['_embedded']['postViewList'][0]['id']
    r = requests.get(f'{SITE}/posts/{post_id}')
    check(r, HTTPStatus.OK, quiet=True)
    _print_json(r.json())


    # COMENTARIOS
    print('==== COMMENTS ====')
    r = requests.get(f'{SITE}/posts/{post_id}/comments')
    check(r, HTTPStatus.OK, quiet=True)
    _print_json(r.json())

    # TODO: Party y mensaje


if __name__ == '__main__':
    setup()
    test_model()

