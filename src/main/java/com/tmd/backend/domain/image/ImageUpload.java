package com.tmd.backend.domain.image;

import com.tmd.backend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(indexes = {
    @Index(name = "idx_image_upload_cleanup", columnList = "status, expiresAt"),
    @Index(name = "idx_image_upload_final_key", columnList = "finalKey", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageUpload {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_usage", nullable = false, length = 20)
    private ImageUsage usage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageUploadStatus status;

    @Column(nullable = false, length = 255)
    private String temporaryKey;

    @Column(nullable = false, length = 255)
    private String finalKey;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long expectedSize;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private ImageUpload(UUID id, User user, ImageUsage usage, String temporaryKey,
                        String finalKey, String contentType, long expectedSize,
                        LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.user = user;
        this.usage = usage;
        this.status = ImageUploadStatus.PENDING;
        this.temporaryKey = temporaryKey;
        this.finalKey = finalKey;
        this.contentType = contentType;
        this.expectedSize = expectedSize;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static ImageUpload pending(UUID id, User user, ImageUsage usage,
                                      String temporaryKey, String finalKey,
                                      String contentType, long expectedSize,
                                      LocalDateTime now, LocalDateTime expiresAt) {
        return new ImageUpload(id, user, usage, temporaryKey, finalKey,
            contentType, expectedSize, now, expiresAt);
    }

    public void markPromoting() {
        this.status = ImageUploadStatus.PROMOTING;
    }

    public void markReady(LocalDateTime expiresAt) {
        this.status = ImageUploadStatus.READY;
        this.expiresAt = expiresAt;
    }

    public void markAttached() {
        this.status = ImageUploadStatus.ATTACHED;
        this.expiresAt = null;
    }

    public void markDeletePending(LocalDateTime now) {
        this.status = ImageUploadStatus.DELETE_PENDING;
        this.expiresAt = now;
    }
}
