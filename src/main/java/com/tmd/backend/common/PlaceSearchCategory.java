package com.tmd.backend.common;

import java.util.Arrays;

public enum PlaceSearchCategory {
    ALL("전체", null, null, null),
    ACCOMMODATION("숙박", "AC", null, null),
    FESTIVAL("축제", "EV", "EV01", null),
    PERFORMANCE("공연", "EV", "EV02", null),
    EVENT("행사", "EV", "EV03", null),
    EXPERIENCE("체험관광", "EX", null, null),
    FOOD("식당카페", "FD", null, null),
    HISTORY("역사관광", "HS", null, null),
    LEISURE_SPORTS("레저스포츠", "LS", null, null),
    NATURE("자연관광", "NA", null, null),
    SHOPPING("쇼핑", "SH", null, null),
    CULTURE("문화관광", "VE", null, null),
    ANIMAL_HOSPITAL("동물병원", "TMDHOSP", null, null);

    private final String displayName;
    private final String level1Code;
    private final String level2Code;
    private final String level3Code;

    PlaceSearchCategory(
        String displayName,
        String level1Code,
        String level2Code,
        String level3Code
    ) {
        this.displayName = displayName;
        this.level1Code = level1Code;
        this.level2Code = level2Code;
        this.level3Code = level3Code;
    }

    public static PlaceSearchCategory fromDisplayName(String displayName) {
        String normalized = displayName == null ? "" : displayName.trim();
        return Arrays.stream(values())
            .filter(category -> category.displayName.equals(normalized))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported place search category"));
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAll() {
        return this == ALL;
    }

    public int getDepth() {
        if (level3Code != null) return 3;
        if (level2Code != null) return 2;
        if (level1Code != null) return 1;
        return 0;
    }

    public String getQueryCode() {
        if (level3Code != null) return level3Code;
        if (level2Code != null) return level2Code;
        return level1Code;
    }
}
