package com.momentive.backend.address.domain;

import com.momentive.backend.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String zipcode;

    @Column(nullable = false)
    private String address1;

    private String address2;

    @Column(nullable = false)
    private Boolean isDefault;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Address(User user, String recipient, String phone, String zipcode, String address1, String address2, Boolean isDefault) {
        this.user = user;
        this.recipient = recipient;
        this.phone = phone;
        this.zipcode = zipcode;
        this.address1 = address1;
        this.address2 = address2;
        this.isDefault = isDefault;
        this.createdAt = LocalDateTime.now();
    }

    public static Address create(User user, String recipient, String phone, String zipcode, String address1, String address2, boolean isDefault) {
        return new Address(user, recipient, phone, zipcode, address1, address2, isDefault);
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
    }

    public void update(String recipient, String phone, String zipcode, String address1, String address2) {
        this.recipient = recipient;
        this.phone = phone;
        this.zipcode = zipcode;
        this.address1 = address1;
        this.address2 = address2;
    }
}
