package com.resiliencelab.catalog.product;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        Product product = new Product(
                request.sku(), request.name(), request.description(), request.price());
        return ProductResponse.from(repository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return ProductResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(Pageable pageable) {
        return repository.findAll(pageable)
                .map(ProductResponse::from)
                .getContent();
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = findOrThrow(id);
        boolean skuChanged = !product.getSku().equals(request.sku());
        if (skuChanged && repository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        product.update(request.sku(), request.name(), request.description(), request.price());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOrThrow(id));
    }

    private Product findOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}