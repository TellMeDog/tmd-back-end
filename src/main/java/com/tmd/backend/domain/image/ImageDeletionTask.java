package com.tmd.backend.domain.image;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
    name = "uk_image_deletion_task_object_key",
    columnNames = "object_key"
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageDeletionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, length = 255)
    private String objectKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ImageDeletionTask(String objectKey) {
        this.objectKey = objectKey;
        this.createdAt = LocalDateTime.now();
    }

    public static ImageDeletionTask create(String objectKey) {
        return new ImageDeletionTask(objectKey);
    }
}
