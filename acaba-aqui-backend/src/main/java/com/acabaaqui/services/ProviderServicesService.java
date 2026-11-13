package com.acabaaqui.services;

import com.acabaaqui.model.PerfilUser;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ProviderServiceRepository;
import com.acabaaqui.persistence.ServiceCatalogRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ProviderServicesService {

    @Inject
    ProviderServiceRepository providerServiceRepository;

    @Inject
    ServiceCatalogRepository serviceRepository;

    @Inject
    UserRepository userRepository;

    public List<Integer> listar(Integer providerId) {
        User provider = userRepository.buscarUsuarioPorId(providerId);
        return provider == null ? null : providerServiceRepository.listarPorPrestador(provider).stream()
                .map(providerService -> providerService.getServico().getId())
                .toList();
    }

    @Transactional
    public List<Integer> substituir(Integer providerId, List<Integer> serviceIds) {
        if (serviceIds == null) {
            throw new IllegalArgumentException("Informe a lista de serviços selecionados.");
        }

        User provider = userRepository.buscarUsuarioPorId(providerId);
        if (provider == null || provider.getPerfil() != PerfilUser.prestador) {
            return null;
        }

        Map<Integer, ServiceCatalogItem> selectedServices = new LinkedHashMap<>();
        for (Integer serviceId : serviceIds) {
            if (serviceId == null) {
                throw new IllegalArgumentException("A seleção contém um serviço inválido.");
            }
            ServiceCatalogItem service = serviceRepository.findById(serviceId.longValue());
            if (service == null || !service.isAtivo()) {
                throw new IllegalArgumentException("Um dos serviços selecionados não está disponível.");
            }
            selectedServices.putIfAbsent(serviceId, service);
        }

        providerServiceRepository.substituir(provider, new ArrayList<>(selectedServices.values()));
        return new ArrayList<>(selectedServices.keySet());
    }
}
