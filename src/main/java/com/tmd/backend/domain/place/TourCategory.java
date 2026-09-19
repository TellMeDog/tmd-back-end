package com.tmd.backend.domain.place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
    @Index(name = "idx_tour_category_parent", columnList = "parent_code"),
    @Index(name = "idx_tour_category_active_depth", columnList = "active, depth")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourCategory {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int depth;

    @Column(name = "parent_code", length = 10)
    private String parentCode;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime syncedAt;

    private TourCategory(String code, String name, int depth, String parentCode,
                         int displayOrder, LocalDateTime syncedAt) {
        this.code = code;
        update(name, depth, parentCode, displayOrder, syncedAt);
    }

    public static TourCategory create(String code, String name, int depth, String parentCode,
                                      int displayOrder, LocalDateTime syncedAt) {
        return new TourCategory(code, name, depth, parentCode, displayOrder, syncedAt);
    }

    public void update(String name, int depth, String parentCode, int displayOrder,
                       LocalDateTime syncedAt) {
        this.name = name;
        this.depth = depth;
        this.parentCode = parentCode;
        this.displayOrder = displayOrder;
        this.active = true;
        this.syncedAt = syncedAt;
    }

    public void deactivate(LocalDateTime syncedAt) {
        this.active = false;
        this.syncedAt = syncedAt;
    }
}
