package com.acabaaqui.services;

import com.acabaaqui.model.PerfilUser;
import com.acabaaqui.model.ProviderRegion;
import com.acabaaqui.model.User;
import com.acabaaqui.persistence.ProviderRegionRepository;
import com.acabaaqui.persistence.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class ProviderCoverageService {

    @Inject
    ProviderRegionRepository areaRepository;

    @Inject
    UserRepository userRepository;

    public List<ProviderRegion> listar(Integer providerId) {
        User provider = userRepository.buscarUsuarioPorId(providerId);
        return provider == null ? null : areaRepository.listarPorPrestador(provider);
    }

    @Transactional
    public ProviderRegion criar(Integer providerId, CoverageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Informe uma região.");
        }
        String city = request.cidade() == null ? "" : request.cidade().trim();
        if (city.isBlank() || city.length() > 100) {
            throw new IllegalArgumentException("Informe uma cidade válida.");
        }
        if (request.uf() == null || !request.uf().trim().matches("(?i)[a-z]{2}")) {
            throw new IllegalArgumentException("Informe uma UF válida com 2 letras.");
        }

        User provider = userRepository.buscarUsuarioPorId(providerId);
        if (provider == null || provider.getPerfil() != PerfilUser.prestador) {
            return null;
        }
        String uf = request.uf().trim().toUpperCase(Locale.ROOT);
        if (areaRepository.find("prestador = ?1 and lower(cidade) = lower(?2) and uf = ?3",
                provider, city, uf).firstResult() != null) {
            throw new IllegalArgumentException("Essa região já está cadastrada.");
        }

        ProviderRegion area = new ProviderRegion();
        area.setPrestador(provider);
        area.setCidade(city);
        area.setUf(uf);
        areaRepository.persist(area);
        return area;
    }

    @Transactional
    public boolean remover(Integer providerId, Long areaId) {
        ProviderRegion area = areaRepository.findById(areaId);
        if (area == null || !area.getPrestador().getId().equals(providerId)) {
            return false;
        }
        areaRepository.delete(area);
        return true;
    }

    public record CoverageRequest(String cidade, String uf) { }
}
