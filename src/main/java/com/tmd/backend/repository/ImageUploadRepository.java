package com.tmd.backend.repository;

import com.tmd.backend.domain.image.ImageUpload;
import com.tmd.backend.domain.image.ImageUploadStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageUploadRepository extends JpaRepository<ImageUpload, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ImageUpload i WHERE i.id = :id AND i.user.email = :email")
    Optional<ImageUpload> findOwnedForUpdate(@Param("id") UUID id, @Param("email") String email);

    Optional<ImageUpload> findByFinalKey(String finalKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i FROM ImageUpload i
        WHERE i.status IN :statuses AND i.expiresAt <= :now
        ORDER BY i.expiresAt ASC
        """)
    List<ImageUpload> findExpiredForUpdate(
        @Param("statuses") Collection<ImageUploadStatus> statuses,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );
}
