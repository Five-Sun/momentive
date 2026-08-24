package com.momentive.backend.product.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer price;

    private Integer discountPrice;

    @Column(nullable = false)
    private Boolean soldOut;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
    }

    public Product(String name, String description, Integer price, Integer discountPrice, Boolean soldOut) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountPrice = discountPrice;
        this.soldOut = soldOut;
        this.createdAt = LocalDateTime.now();
    }

    public void addImage(String url, int displayOrder) {
        images.add(new ProductImage(this, url, displayOrder));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getPrice() {
        return price;
    }

    public Integer getDiscountPrice() {
        return discountPrice;
    }

    public Boolean getSoldOut() {
        return soldOut;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public String getThumbnailUrl() {
        return images.isEmpty() ? null : images.get(0).getUrl();
    }
}
