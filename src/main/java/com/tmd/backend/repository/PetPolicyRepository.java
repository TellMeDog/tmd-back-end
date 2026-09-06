package com.tmd.backend.repository;

import com.tmd.backend.domain.place.PlacePetPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetPolicyRepository extends JpaRepository<PlacePetPolicy, Long> {

}
