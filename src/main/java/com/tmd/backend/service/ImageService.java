package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.dto.response.image.PresignedUrlResponse;
import com.tmd.backend.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3Presigner r2Presigner;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.public-url}")
    private String publicUrl;

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(10);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    public PresignedUrlResponse generatePresignedUrl(String originalFilename) {
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_TYPE);
        }

        String key = "images/" + UUID.randomUUID() + "." + extension;

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        PresignedPutObjectRequest presigned = r2Presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_TTL)
                .putObjectRequest(putRequest)
                .build()
        );

        return PresignedUrlResponse.builder()
            .presignedUrl(presigned.url().toString())
            .imageKey(key)
            .imageUrl(publicUrl + "/" + key)
            .build();
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= filename.length() - 1) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_TYPE);
        }
        return filename.substring(dotIndex + 1);
    }
}
