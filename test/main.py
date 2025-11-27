# pip install requests
# # or
# python -m venv .venv
# source .venv/bin/activate
# pip install requests
import requests
import random
from http import HTTPStatus

SITE = 'http://localhost:8080'
REFRESH_COOKIE = '__Secure-RefreshToken'

def check(response: requests.Response, expected_status):
    if response.status_code == expected_status:
        return

    print('==== ERROR ====')
    print('Petition:', response.request.method, response.url)
    print('Response code:', response.status_code, response.reason)
    print(response.text)
    exit(1)

username = f'test{random.randint(1, 10):02d}'




# TODO: más operaciones que no necesitan autenticacion
response = requests.get(f'{SITE}/users')
check(response, HTTPStatus.OK)

# Crear un usuario
response = requests.post(f'{SITE}/users', json = {'username': username, 'password': 'password-test'})
check(response, HTTPStatus.CREATED)

# Ver datos del usuario
response = requests.get(f'{SITE}/users/{username}')
check(response, HTTPStatus.OK)
print(response.json())

# Comprobar que se deniega el acceso por no estar autenticado
response = requests.delete(f'{SITE}/users/{username}')
check(response, HTTPStatus.UNAUTHORIZED)


print('==== LOGIN ====')
# Iniciar sesión
response = requests.post(f'{SITE}/auth/login', json = {'username': username, 'password': 'password-test'})
check(response, HTTPStatus.OK)

json = response.json()
jwt = json['jwt']
refresh_token = response.cookies[REFRESH_COOKIE]
print('JWT', jwt)
print('RefreshToken:', refresh_token)

print('-- Refresh tokens --')
response = requests.post(f'{SITE}/auth/refresh', cookies={REFRESH_COOKIE: refresh_token})
check(response, HTTPStatus.OK)

json = response.json()
jwt = json['jwt']
refresh_token = response.cookies[REFRESH_COOKIE]
print('JWT', jwt)
print('RefreshToken:', refresh_token)

# Borrar el usuario
response = requests.delete(f'{SITE}/users/{username}', headers={'Authorization': 'Bearer ' + jwt})
check(response, HTTPStatus.NO_CONTENT)

