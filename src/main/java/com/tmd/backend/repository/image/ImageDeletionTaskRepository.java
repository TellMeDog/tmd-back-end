package com.tmd.backend.repository.image;

import com.tmd.backend.domain.image.ImageDeletionTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ImageDeletionTaskRepository extends JpaRepository<ImageDeletionTask, Long> {

    Optional<ImageDeletionTask> findByObjectKey(String objectKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT task FROM ImageDeletionTask task ORDER BY task.createdAt ASC, task.id ASC")
    List<ImageDeletionTask> findPendingForUpdate(Pageable pageable);
}
