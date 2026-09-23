package com.luma.users.application;

import com.luma.common.error.AuthenticationFailedException;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el usuario de la peticion a partir del sujeto del token.
 *
 * <p>Existe para que ningun controlador reciba un identificador de usuario por
 * parametro. El usuario siempre sale del token: asi no hay forma de leer los
 * datos de otra persona cambiando un numero en la direccion.
 */
@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User require(String publicId) {
        return users.findByPublicId(publicId)
                .filter(User::isActive)
                .orElseThrow(AuthenticationFailedException::new);
    }

    @Transactional(readOnly = true)
    public Long requireId(String publicId) {
        return require(publicId).getId();
    }
}
