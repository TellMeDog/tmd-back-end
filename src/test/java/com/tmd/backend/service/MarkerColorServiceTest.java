package com.tmd.backend.service;

import com.tmd.backend.ai.AccessScope;
import com.tmd.backend.ai.WeightLimitType;
import com.tmd.backend.common.MarkerColor;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.place.PlacePetPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarkerColorServiceTest {

    private final MarkerColorService service = new MarkerColorService();

    @Test
    void 반려동물이_없으면_정책과_무관하게_GREY다() {
        assertThat(service.calculateMarkerColor(policy(AccessScope.NONE), null))
            .isEqualTo(MarkerColor.GREY);
    }

    @Test
    void 정책이_없으면_GREY다() {
        assertThat(service.calculateMarkerColor(null, pet(PetBreed.MALTESE, 5.0, true, true, true)))
            .isEqualTo(MarkerColor.GREY);
    }

    @Test
    void 출입범위가_없거나_UNKNOWN이면_GREY다() {
        Pet pet = pet(PetBreed.MALTESE, 5.0, true, true, true);

        assertThat(service.calculateMarkerColor(policy(null), pet)).isEqualTo(MarkerColor.GREY);
        assertThat(service.calculateMarkerColor(policy(AccessScope.UNKNOWN), pet)).isEqualTo(MarkerColor.GREY);
    }

    @Test
    void 출입불가_정책이면_RED다() {
        assertThat(service.calculateMarkerColor(
            policy(AccessScope.NONE),
            pet(PetBreed.MALTESE, 5.0, true, true, true)
        )).isEqualTo(MarkerColor.RED);
    }

    @Test
    void 출입금지된_맹견이면_defaultPolicy보다_RED가_우선한다() {
        PlacePetPolicy policy = policy(AccessScope.ALL);
        when(policy.getDangerousBreedAllowed()).thenReturn(false);
        when(policy.getDefaultPolicy()).thenReturn("YELLOW");

        assertThat(service.calculateMarkerColor(
            policy,
            pet(PetBreed.TOSA_INU, 25.0, true, true, true)
        )).isEqualTo(MarkerColor.RED);
    }

    @Test
    void 맹견의_MUZZLE조건을_충족하지_못하면_RED다() {
        PlacePetPolicy policy = policy(AccessScope.ALL);
        when(policy.getDangerousBreedAllowed()).thenReturn(true);
        when(policy.getDangerousBreedAllowedCondition()).thenReturn("MUZZLE");

        assertThat(service.calculateMarkerColor(
            policy,
            pet(PetBreed.ROTTWEILER, 30.0, false, true, true)
        )).isEqualTo(MarkerColor.RED);
        assertThat(service.calculateMarkerColor(
            policy,
            pet(PetBreed.ROTTWEILER, 30.0, true, true, true)
        )).isEqualTo(MarkerColor.GREEN);
    }

    @Test
    void 필수장비를_하나라도_갖추지_못하면_RED다() {
        PlacePetPolicy leashPolicy = policy(AccessScope.ALL);
        when(leashPolicy.getLeashRequired()).thenReturn(true);
        PlacePetPolicy muzzlePolicy = policy(AccessScope.ALL);
        when(muzzlePolicy.getMuzzleRequired()).thenReturn(true);
        PlacePetPolicy kennelPolicy = policy(AccessScope.ALL);
        when(kennelPolicy.getKennelRequired()).thenReturn(true);

        assertThat(service.calculateMarkerColor(leashPolicy, pet(PetBreed.MALTESE, 5.0, true, false, true)))
            .isEqualTo(MarkerColor.RED);
        assertThat(service.calculateMarkerColor(muzzlePolicy, pet(PetBreed.MALTESE, 5.0, false, true, true)))
            .isEqualTo(MarkerColor.RED);
        assertThat(service.calculateMarkerColor(kennelPolicy, pet(PetBreed.MALTESE, 5.0, true, true, false)))
            .isEqualTo(MarkerColor.RED);
    }

    @Test
    void LESS_THAN은_제한과_같은_몸무게도_RED다() {
        PlacePetPolicy policy = weightPolicy(10.0, WeightLimitType.LESS_THAN);

        assertThat(service.calculateMarkerColor(policy, pet(PetBreed.MALTESE, 9.9, true, true, true)))
            .isEqualTo(MarkerColor.GREEN);
        assertThat(service.calculateMarkerColor(policy, pet(PetBreed.MALTESE, 10.0, true, true, true)))
            .isEqualTo(MarkerColor.RED);
    }

    @Test
    void LESS_THAN_OR_EQUAL은_제한과_같은_몸무게를_허용한다() {
        PlacePetPolicy policy = weightPolicy(10.0, WeightLimitType.LESS_THAN_OR_EQUAL);

        assertThat(service.calculateMarkerColor(policy, pet(PetBreed.MALTESE, 10.0, true, true, true)))
            .isEqualTo(MarkerColor.GREEN);
        assertThat(service.calculateMarkerColor(policy, pet(PetBreed.MALTESE, 10.1, true, true, true)))
            .isEqualTo(MarkerColor.RED);
    }

    @Test
    void 추가확인이_필요한_정책이면_YELLOW다() {
        Pet pet = pet(PetBreed.MALTESE, 5.0, true, true, true);

        PlacePetPolicy defaultPolicy = policy(AccessScope.ALL);
        when(defaultPolicy.getDefaultPolicy()).thenReturn("YELLOW");
        PlacePetPolicy partialPolicy = policy(AccessScope.PARTIAL);
        PlacePetPolicy inquiryPolicy = policy(AccessScope.ALL);
        when(inquiryPolicy.getAdvanceInquiryRequired()).thenReturn(true);

        assertThat(service.calculateMarkerColor(defaultPolicy, pet)).isEqualTo(MarkerColor.YELLOW);
        assertThat(service.calculateMarkerColor(partialPolicy, pet)).isEqualTo(MarkerColor.YELLOW);
        assertThat(service.calculateMarkerColor(inquiryPolicy, pet)).isEqualTo(MarkerColor.YELLOW);
    }

    @Test
    void 검토대기_정책이면_YELLOW다() {
        PlacePetPolicy policy = policy(AccessScope.ALL);
        when(policy.getReviewPending()).thenReturn(true);

        assertThat(service.calculateMarkerColor(
            policy,
            pet(PetBreed.MALTESE, 5.0, true, true, true)
        )).isEqualTo(MarkerColor.YELLOW);
    }

    @Test
    void 해석할_수_없는_체중이나_맹견조건이면_YELLOW다() {
        PlacePetPolicy weightPolicy = weightPolicy(10.0, WeightLimitType.UNKNOWN);
        PlacePetPolicy breedPolicy = policy(AccessScope.ALL);
        when(breedPolicy.getDangerousBreedAllowedCondition()).thenReturn("OWNER_CONTROL");

        assertThat(service.calculateMarkerColor(
            weightPolicy,
            pet(PetBreed.MALTESE, 5.0, true, true, true)
        )).isEqualTo(MarkerColor.YELLOW);
        assertThat(service.calculateMarkerColor(
            breedPolicy,
            pet(PetBreed.TOSA_INU, 20.0, true, true, true)
        )).isEqualTo(MarkerColor.YELLOW);
    }

    @Test
    void 알려진_모든_조건을_충족하면_GREEN이다() {
        PlacePetPolicy policy = weightPolicy(10.0, WeightLimitType.LESS_THAN_OR_EQUAL);
        when(policy.getLeashRequired()).thenReturn(true);
        when(policy.getMuzzleRequired()).thenReturn(true);
        when(policy.getKennelRequired()).thenReturn(true);

        assertThat(service.calculateMarkerColor(
            policy,
            pet(PetBreed.MALTESE, 5.0, true, true, true)
        )).isEqualTo(MarkerColor.GREEN);
    }

    private PlacePetPolicy policy(AccessScope accessScope) {
        PlacePetPolicy policy = mock(PlacePetPolicy.class);
        when(policy.getAccessScope()).thenReturn(accessScope);
        when(policy.getDangerousBreedAllowed()).thenReturn(null);
        when(policy.getMaxWeightKg()).thenReturn(null);
        when(policy.getLeashRequired()).thenReturn(null);
        when(policy.getMuzzleRequired()).thenReturn(null);
        when(policy.getKennelRequired()).thenReturn(null);
        when(policy.getAdvanceInquiryRequired()).thenReturn(null);
        return policy;
    }

    private PlacePetPolicy weightPolicy(double maxWeight, WeightLimitType type) {
        PlacePetPolicy policy = policy(AccessScope.ALL);
        when(policy.getMaxWeightKg()).thenReturn(maxWeight);
        when(policy.getWeightLimitType()).thenReturn(type);
        return policy;
    }

    private Pet pet(
        PetBreed breed,
        double weight,
        boolean hasMuzzle,
        boolean hasLeash,
        boolean hasCarrier
    ) {
        Pet pet = mock(Pet.class);
        when(pet.getBreed()).thenReturn(breed);
        when(pet.getWeight()).thenReturn(weight);
        when(pet.isHasMuzzle()).thenReturn(hasMuzzle);
        when(pet.isHasLeash()).thenReturn(hasLeash);
        when(pet.isHasCarrier()).thenReturn(hasCarrier);
        return pet;
    }
}
