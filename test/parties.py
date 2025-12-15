import requests
from .utils import LoginUser, check, check_value, setup, end


def test_parties(login: LoginUser):
    print('\n==== PARTIES ====')
    # TODO: completar estos tests
    # VER/BUSCAR PARTIES
    # VER PARTY INDIVIDUAL
    # CREAR PARTY
    # AÑADIR MIEMBROS A LA PARTY
    # EDITAR PARTY
    # ...
    pass


if __name__ == '__main__':
    user = setup()
    test_parties(user)
    end()

