package com.momentive.backend.pet.domain;

import com.momentive.backend.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String breed;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PetGender gender;

    private Double weightKg;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Pet(User user, String name, String breed, LocalDate birthDate, PetGender gender, Double weightKg) {
        this.user = user;
        this.name = name;
        this.breed = breed;
        this.birthDate = birthDate;
        this.gender = gender;
        this.weightKg = weightKg;
        this.createdAt = LocalDateTime.now();
    }

    public static Pet create(User user, String name, String breed, LocalDate birthDate, PetGender gender, Double weightKg) {
        return new Pet(user, name, breed, birthDate, gender, weightKg);
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void update(String name, String breed, LocalDate birthDate, PetGender gender, Double weightKg) {
        this.name = name;
        this.breed = breed;
        this.birthDate = birthDate;
        this.gender = gender;
        this.weightKg = weightKg;
    }
}
