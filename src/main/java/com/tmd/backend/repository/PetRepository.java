package com.tmd.backend.repository;

import com.tmd.backend.domain.pet.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByUserId(Long userId);
    @Query("SELECT p.id FROM Pet p WHERE p.user.id = :userId ORDER BY p.id ASC")
    List<Long> findIdsByUserId(@Param("userId") Long userId);
    Optional<Pet> findByIdAndUserId(Long id, Long userId);
    Optional<Pet> findByIdAndUserEmail(Long id, String email);
}
