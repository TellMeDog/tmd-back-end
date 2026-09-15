package com.tmd.backend.repository;

import com.tmd.backend.config.QuerydslConfig;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class PetRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired PetRepository petRepository;

    @Test
    void findsOnlyOwnedPetIdsInAscendingOrder() {
        User owner = em.persist(User.createLocal("owner@example.com", "password", "보호자"));
        User other = em.persist(User.createLocal("other@example.com", "password", "다른 사용자"));
        Pet first = em.persist(Pet.create(owner, "첫째", PetBreed.MALTESE, 3.0, null, false, true, false));
        Pet second = em.persist(Pet.create(owner, "둘째", PetBreed.BEAGLE, 9.0, null, false, true, false));
        em.persist(Pet.create(other, "타인 반려견", PetBreed.SHIBA_INU, 7.0, null, false, true, false));
        em.flush();

        assertThat(petRepository.findIdsByUserId(owner.getId()))
            .containsExactly(first.getId(), second.getId());
    }
}
