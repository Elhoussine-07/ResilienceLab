package com.resiliencelab.catalog.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    @Test
    void createSavesTheProductWhenSkuIsFree() {
        ProductRequest request = new ProductRequest("SKU-1", "Keyboard", "Tenkeyless", new BigDecimal("79.90"));
        when(repository.existsBySku("SKU-1")).thenReturn(false);
        when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service.create(request);

        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.price()).isEqualByComparingTo("79.90");
        verify(repository).save(any(Product.class));
    }

    @Test
    void createRejectsADuplicateSku() {
        ProductRequest request = new ProductRequest("SKU-1", "Keyboard", null, new BigDecimal("79.90"));
        when(repository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateSkuException.class);

        verify(repository, never()).save(any(Product.class));
    }

    @Test
    void getByIdThrowsWhenTheProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ProductNotFoundException.class);
    }
}