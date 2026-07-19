package com.example.product_api;

import com.example.product_api.controller.ProductController;
import com.example.product_api.dto.ProductRequest;
import com.example.product_api.dto.ProductResponse;
import com.example.product_api.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
    }

    @Test
    void shouldCreateProduct() throws Exception {
        ProductRequest request = new ProductRequest("Mouse", 25.0);
        ProductResponse response = new ProductResponse(1L, "Mouse", 25.0);

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.price").value(25.0));
    }

    @Test
    void shouldReturnAllProducts() throws Exception {
        ProductResponse p1 = new ProductResponse(1L, "Teclado", 50.0);
        ProductResponse p2 = new ProductResponse(2L, "Monitor", 200.0);

        when(productService.getAllProducts(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 10), 2));

        mockMvc.perform(get("/api/products?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldReturn400ForInvalidProduct() throws Exception {
        ProductRequest invalidRequest = new ProductRequest("", -10.0);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}