package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.image.ImageDeletionTask;
import com.tmd.backend.domain.image.ImageUpload;
import com.tmd.backend.domain.image.ImageUploadStatus;
import com.tmd.backend.domain.image.ImageUsage;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.request.image.PresignedUrlRequest;
import com.tmd.backend.dto.response.image.ImageUploadCompleteResponse;
import com.tmd.backend.dto.response.image.PresignedUrlResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.ImageUploadRepository;
import com.tmd.backend.repository.ImageDeletionTaskRepository;
import com.tmd.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ImageService {
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
        "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png",
        "gif", "image/gif", "webp", "image/webp"
    );
    private static final List<ImageUploadStatus> CLEANUP_STATUSES = List.of(
        ImageUploadStatus.PENDING, ImageUploadStatus.PROMOTING,
        ImageUploadStatus.READY, ImageUploadStatus.DELETE_PENDING
    );

    private final S3Presigner r2Presigner;
    private final S3Client r2Client;
    private final UserRepository userRepository;
    private final ImageUploadRepository imageUploadRepository;
    private final ImageDeletionTaskRepository imageDeletionTaskRepository;
    private final String bucket;
    private final String publicUrl;
    private final Duration presignedUrlTtl;
    private final Duration unattachedImageTtl;
    private final long maxUploadBytes;
    private final int cleanupBatchSize;

    public ImageService(S3Presigner r2Presigner, S3Client r2Client,
                        UserRepository userRepository, ImageUploadRepository imageUploadRepository,
                        ImageDeletionTaskRepository imageDeletionTaskRepository,
                        @Value("${cloudflare.r2.bucket}") String bucket,
                        @Value("${cloudflare.r2.public-url}") String publicUrl,
                        @Value("${cloudflare.r2.presigned-url-ttl-seconds:600}") long presignedUrlTtlSeconds,
                        @Value("${cloudflare.r2.unattached-image-ttl-hours:24}") long unattachedImageTtlHours,
                        @Value("${cloudflare.r2.max-upload-bytes:10485760}") long maxUploadBytes,
                        @Value("${cloudflare.r2.cleanup-batch-size:100}") int cleanupBatchSize) {
        if (presignedUrlTtlSeconds < 1 || presignedUrlTtlSeconds > Duration.ofDays(7).toSeconds()) {
            throw new IllegalArgumentException("R2 presigned URL TTL must be between 1 second and 7 days");
        }
        if (unattachedImageTtlHours < 1 || maxUploadBytes < 1 || cleanupBatchSize < 1) {
            throw new IllegalArgumentException("R2 image limits must be greater than zero");
        }
        this.r2Presigner = r2Presigner;
        this.r2Client = r2Client;
        this.userRepository = userRepository;
        this.imageUploadRepository = imageUploadRepository;
        this.imageDeletionTaskRepository = imageDeletionTaskRepository;
        this.bucket = bucket;
        this.publicUrl = removeTrailingSlash(publicUrl);
        this.presignedUrlTtl = Duration.ofSeconds(presignedUrlTtlSeconds);
        this.unattachedImageTtl = Duration.ofHours(unattachedImageTtlHours);
        this.maxUploadBytes = maxUploadBytes;
        this.cleanupBatchSize = cleanupBatchSize;
    }

    @Transactional
    public PresignedUrlResponse initiateUpload(String email, PresignedUrlRequest request) {
        String extension = extractExtension(request.filename());
        String contentType = normalizeContentType(request.contentType());
        validateFile(extension, contentType, request.fileSize());
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BaseException(ErrorCode.UNAUTHORIZED));

        UUID uploadId = UUID.randomUUID();
        String temporaryKey = "tmp/" + user.getId() + "/" + uploadId + "." + extension;
        String finalKey = "images/" + request.usage().name().toLowerCase(Locale.ROOT)
            + "/" + uploadId + "." + extension;
        LocalDateTime now = LocalDateTime.now();
        imageUploadRepository.save(ImageUpload.pending(uploadId, user, request.usage(), temporaryKey,
            finalKey, contentType, request.fileSize(), now, now.plus(unattachedImageTtl)));

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket).key(temporaryKey).contentType(contentType)
            .contentLength(request.fileSize()).build();
        PresignedPutObjectRequest presigned = r2Presigner.presignPutObject(
            PutObjectPresignRequest.builder().signatureDuration(presignedUrlTtl)
                .putObjectRequest(putRequest).build());

        return PresignedUrlResponse.builder()
            .uploadId(uploadId).presignedUrl(presigned.url().toString())
            .requiredHeaders(Map.of("Content-Type", contentType))
            .expiresInSeconds(presignedUrlTtl.toSeconds()).build();
    }

    @Transactional
    public ImageUploadCompleteResponse completeUpload(String email, UUID uploadId) {
        ImageUpload upload = findOwnedForUpdate(email, uploadId);
        if (upload.getStatus() == ImageUploadStatus.READY) {
            return completeResponse(upload);
        }
        if (upload.getStatus() != ImageUploadStatus.PENDING) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_UPLOAD);
        }
        HeadObjectResponse object = headObject(upload.getTemporaryKey());
        if (object.contentLength() != upload.getExpectedSize()
            || !upload.getContentType().equalsIgnoreCase(object.contentType())) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_UPLOAD);
        }

        upload.markPromoting();
        try {
            r2Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucket).sourceKey(upload.getTemporaryKey())
                .destinationBucket(bucket).destinationKey(upload.getFinalKey())
                .contentType(upload.getContentType()).metadataDirective(MetadataDirective.REPLACE).build());
            deleteObject(upload.getTemporaryKey());
        } catch (SdkException e) {
            throw new BaseException(ErrorCode.IMAGE_STORAGE_ERROR, "R2 image promotion failed", e);
        }
        upload.markReady(LocalDateTime.now().plus(unattachedImageTtl));
        return completeResponse(upload);
    }

    @Transactional
    public String attachReadyUpload(String email, UUID uploadId, ImageUsage expectedUsage) {
        if (uploadId == null) return null;
        ImageUpload upload = findOwnedForUpdate(email, uploadId);
        if (upload.getStatus() == ImageUploadStatus.ATTACHED) {
            throw new BaseException(ErrorCode.IMAGE_UPLOAD_ALREADY_USED);
        }
        if (upload.getStatus() != ImageUploadStatus.READY || upload.getUsage() != expectedUsage) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_UPLOAD);
        }
        upload.markAttached();
        return upload.getFinalKey();
    }

    @Transactional
    public void markForDeletion(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) return;
        enqueueDeletion(imageKey);
        imageUploadRepository.findByFinalKey(imageKey)
            .ifPresent(imageUploadRepository::delete);
    }

    @Transactional
    public void markAllForDeletion(Long userId) {
        List<ImageUpload> uploads = imageUploadRepository.findAllByUserId(userId);
        for (ImageUpload upload : uploads) {
            enqueueDeletion(upload.getTemporaryKey());
            enqueueDeletion(upload.getFinalKey());
        }
        if (!uploads.isEmpty()) {
            imageUploadRepository.deleteAllInBatch(uploads);
        }
    }

    public String toPublicUrl(String imageKey) {
        return imageKey == null || imageKey.isBlank() ? null : publicUrl + "/" + imageKey;
    }

    @Scheduled(fixedDelayString = "${cloudflare.r2.cleanup-interval-millis:3600000}")
    @Transactional
    public void cleanupExpiredUploads() {
        List<ImageUpload> expired = imageUploadRepository.findExpiredForUpdate(
            CLEANUP_STATUSES, LocalDateTime.now(), PageRequest.of(0, cleanupBatchSize));
        for (ImageUpload upload : expired) {
            try {
                deleteObject(upload.getTemporaryKey());
                deleteObject(upload.getFinalKey());
                imageUploadRepository.delete(upload);
            } catch (SdkException e) {
                log.warn("Failed to clean up image upload {}. It will be retried.", upload.getId(), e);
            }
        }

        List<ImageDeletionTask> deletionTasks = imageDeletionTaskRepository.findPendingForUpdate(
            PageRequest.of(0, cleanupBatchSize));
        for (ImageDeletionTask task : deletionTasks) {
            try {
                deleteObject(task.getObjectKey());
                imageDeletionTaskRepository.delete(task);
            } catch (SdkException e) {
                log.warn("Failed to delete image object {}. It will be retried.", task.getObjectKey(), e);
            }
        }
    }

    private void enqueueDeletion(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        imageDeletionTaskRepository.findByObjectKey(objectKey)
            .orElseGet(() -> imageDeletionTaskRepository.save(ImageDeletionTask.create(objectKey)));
    }

    private ImageUpload findOwnedForUpdate(String email, UUID uploadId) {
        return imageUploadRepository.findOwnedForUpdate(uploadId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.IMAGE_UPLOAD_NOT_FOUND));
    }

    private HeadObjectResponse headObject(String key) {
        try {
            return r2Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            if (e instanceof S3Exception s3Exception && s3Exception.statusCode() == 404) {
                throw new BaseException(ErrorCode.INVALID_IMAGE_UPLOAD, "Uploaded object was not found", e);
            }
            throw new BaseException(ErrorCode.IMAGE_STORAGE_ERROR, "R2 image lookup failed", e);
        }
    }

    private void deleteObject(String key) {
        r2Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private ImageUploadCompleteResponse completeResponse(ImageUpload upload) {
        return ImageUploadCompleteResponse.builder().uploadId(upload.getId())
            .imageUrl(toPublicUrl(upload.getFinalKey())).build();
    }

    private void validateFile(String extension, String contentType, long fileSize) {
        String expected = ALLOWED_IMAGE_TYPES.get(extension);
        if (expected == null || !expected.equals(contentType)) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_TYPE);
        }
        if (fileSize < 1 || fileSize > maxUploadBytes) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_SIZE);
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= filename.length() - 1) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_TYPE);
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private static String removeTrailingSlash(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') end--;
        return url.substring(0, end);
    }
}
