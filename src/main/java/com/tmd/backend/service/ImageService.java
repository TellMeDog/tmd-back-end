package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.dto.request.image.PresignedUrlRequest;
import com.tmd.backend.dto.response.image.PresignedUrlResponse;
import com.tmd.backend.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageService {

    private final S3Presigner r2Presigner;
    private final String bucket;
    private final String publicUrl;
    private final Duration presignedUrlTtl;
    private final long maxUploadBytes;

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "png", "image/png",
        "gif", "image/gif",
        "webp", "image/webp"
    );

    public ImageService(
        S3Presigner r2Presigner,
        @Value("${cloudflare.r2.bucket}") String bucket,
        @Value("${cloudflare.r2.public-url}") String publicUrl,
        @Value("${cloudflare.r2.presigned-url-ttl-seconds:600}") long presignedUrlTtlSeconds,
        @Value("${cloudflare.r2.max-upload-bytes:10485760}") long maxUploadBytes
    ) {
        if (presignedUrlTtlSeconds < 1 || presignedUrlTtlSeconds > Duration.ofDays(7).toSeconds()) {
            throw new IllegalArgumentException("R2 presigned URL TTL must be between 1 second and 7 days");
        }
        if (maxUploadBytes < 1) {
            throw new IllegalArgumentException("R2 max upload size must be greater than zero");
        }

        this.r2Presigner = r2Presigner;
        this.bucket = bucket;
        this.publicUrl = removeTrailingSlash(publicUrl);
        this.presignedUrlTtl = Duration.ofSeconds(presignedUrlTtlSeconds);
        this.maxUploadBytes = maxUploadBytes;
    }

    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request) {
        String extension = extractExtension(request.filename());
        String contentType = normalizeContentType(request.contentType());
        String expectedContentType = ALLOWED_IMAGE_TYPES.get(extension);
        if (expectedContentType == null || !expectedContentType.equals(contentType)) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_TYPE);
        }
        if (request.fileSize() < 1 || request.fileSize() > maxUploadBytes) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_SIZE);
        }

        String key = "images/" + UUID.randomUUID() + "." + extension;

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(request.fileSize())
            .build();

        PresignedPutObjectRequest presigned = r2Presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(presignedUrlTtl)
                .putObjectRequest(putRequest)
                .build()
        );

        return PresignedUrlResponse.builder()
            .presignedUrl(presigned.url().toString())
            .imageKey(key)
            .imageUrl(publicUrl + "/" + key)
            .requiredHeaders(Map.of("Content-Type", contentType))
            .expiresInSeconds(presignedUrlTtl.toSeconds())
            .build();
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
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }
}
