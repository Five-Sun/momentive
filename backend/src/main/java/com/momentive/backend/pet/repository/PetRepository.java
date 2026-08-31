package com.momentive.backend.pet.repository;

import com.momentive.backend.pet.domain.Pet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
