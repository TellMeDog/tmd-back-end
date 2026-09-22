package com.tmd.backend.service.place;

import com.tmd.backend.ai.AccessScope;
import com.tmd.backend.ai.WeightLimitType;
import com.tmd.backend.common.MarkerColor;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetPolicy;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class MarkerColorService {

    private static final Set<PetBreed> DANGEROUS_BREEDS = Set.of(
        PetBreed.TOSA_INU,
        PetBreed.AMERICAN_PIT_BULL_TERRIER,
        PetBreed.AMERICAN_STAFFORDSHIRE_TERRIER,
        PetBreed.STAFFORDSHIRE_BULL_TERRIER,
        PetBreed.DOGO_ARGENTINO,
        PetBreed.CANE_CORSO,
        PetBreed.OVCHARKA,
        PetBreed.ROTTWEILER,
        PetBreed.TIBETAN_MASTIFF,
        PetBreed.WOLF_DOG
    );

    public MarkerColor calculateMarkerColorForPlace(Place place, Pet pet) {
        if (pet == null) {
            return MarkerColor.GREY;
        }
        if (place.isAnimalHospital()) {
            return MarkerColor.GREEN;
        }
        return calculateMarkerColor(place.getPlacePetPolicy(), pet);
    }

    public MarkerColor calculateMarkerColor(PlacePetPolicy policy, Pet pet) {
        if (pet == null) {
            return MarkerColor.GREY;
        }

        if (policy == null
            || policy.getAccessScope() == null
            || policy.getAccessScope() == AccessScope.UNKNOWN) {
            return MarkerColor.GREY;
        }

        if (policy.getAccessScope() == AccessScope.NONE || violatesKnownPolicy(policy, pet)) {
            return MarkerColor.RED;
        }

        if (requiresAdditionalConfirmation(policy, pet)) {
            return MarkerColor.YELLOW;
        }

        return MarkerColor.GREEN;
    }

    private boolean violatesKnownPolicy(PlacePetPolicy policy, Pet pet) {
        return violatesDangerousBreedPolicy(policy, pet)
            || violatesWeightLimit(policy, pet)
            || Boolean.TRUE.equals(policy.getLeashRequired()) && !pet.isHasLeash()
            || Boolean.TRUE.equals(policy.getMuzzleRequired()) && !pet.isHasMuzzle()
            || Boolean.TRUE.equals(policy.getKennelRequired()) && !pet.isHasCarrier();
    }

    private boolean violatesDangerousBreedPolicy(PlacePetPolicy policy, Pet pet) {
        if (!DANGEROUS_BREEDS.contains(pet.getBreed())) {
            return false;
        }
        if (Boolean.FALSE.equals(policy.getDangerousBreedAllowed())) {
            return true;
        }
        return "MUZZLE".equalsIgnoreCase(policy.getDangerousBreedAllowedCondition())
            && !pet.isHasMuzzle();
    }

    private boolean violatesWeightLimit(PlacePetPolicy policy, Pet pet) {
        Double maxWeightKg = policy.getMaxWeightKg();
        Double weight = pet.getWeight();
        WeightLimitType limitType = policy.getWeightLimitType();
        if (maxWeightKg == null || weight == null || limitType == null) {
            return false;
        }

        return switch (limitType) {
            case LESS_THAN -> weight >= maxWeightKg;
            case LESS_THAN_OR_EQUAL -> weight > maxWeightKg;
            case UNKNOWN -> false;
        };
    }

    private boolean requiresAdditionalConfirmation(PlacePetPolicy policy, Pet pet) {
        if (Boolean.TRUE.equals(policy.getReviewPending())
            || "YELLOW".equals(policy.getDefaultPolicy())
            || policy.getAccessScope() == AccessScope.PARTIAL
            || Boolean.TRUE.equals(policy.getAdvanceInquiryRequired())) {
            return true;
        }

        if (policy.getMaxWeightKg() != null
            && (policy.getWeightLimitType() == null
            || policy.getWeightLimitType() == WeightLimitType.UNKNOWN)) {
            return true;
        }

        String dangerousCondition = policy.getDangerousBreedAllowedCondition();
        return DANGEROUS_BREEDS.contains(pet.getBreed())
            && dangerousCondition != null
            && !dangerousCondition.isBlank()
            && !"MUZZLE".equalsIgnoreCase(dangerousCondition);
    }
}
