package com.acabaaqui.services;

import com.acabaaqui.model.ServiceCategory;
import com.acabaaqui.model.ServiceCatalogItem;
import com.acabaaqui.persistence.ServiceCategoryRepository;
import com.acabaaqui.persistence.ServiceCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class ServiceCatalogService {

    @Inject
    ServiceCategoryRepository categoryRepository;

    @Inject
    ServiceCatalogRepository serviceRepository;

    public List<ServiceCategory> listarCategorias(Boolean ativo) {
        return categoryRepository.listar(ativo);
    }

    @Transactional
    public ServiceCategory criarCategoria(String nome, String descricao) {
        String nomeNormalizado = validarTexto(nome, "Informe um nome de categoria válido.", 80);
        validarDescricaoOpcional(descricao);
        if (categoryRepository.buscarPorNome(nomeNormalizado) != null) {
            throw new IllegalArgumentException("Já existe uma categoria com esse nome.");
        }

        ServiceCategory category = new ServiceCategory();
        category.setNome(nomeNormalizado);
        category.setDescricao(normalizarDescricao(descricao));
        category.setAtivo(true);
        categoryRepository.persist(category);
        return category;
    }

    @Transactional
    public ServiceCategory atualizarCategoria(Integer id, String nome, String descricao, Boolean ativo) {
        ServiceCategory category = categoryRepository.findById(id.longValue());
        if (category == null) {
            return null;
        }

        String nomeNormalizado = validarTexto(nome, "Informe um nome de categoria válido.", 80);
        validarDescricaoOpcional(descricao);
        ServiceCategory sameName = categoryRepository.buscarPorNome(nomeNormalizado);
        if (sameName != null && !sameName.getId().equals(id)) {
            throw new IllegalArgumentException("Já existe uma categoria com esse nome.");
        }

        category.setNome(nomeNormalizado);
        category.setDescricao(normalizarDescricao(descricao));
        if (ativo != null) {
            category.setAtivo(ativo);
        }
        return category;
    }

    @Transactional
    public ServiceCategory desativarCategoria(Integer id) {
        ServiceCategory category = categoryRepository.findById(id.longValue());
        if (category != null) {
            category.setAtivo(false);
        }
        return category;
    }

    public List<ServiceCatalogItem> listarServicos(Boolean ativo) {
        return serviceRepository.listar(ativo);
    }

    @Transactional
    public ServiceCatalogItem criarServico(String nome, String descricao, Integer categoriaId) {
        ServiceCategory category = buscarCategoria(categoriaId);
        validarCategoriaAtiva(category);
        ServiceCatalogItem item = new ServiceCatalogItem();
        atualizarCampos(item, nome, descricao, category, null);
        item.setAtivo(true);
        serviceRepository.persist(item);
        return item;
    }

    @Transactional
    public ServiceCatalogItem atualizarServico(Integer id, String nome, String descricao,
                                                Integer categoriaId, Boolean ativo) {
        ServiceCatalogItem item = serviceRepository.findById(id.longValue());
        if (item == null) {
            return null;
        }

        ServiceCategory category = categoriaId == null
                ? item.getCategoria()
                : buscarCategoria(categoriaId);
        if (categoriaId != null && !categoriaId.equals(item.getCategoria().getId())) {
            validarCategoriaAtiva(category);
        }
        atualizarCampos(item, nome, descricao, category, ativo);
        return item;
    }

    @Transactional
    public ServiceCatalogItem desativarServico(Integer id) {
        ServiceCatalogItem item = serviceRepository.findById(id.longValue());
        if (item != null) {
            item.setAtivo(false);
        }
        return item;
    }

    private ServiceCategory buscarCategoria(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Selecione uma categoria.");
        }
        ServiceCategory category = categoryRepository.findById(id.longValue());
        if (category == null) {
            throw new IllegalArgumentException("A categoria selecionada não existe.");
        }
        return category;
    }

    private void validarCategoriaAtiva(ServiceCategory category) {
        if (!category.isAtivo()) {
            throw new IllegalArgumentException("Selecione uma categoria ativa.");
        }
    }

    private void atualizarCampos(ServiceCatalogItem item, String nome, String descricao,
                                 ServiceCategory category, Boolean ativo) {
        item.setNome(validarTexto(nome, "Informe um nome de serviço válido.", 100));
        item.setDescricao(validarTexto(descricao, "Informe uma descrição válida.", 1000));
        item.setCategoria(category);
        if (ativo != null) {
            item.setAtivo(ativo);
        }
    }

    private String validarTexto(String value, String message, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private void validarDescricaoOpcional(String descricao) {
        if (descricao != null && descricao.trim().length() > 255) {
            throw new IllegalArgumentException("A descrição da categoria deve ter até 255 caracteres.");
        }
    }

    private String normalizarDescricao(String descricao) {
        return descricao == null || descricao.isBlank() ? null : descricao.trim();
    }
}
