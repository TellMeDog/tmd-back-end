package com.tmd.backend.domain.review;

public enum MismatchReason {
    STAFF_UNWARE, // 직원이 규정을 몰랐어요
    CARRIED_REQUIRED, // 이동장이 필요했어요
    MUZZLE_REQUIRED, // 입마개가 필요했어요
    LEASH_REQUIRED, // 목줄이 필요했어요
    ETC // 기타
}
