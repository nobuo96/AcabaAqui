package com.acabaaqui.services;

import com.acabaaqui.model.PerfilUser;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServicePersonalInfoTest {

    @Test
    void updatesOnlyEditablePersonalFields() {
        User user = new User();
        user.setId(10);
        user.setNome("Nome antigo");
        user.setEmail("pessoa@example.com");
        user.setTelefone("11999990000");
        user.setPerfil(PerfilUser.cliente);
        user.setEmailVerificado(true);

        UserService service = new UserService();
        service.repository = new UserRepository() {
            @Override
            public User buscarUsuarioPorId(Integer id) {
                return id == 10 ? user : null;
            }
        };

        User updated = service.atualizarDadosPessoais(
                10, "  Novo nome  ", "  11988887777  ", LocalDate.of(1994, 7, 12));

        assertEquals("Novo nome", updated.getNome());
        assertEquals("11988887777", updated.getTelefone());
        assertEquals(LocalDate.of(1994, 7, 12), updated.getDataNascimento());
        assertEquals("pessoa@example.com", updated.getEmail());
        assertEquals(PerfilUser.cliente, updated.getPerfil());
        assertEquals(true, updated.getEmailVerificado());
    }

    @Test
    void rejectsFutureBirthDate() {
        UserService service = new UserService();
        assertThrows(IllegalArgumentException.class, () -> service.atualizarDadosPessoais(
                10, "Nome válido", "11999990000", LocalDate.now().plusDays(1)));
    }

    @Test
    void returnsNullWhenUserDoesNotExist() {
        UserService service = new UserService();
        service.repository = new UserRepository() {
            @Override
            public User buscarUsuarioPorId(Integer id) {
                return null;
            }
        };

        assertNull(service.atualizarDadosPessoais(404, "Nome válido", "11999990000", null));
    }
}