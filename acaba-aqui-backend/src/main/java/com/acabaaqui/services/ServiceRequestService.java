package com.acabaaqui.services;

import com.acabaaqui.model.Address;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.model.ServiceRequest;
import com.acabaaqui.model.ServiceRequestStatus;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.AddressRepository;
import com.acabaaqui.persistence.ServiceCatalogRepository;
import com.acabaaqui.persistence.ServiceRequestRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class ServiceRequestService {

    @Inject
    ServiceRequestRepository requestRepository;

    @Inject
    ServiceCatalogRepository serviceRepository;

    @Inject
    AddressRepository addressRepository;

    @Inject
    UserRepository userRepository;

    public List<ServiceRequest> listar(Integer userId) {
        User user = userRepository.buscarUsuarioPorId(userId);
        return user == null ? null : requestRepository.listarPorUsuario(user);
    }

    @Transactional
    public ServiceRequest criar(Integer userId, CreateServiceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dados da solicitação são obrigatórios.");
        }
        BigDecimal area = validarArea(request.areaM2());
        if (request.servicoId() == null) {
            throw new IllegalArgumentException("Selecione um serviço.");
        }
        if (request.enderecoId() == null) {
            throw new IllegalArgumentException("Selecione um endereço.");
        }

        User user = userRepository.buscarUsuarioPorId(userId);
        if (user == null) {
            return null;
        }
        ServiceCatalogItem service = serviceRepository.findById(request.servicoId().longValue());
        if (service == null || !service.isAtivo()) {
            throw new IllegalArgumentException("O serviço selecionado não está disponível.");
        }
        Address address = addressRepository.findById(request.enderecoId());
        if (address == null || !address.isAtivo() || !address.getUsuario().getId().equals(userId)) {
            throw new IllegalArgumentException("Selecione um endereço ativo cadastrado na sua conta.");
        }

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setUsuario(user);
        serviceRequest.setServico(service);
        serviceRequest.setEnderecoOrigemId(address.getId());
        serviceRequest.setAreaM2(area);
        serviceRequest.setStatus(ServiceRequestStatus.CRIADA);
        copiarEndereco(address, serviceRequest);
        requestRepository.persist(serviceRequest);
        return serviceRequest;
    }

    @Transactional
    public ServiceRequest cancelar(Integer userId, Long requestId) {
        ServiceRequest request = requestRepository.buscarComBloqueio(requestId);
        if (request == null || !request.getUsuario().getId().equals(userId)) {
            return null;
        }
        if (request.getStatus() == ServiceRequestStatus.ENCERRADA) {
            throw new IllegalStateException("A solicitação já está encerrada.");
        }
        request.setStatus(ServiceRequestStatus.ENCERRADA);
        return request;
    }

    static BigDecimal validarArea(BigDecimal area) {
        if (area == null || area.signum() <= 0) {
            throw new IllegalArgumentException("Informe uma área maior que zero.");
        }
        if (area.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("A área deve ter até 2 casas decimais e no máximo 8 dígitos inteiros.");
        }
        BigDecimal normalized = area.setScale(2);
        if (normalized.precision() > 10) {
            throw new IllegalArgumentException("A área deve ter até 2 casas decimais e no máximo 8 dígitos inteiros.");
        }
        return normalized;
    }

    private void copiarEndereco(Address address, ServiceRequest request) {
        request.setEnderecoCep(address.getCep());
        request.setEnderecoLogradouro(address.getLogradouro());
        request.setEnderecoNumero(address.getNumero());
        request.setEnderecoComplemento(address.getComplemento());
        request.setEnderecoBairro(address.getBairro());
        request.setEnderecoCidade(address.getCidade());
        request.setEnderecoUf(address.getUf());
        request.setEnderecoApelido(address.getApelido());
    }

    public record CreateServiceRequest(Integer servicoId, Long enderecoId, BigDecimal areaM2) { }
}
