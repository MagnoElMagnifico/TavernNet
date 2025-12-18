from .login import test_login
from .parties import test_parties_messages
from .posts import test_posts
from .users_characters import test_noauth_character, test_noauth_user, test_auth_characters, test_auth_user
from .model import test_model
from .utils import setup, end


if __name__ == '__main__':
    # Crear usuario y personaje para las pruebas
    user = setup()

    print('\n==== NO AUTHENTICATION ========================================')
    test_noauth_user(user)
    test_noauth_character(user)

    test_login(user)
    test_posts(user)
    test_parties_messages(user)

    print('\n==== ADMIN ======================================================')
    # TODO: ADMIN
    # Probar que estas operaciones no se pueden hacer sobre recursos de los que
    # no son dueños o sin autenticar. Repetir estas operaciones como ADMIN y ver
    # que funcionan.

    print('\n==== AUTH: USER AND CARACTERS ===================================')
    test_auth_characters(user)
    test_auth_user(user)


    print('\n==== MODEL ======================================================')
    test_model()

    end()

