package com.acabaaqui.services;

import com.acabaaqui.model.Address;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.AddressRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class AddressService {

    @Inject
    AddressRepository addressRepository;

    @Inject
    UserRepository userRepository;

    public List<Address> listar(Integer userId) {
        User user = userRepository.buscarUsuarioPorId(userId);
        return user == null ? null : addressRepository.listarAtivos(user);
    }

    @Transactional
    public Address criar(Integer userId, AddressRequest request) {
        User user = bloquearUsuario(userId);
        if (user == null) {
            return null;
        }

        Address address = new Address();
        address.setUsuario(user);
        atualizarCampos(address, request);
        address.setAtivo(true);
        address.setPrincipal(Boolean.TRUE.equals(request.principal()));
        if (address.isPrincipal()) {
            desmarcarPrincipal(user, null);
        }
        addressRepository.persist(address);
        return address;
    }

    @Transactional
    public Address atualizar(Integer userId, Long addressId, AddressRequest request) {
        User user = bloquearUsuario(userId);
        if (user == null) {
            return null;
        }

        Address address = buscarDoUsuario(user, addressId);
        if (address == null || !address.isAtivo()) {
            return null;
        }

        atualizarCampos(address, request);
        if (Boolean.TRUE.equals(request.principal())) {
            desmarcarPrincipal(user, address);
            address.setPrincipal(true);
        } else if (Boolean.FALSE.equals(request.principal())) {
            address.setPrincipal(false);
        }
        return address;
    }

    @Transactional
    public Address definirPrincipal(Integer userId, Long addressId) {
        User user = bloquearUsuario(userId);
        if (user == null) {
            return null;
        }

        Address address = buscarDoUsuario(user, addressId);
        if (address == null || !address.isAtivo()) {
            return null;
        }

        desmarcarPrincipal(user, address);
        address.setPrincipal(true);
        return address;
    }

    @Transactional
    public Address desativar(Integer userId, Long addressId) {
        User user = bloquearUsuario(userId);
        if (user == null) {
            return null;
        }

        Address address = buscarDoUsuario(user, addressId);
        if (address == null || !address.isAtivo()) {
            return null;
        }

        address.setAtivo(false);
        address.setPrincipal(false);
        return address;
    }

    static void validar(AddressRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dados do endereço são obrigatórios.");
        }
        if (request.cep() == null || !request.cep().matches("\\d{5}-?\\d{3}")) {
            throw new IllegalArgumentException("Informe um CEP válido com 8 dígitos.");
        }
        textoObrigatorio(request.numero(), "Informe o número do endereço ou s/n.", 20);
        textoObrigatorio(request.cidade(), "Informe a cidade.", 100);
        if (request.uf() == null || !request.uf().trim().matches("(?i)[a-z]{2}")) {
            throw new IllegalArgumentException("Informe uma UF válida com 2 letras.");
        }
        validarOpcional(request.logradouro(), "logradouro", 150);
        validarOpcional(request.complemento(), "complemento", 100);
        validarOpcional(request.bairro(), "bairro", 100);
        validarOpcional(request.apelido(), "apelido", 50);
    }

    private User bloquearUsuario(Integer userId) {
        return userRepository.buscarUsuarioPorIdComBloqueio(userId);
    }

    private Address buscarDoUsuario(User user, Long addressId) {
        Address address = addressRepository.findById(addressId);
        return address != null && address.getUsuario().getId().equals(user.getId()) ? address : null;
    }

    private void desmarcarPrincipal(User user, Address manter) {
        for (Address current : addressRepository.list("usuario = ?1 and principal = true", user)) {
            if (manter == null || !current.getId().equals(manter.getId())) {
                current.setPrincipal(false);
            }
        }
    }

    private void atualizarCampos(Address address, AddressRequest request) {
        validar(request);
        address.setCep(request.cep().replace("-", ""));
        address.setLogradouro(normalizar(request.logradouro()));
        address.setNumero(request.numero().trim());
        address.setComplemento(normalizar(request.complemento()));
        address.setBairro(normalizar(request.bairro()));
        address.setCidade(request.cidade().trim());
        address.setUf(request.uf().trim().toUpperCase(Locale.ROOT));
        address.setApelido(normalizar(request.apelido()));
    }

    private static String textoObrigatorio(String value, String message, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static void validarOpcional(String value, String field, int maxLength) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException("O campo " + field + " deve ter até " + maxLength + " caracteres.");
        }
    }

    private static String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record AddressRequest(String cep, String logradouro, String numero, String complemento,
                                 String bairro, String cidade, String uf, String apelido, Boolean principal) { }
}
