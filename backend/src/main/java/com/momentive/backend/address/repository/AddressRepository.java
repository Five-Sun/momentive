package com.momentive.backend.address.repository;

import com.momentive.backend.address.domain.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
}
