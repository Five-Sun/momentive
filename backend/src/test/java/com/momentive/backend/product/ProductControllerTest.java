package com.momentive.backend.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }

    @Test
    void getProducts_returns_paginated_list_ordered_by_latest() throws Exception {
        Product older = new Product("older", "desc", 10000, null, false);
        Product newer = new Product("newer", "desc", 20000, 15000, false);
        productRepository.save(older);
        productRepository.save(newer);

        mockMvc.perform(get("/products").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("newer"))
                .andExpect(jsonPath("$.content[0].discountPrice").value(15000))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getProducts_returns_empty_array_when_no_products() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void getProduct_returns_detail_with_images_in_order() throws Exception {
        Product product = new Product("치즈맛 캔", "고양이 아님, 강아지용 캔입니다.", 5000, null, false);
        product.addImage("https://example.com/1.jpg", 0);
        product.addImage("https://example.com/2.jpg", 1);
        Product saved = productRepository.save(product);

        mockMvc.perform(get("/products/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("치즈맛 캔"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].url").value("https://example.com/1.jpg"))
                .andExpect(jsonPath("$.images[1].url").value("https://example.com/2.jpg"));
    }

    @Test
    void getProduct_returns_404_for_missing_id() throws Exception {
        mockMvc.perform(get("/products/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
}
