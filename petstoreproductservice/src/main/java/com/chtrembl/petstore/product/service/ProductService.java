package com.chtrembl.petstore.product.service;

import com.chtrembl.petstore.product.model.Product;
import com.chtrembl.petstore.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> findProductsByStatus(List<String> status) {
        log.info("Finding products with status: {}", status);
        try {
            List<Product> allProducts = productRepository.findAll();
            log.info("Loaded {} products from database", allProducts.size());
            
            List<Product> filtered = allProducts.stream()
                    .filter(product -> {
                        if (product.getStatus() == null) {
                            log.warn("Product {} has null status", product.getId());
                            return false;
                        }
                        return status.contains(product.getStatus().getValue());
                    })
                    .toList();
            
            log.info("Filtered to {} products with status: {}", filtered.size(), status);
            return filtered;
        } catch (Exception e) {
            log.error("Error finding products by status: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Optional<Product> findProductById(Long productId) {
        log.info("Finding product with id: {}", productId);
        return productRepository.findById(productId);
    }

    public List<Product> getAllProducts() {
        log.info("Getting all products");
        return productRepository.findAll();
    }

    public int getProductCount() {
        return (int) productRepository.count();
    }
}