package com.tmd.backend.repository;

import com.tmd.backend.domain.pet.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByUserId(Long userId);
    Optional<Pet> findByIdAndUserId(Long id, Long userId);
    Optional<Pet> findByIdAndUserEmail(Long id, String email);
}
