package com.example.product_api.service;

import com.example.product_api.dto.ProductRequest;
import com.example.product_api.dto.ProductResponse;
import com.example.product_api.model.Product;
import com.example.product_api.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;


@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ProductService(ProductRepository productRepository, SimpMessagingTemplate messagingTemplate) {
        this.productRepository = productRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Obteniendo todos los productos paginados");
        return productRepository.findAll(pageable)
                .map(this::toProductResponse);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        return toProductResponse(product);
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product(request.getName(), request.getPrice());
        Product saved = productRepository.save(product);
        messagingTemplate.convertAndSend("/topic/products", "Nuevo producto creado: " + saved.getName());
        return toProductResponse(saved);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id: " + id));
        existing.setName(request.getName());
        existing.setPrice(request.getPrice());
        Product updated = productRepository.save(existing);
        return toProductResponse(updated);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            log.error("Producto no encontrado con id: {}", id);
            throw new RuntimeException("Producto no encontrado con id: " + id);
        }
        log.warn("Eliminando producto con id: {}", id);
        productRepository.deleteById(id);
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice());
    }
}